import { Component, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subject, takeUntil } from 'rxjs';
import { FinanceApiService, Overview, Pocket, Credit, Risk, DocumentRecord, Profile, Activity } from '../utils/finance-api.service';
import { FrontEndService } from '../utils/front-end.service';
import { UserService } from '../utils/user.service';

@Component({ selector: 'app-finance', templateUrl: './finance.component.html', styleUrls: ['./finance.component.scss'] })
export class FinanceComponent implements OnInit, OnDestroy {
  @ViewChild('confirmation') confirmation!: TemplateRef<unknown>;
  section = 'overview'; data: Overview | null = null;
  loading = true; busy = false; error = ''; riskLoading = false;
  risks: Risk[] = []; reviewCredits: Credit[] = []; reviewDocs: DocumentRecord[] = [];
  selectedReview: Credit | null = null; reviewNote = ''; currentCreditId: number | null = null;
  pocket = { name: '', initialAmount: 0, targetAmount: 1000 };
  payment = { recipientIban: '', amount: null as number | null, scheduledAt: '' };
  profile: Profile = { email: '', phone: '', country: 'Romania', dateOfBirth: '', occupation: '' };
  credit = { amount: 3000, monthlyIncome: 1500, termMonths: 12, purpose: '' };
  dialogKind = ''; dialogTitle = ''; pocketAction = 'deposit'; selectedPocket: Pocket | null = null;
  moveAmount = 100; selectedSchedule = 0; risk: Risk | null = null;
  private destroyed = new Subject<void>();
  constructor(public api: FinanceApiService, private route: ActivatedRoute, private dialog: MatDialog,
              private toast: MatSnackBar, private transfers: FrontEndService, public user: UserService) {}
  ngOnInit(): void {
    this.route.data.pipe(takeUntil(this.destroyed)).subscribe(data => {
      this.section = data['section'] ?? 'overview'; this.dialog.closeAll(); this.load();
    });
  }
  ngOnDestroy(): void { this.destroyed.next(); this.destroyed.complete(); this.dialog.closeAll(); }
  load(): void {
    this.loading = true; this.error = '';
    this.api.overview().pipe(takeUntil(this.destroyed)).subscribe({ next: data => {
      this.data = data; this.loading = false;
      if (data.profile) this.profile = { ...data.profile };
      if (!this.currentCreditId) this.currentCreditId = data.credits.find(c => c.status === 'DRAFT')?.id ?? null;
      if (this.section === 'security') this.loadRisks();
    }, error: e => { this.loading = false; this.error = this.message(e); } });
  }
  scanLabel(status: string): string {
    return ({TEXT_EXTRACTED: 'Text ready to review', NEEDS_MANUAL_REVIEW: 'Manual review needed', SCANNER_UNAVAILABLE: 'Document saved · Scanner not installed', TIMED_OUT: 'Document saved · Scan timed out', INTERRUPTED: 'Document saved · Scan interrupted'} as Record<string, string>)[status] ?? 'Document saved';
  }
  private message(e: any): string { return e?.error?.message || 'We could not complete that request. Check your connection and try again.'; }
  mutate<T>(request: Observable<T>, message: string, after?: (result: T) => void): void {
    if (this.busy) return;
    this.busy = true; this.error = '';
    request.pipe(takeUntil(this.destroyed)).subscribe({ next: result => {
      this.busy = false; this.dialog.closeAll(); if (after) after(result);
      this.toast.open(message, 'Dismiss', { duration: 5000 }); this.load();
    }, error: e => { this.busy = false; this.error = this.message(e); this.toast.open(this.error, 'Dismiss', { duration: 8000 }); } });
  }
  get savings(): number { return this.data?.pockets.reduce((sum, p) => sum + Number(p.balance), 0) ?? 0; }
  get total(): number { return Number(this.data?.account.amount ?? 0) + this.savings; }
  get draft(): Credit | undefined { return this.data?.credits.find(c => c.id === this.currentCreditId && c.status === 'DRAFT'); }
  get currentDocuments(): DocumentRecord[] { return this.data?.documents.filter(d => d.applicationId === this.currentCreditId) ?? []; }
  get isAdmin(): boolean { return this.user.getCurrentRole() === 'Admin'; }
  get active(): boolean { return this.data?.account.status === 'ACTIVE'; }
  get hasLegacyData(): boolean {
    const id = this.data?.user.id;
    return !!(id && localStorage.getItem('payments-deposits:' + id)) || !!localStorage.getItem('payments-pending-transactions');
  }
  exportLegacy(): void {
    const raw = { savings: localStorage.getItem('payments-deposits:' + this.data?.user.id), schedules: localStorage.getItem('payments-pending-transactions') };
    const url = URL.createObjectURL(new Blob([JSON.stringify(raw, null, 2)], { type: 'application/json' }));
    const link = document.createElement('a'); link.href = url; link.download = 'nspay-legacy-browser-data.json'; link.click(); URL.revokeObjectURL(url);
  }
  units(symbol: string): number { return this.data?.trades.filter(t => t.symbol === symbol).reduce((sum, t) => sum + (t.side === 'BUY' ? 1 : -1) * Number(t.quantity), 0) ?? 0; }
  progress(p: Pocket): number { return Math.min(100, Number(p.balance) / Number(p.targetAmount) * 100); }
  incoming(a: Activity): boolean { return ['TRANSFER_IN', 'SAVINGS_IN', 'TRADE_SELL', 'INVESTMENT_WITHDRAWAL'].includes(a.transactionType); }
  activityName(a: Activity): string {
    return ({ TRANSFER_IN: 'Transfer received', TRANSFER_OUT: 'Transfer sent', SAVINGS_OUT: 'Added to savings', SAVINGS_IN: 'Savings withdrawal', INVESTMENT_DEPOSIT: 'Investment deposit', INVESTMENT_WITHDRAWAL: 'Investment withdrawal', TRADE_BUY: 'Investment purchase', TRADE_SELL: 'Investment sale' } as Record<string, string>)[a.transactionType] || 'Card payment';
  }
  spending(day: number): number {
    const date = new Date(); date.setDate(date.getDate() - (6 - day));
    return this.data?.transactions.filter(t => new Date(t.timestamp).toDateString() === date.toDateString() && !this.incoming(t) && String(t.returnCode) === '00')
      .reduce((sum, t) => sum + Number(t.amount), 0) ?? 0;
  }
  get maxSpending(): number { return Math.max(1, ...[0,1,2,3,4,5,6].map(d => this.spending(d))); }
  dayLabel(day: number): string { const date = new Date(); date.setDate(date.getDate() - (6 - day)); return date.toLocaleDateString('en', { weekday: 'short' }); }
  createPocket(): void {
    this.mutate(this.api.post<Pocket>('pockets', this.pocket), 'Savings pocket created.', () => { this.pocket = { name: '', initialAmount: 0, targetAmount: 1000 }; });
  }
  openPocket(p: Pocket, action: string): void {
    this.selectedPocket = p; this.pocketAction = action; this.moveAmount = 100;
    this.open('pocket', action === 'withdraw' ? 'Withdraw from ' + p.name : 'Add money to ' + p.name);
  }
  closePocket(p: Pocket): void { this.selectedPocket = p; this.open('close', 'Close ' + p.name + '?'); }
  cancelSchedule(id: number): void { this.selectedSchedule = id; this.open('cancel', 'Cancel scheduled transfer?'); }
  reviewPayment(): void {
    this.risk = null;
    this.open('payment', this.payment.scheduledAt ? 'Review scheduled transfer' : 'Review transfer');
    this.riskLoading = true;
    this.api.risk({ recipientIban: this.payment.recipientIban, amount: this.payment.amount }).pipe(takeUntil(this.destroyed)).subscribe({
      next: risk => { this.risk = risk; this.riskLoading = false; },
      error: e => { this.riskLoading = false; this.error = this.message(e); this.dialog.closeAll(); }
    });
  }
  open(kind: string, title: string): void {
    this.error = ''; this.dialogKind = kind; this.dialogTitle = title;
    this.dialog.open(this.confirmation, { width: '480px', maxWidth: '94vw', ariaLabel: title, disableClose: false });
  }
  confirm(): void {
    if (this.dialogKind === 'pocket' && this.selectedPocket)
      this.mutate(this.api.post('pockets/' + this.selectedPocket.id + '/' + this.pocketAction, { amount: this.moveAmount }), 'Savings balance updated.');
    if (this.dialogKind === 'close' && this.selectedPocket)
      this.mutate(this.api.remove('pockets/' + this.selectedPocket.id), 'Pocket closed. Remaining money returned to your main balance.');
    if (this.dialogKind === 'cancel') this.mutate(this.api.remove('scheduled-transfers/' + this.selectedSchedule), 'Scheduled transfer cancelled.');
    if (this.dialogKind === 'payment') {
      const payload = { recipientIban: this.payment.recipientIban, amount: this.payment.amount! };
      if (this.payment.scheduledAt) {
        const date = new Date(this.payment.scheduledAt);
        if (!Number.isFinite(date.getTime()) || date.getTime() <= Date.now()) { this.error = 'Choose a future date and time.'; return; }
        this.mutate(this.api.post('scheduled-transfers', { ...payload, scheduledAt: date.toISOString() }), 'Transfer scheduled. It runs while the backend is online.', () => this.resetPayment());
      } else this.mutate(this.transfers.transfer(payload), 'Transfer completed.', () => this.resetPayment());
    }
  }
  private resetPayment(): void { this.payment = { recipientIban: '', amount: null, scheduledAt: '' }; }
  saveProfile(): void { this.mutate(this.api.profile(this.profile), 'Profile saved. You can now add your identity document.'); }
  createCredit(): void { this.mutate(this.api.post<Credit>('credit', this.credit), 'Application draft saved. Add your supporting documents.', c => this.currentCreditId = c.id); }
  submitCredit(): void { if (this.currentCreditId) this.mutate(this.api.post('credit/' + this.currentCreditId + '/submit'), 'Application submitted.'); }
  upload(event: Event, kind: string, credit: boolean): void {
    const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return;
    if (file.size > 5 * 1024 * 1024) { this.error = 'Choose a file smaller than 5 MB.'; input.value = ''; return; }
    if (credit && !this.currentCreditId) { this.error = 'Create an application draft first.'; input.value = ''; return; }
    this.mutate(this.api.upload(file, kind, credit ? this.currentCreditId! : undefined), 'Document saved. Review the extracted text before relying on it.'); input.value = '';
  }
  loadRisks(): void {
    this.api.risks().pipe(takeUntil(this.destroyed)).subscribe({ next: rows => this.risks = rows, error: e => this.error = this.message(e) });
    if (this.isAdmin) this.api.reviews().pipe(takeUntil(this.destroyed)).subscribe({ next: rows => { this.risks = rows.risks; this.reviewCredits = rows.credits; }, error: e => this.error = this.message(e) });
  }
  inspectCredit(c: Credit): void {
    this.selectedReview = c; this.reviewNote = ''; this.reviewDocs = [];
    this.api.creditDocuments(c.id).pipe(takeUntil(this.destroyed)).subscribe({ next: rows => this.reviewDocs = rows, error: e => this.error = this.message(e) });
  }
  reviewCredit(decision: string): void {
    if (this.selectedReview) this.mutate(this.api.post('credit/' + this.selectedReview.id + '/review', { decision, note: this.reviewNote }), 'Decision recorded.', () => { this.selectedReview = null; });
  }
  reviewRisk(r: Risk): void { if (r.id) this.mutate(this.api.post('risk-assessments/' + r.id + '/review', { decision: 'REVIEWED', note: 'Reviewed by administrator' }), 'Risk signal marked reviewed.'); }
}
