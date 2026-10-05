import { Component, OnInit, OnDestroy, ViewChild, TemplateRef, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepper } from '@angular/material/stepper';
import { Observable, Subject, takeUntil, timer } from 'rxjs';
import { FinanceApiService, Overview, Pocket, Credit, Risk, DocumentRecord, Profile, Activity } from '../utils/finance-api.service';
import { FrontEndService } from '../utils/front-end.service';
import { UserService } from '../utils/user.service';

@Component({ selector: 'app-finance', templateUrl: './finance.component.html', styleUrls: ['./finance.component.scss','./onboarding.scss','./transfer-schedule.scss'] })
export class FinanceComponent implements OnInit, OnDestroy {
  @ViewChild('confirmation') confirmation!: TemplateRef<unknown>;
  @ViewChild('schedulePicker') schedulePicker!: TemplateRef<unknown>;
  @ViewChild('creditSteps') creditSteps?: MatStepper;
  @ViewChild('creditWorkflow') creditWorkflow?: ElementRef<HTMLElement>;
  section = 'overview'; data: Overview | null = null;
  loading = true; busy = false; error = ''; riskLoading = false;
  risks: Risk[] = []; reviewCredits: Credit[] = []; reviewDocs: DocumentRecord[] = [];
  selectedReview: Credit | null = null; reviewNote = ''; currentCreditId: number | null = null;
  pocket = { name: '', initialAmount: 0, targetAmount: 1000 };
  payment = { recipientIban: '', amount: null as number | null, scheduledAt: '' };
  scheduleDate: Date | null = null; scheduleMinDate = new Date();
  scheduleHour = 9; scheduleMinute = 0; scheduleError = '';
  readonly hours = Array.from({ length: 24 }, (_, i) => i);
  readonly minutes = Array.from({ length: 60 }, (_, i) => i);
  readonly timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  profile: Profile = { email: '', phone: '', country: 'Romania', dateOfBirth: '', occupation: '' };
  credit = { amount: 3000, monthlyIncome: 1500, termMonths: 12, purpose: '' };
  dialogKind = ''; dialogTitle = ''; pocketAction = 'deposit'; selectedPocket: Pocket | null = null;
  moveAmount = 100; selectedSchedule = 0; risk: Risk | null = null;
  private destroyed = new Subject<void>();
  private creditLoaded = false;
  constructor(public api: FinanceApiService, private route: ActivatedRoute, private dialog: MatDialog,
              private toast: MatSnackBar, private transfers: FrontEndService, public user: UserService,private router: Router) {}
  ngOnInit(): void {
    this.route.data.pipe(takeUntil(this.destroyed)).subscribe(data => {
      this.section = data['section'] ?? 'overview'; this.dialog.closeAll(); this.load();
    });
    timer(15000,15000).pipe(takeUntil(this.destroyed)).subscribe(() => {
      if (!document.hidden && !this.busy && !this.loading && ['overview','trading','payments','savings','credit','onboarding','security'].includes(this.section)) this.load(true);
    });
  }
  ngOnDestroy(): void { this.destroyed.next(); this.destroyed.complete(); this.dialog.closeAll(); }
  load(background = false): void {
    if (!background) {this.loading = true; this.error = '';}
    this.api.overview().pipe(takeUntil(this.destroyed)).subscribe({ next: data => {
      this.data = data; this.loading = false;
      if(this.section==='onboarding'&&(!data.onboardingAvailable||data.account.status!=='ACTIVE')){this.router.navigateByUrl('/dashboard/overview');return;}
      if (!background && data.profile) this.profile = { ...data.profile };
      if (this.section === 'credit' && !this.creditLoaded) {
        this.creditLoaded = true;
        const draft = data.credits.find(c => c.status === 'DRAFT');
        if (draft) this.selectCredit(draft.id);
      }
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
  get verified(): boolean { return this.data?.profile?.onboardingStatus === 'VERIFIED'; }
  get detailsSaved(): boolean { return !!this.data?.profile && this.data.profile.onboardingStatus !== 'NOT_STARTED'; }
  get creditEligible(): boolean { return this.active && this.verified && this.data?.creditEligible === true; }
  get creditBlockReason(): string {
    if(!this.active){const state=({FREEZE:'frozen',SUSPENDED:'suspended',BLOCKED:'blocked'} as Record<string,string>)[this.data?.account.status??'']??'restricted';return 'Credit applications are unavailable while your account is '+state+'.';}
    if(this.data?.profile?.onboardingStatus==='DECLINED')return 'Identity verification was declined. Contact support to review the decision.';
    if(this.data?.profile?.onboardingStatus==='UNDER_REVIEW'||!this.data?.onboardingAvailable)return 'Your identity is awaiting administrator review.';
    return 'Verify your identity before applying for credit.';
  }
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
  get paymentReady(): boolean { return this.active && !this.busy && !!this.payment.recipientIban.trim() && Number.isFinite(this.payment.amount) && Number(this.payment.amount) > 0; }
  openSchedulePicker(): void {
    if (!this.paymentReady) return;
    this.scheduleMinDate = new Date(); this.scheduleMinDate.setHours(0, 0, 0, 0);
    this.scheduleDate = new Date(this.scheduleMinDate); this.scheduleDate.setDate(this.scheduleDate.getDate() + 1);
    this.scheduleHour = 9; this.scheduleMinute = 0; this.scheduleError = '';
    this.dialog.open(this.schedulePicker, { width: '360px', maxWidth: '94vw', ariaLabel: 'Schedule transfer', autoFocus: false });
  }
  get scheduledDateTime(): Date | null {
    if (!this.scheduleDate) return null;
    const date = new Date(this.scheduleDate.getFullYear(), this.scheduleDate.getMonth(), this.scheduleDate.getDate(), this.scheduleHour, this.scheduleMinute);
    // Daylight-saving gaps must not silently move the selected local time.
    return date.getHours() === this.scheduleHour && date.getMinutes() === this.scheduleMinute ? date : null;
  }
  reviewSchedule(): void {
    const date = this.scheduledDateTime;
    if (!date || date.getTime() <= Date.now()) { this.scheduleError = 'Choose a future date and time.'; return; }
    this.payment.scheduledAt = date.toISOString(); this.dialog.closeAll(); this.reviewPayment(true);
  }
  reviewPayment(scheduled = false): void {
    if (!this.paymentReady) return;
    if (!scheduled) this.payment.scheduledAt = '';
    this.risk = null;
    this.open('payment', this.payment.scheduledAt ? 'Review scheduled transfer' : 'Review transfer');
    this.riskLoading = false;
    if (!this.isAdmin) return;
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
        this.mutate(this.api.post('scheduled-transfers', { ...payload, scheduledAt: date.toISOString() }), 'Transfer scheduled.', () => this.resetPayment());
      } else this.mutate(this.transfers.transfer(payload), 'Transfer completed.', () => this.resetPayment());
    }
  }
  private resetPayment(): void { this.payment = { recipientIban: '', amount: null, scheduledAt: '' }; }
  saveProfile(): void { if(!this.detailsSaved&&this.active)this.mutate(this.api.profile(this.profile), 'Details submitted. Add your identity document to finish onboarding.'); }
  selectCredit(id: number): void {
    const draft = this.data?.credits.find(c => c.id === id && c.status === 'DRAFT');
    if (!draft) return;
    this.currentCreditId = draft.id;
    this.credit = { purpose: draft.purpose, amount: Number(draft.amount), monthlyIncome: Number(draft.monthlyIncome), termMonths: draft.termMonths };
  }
  continueCredit(id: number): void {
    if (!this.creditEligible) return;
    this.selectCredit(id);
    if (this.creditSteps) this.creditSteps.selectedIndex = 0;
    this.creditWorkflow?.nativeElement.scrollIntoView({ block: 'start', behavior: 'smooth' });
  }
  startCredit(): void {
    if (!this.creditEligible) return;
    this.currentCreditId = null; this.credit = { amount: 3000, monthlyIncome: 1500, termMonths: 12, purpose: '' };
    if (this.creditSteps) this.creditSteps.selectedIndex = 0;
  }
  get creditChanged(): boolean {
    const draft = this.draft;
    return !draft || draft.purpose !== this.credit.purpose.trim() || Number(draft.amount) !== Number(this.credit.amount) || Number(draft.monthlyIncome) !== Number(this.credit.monthlyIncome) || draft.termMonths !== this.credit.termMonths;
  }
  saveCredit(): void {
    if (!this.creditEligible) return;
    const request = this.currentCreditId ? this.api.put<Credit>('credit/' + this.currentCreditId, this.credit) : this.api.post<Credit>('credit', this.credit);
    this.mutate(request, 'Application draft saved.', c => {
      this.currentCreditId = c.id;
      this.credit = { purpose: c.purpose, amount: Number(c.amount), monthlyIncome: Number(c.monthlyIncome), termMonths: c.termMonths };
    });
  }
  submitCredit(): void { if (this.currentCreditId&&this.creditEligible&&!this.creditChanged) this.mutate(this.api.post('credit/' + this.currentCreditId + '/submit'), 'Application submitted.'); }
  upload(event: Event, kind: string, credit: boolean): void {
    const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return;
    if((credit&&!this.creditEligible)||(!credit&&(!this.active||!this.data?.onboardingAvailable||!this.detailsSaved))){input.value='';return;}
    if (file.size > 5 * 1024 * 1024) { this.error = 'Choose a file smaller than 5 MB.'; input.value = ''; return; }
    if (credit && !this.currentCreditId) { this.error = 'Create an application draft first.'; input.value = ''; return; }
    this.mutate(this.api.upload(file, kind, credit ? this.currentCreditId! : undefined), credit?'Document saved.':'Identity submitted for administrator review.',()=>{if(!credit)this.router.navigateByUrl('/dashboard/overview');}); input.value = '';
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
