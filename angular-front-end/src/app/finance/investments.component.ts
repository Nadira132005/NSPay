import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Subject, takeUntil, forkJoin } from 'rxjs';
interface Instrument {symbol: string; name: string; group: string; currency: string;}
interface Quote {id: string; symbol: string; price: number; eurPrice: number; change: number; exchangeRate: number; priceAt: string; exchangeRateAt: string; expiresAt: string; marketOpen: boolean; provider: string;}
interface Order {id: number; symbol: string; side: string; quantity: number; price: number; total: number; createdAt: string; status: string;}
@Component({selector: 'app-investments', templateUrl: './investments.component.html', styleUrls: ['./workspace.component.scss']})
export class InvestmentsComponent implements OnInit, OnDestroy {
  @Input() mainBalance = 0; @Input() active = true; @Output() changed = new EventEmitter<void>();
  @ViewChild('confirmation') confirmation!: TemplateRef<unknown>;
  cash = 0; orders: Order[] = []; catalog: Instrument[] = []; configured = false;
  selected: Instrument | null = null; quote: Quote | null = null; range = '1M'; group = 'All'; search = '';
  points: {datetime: string; close: number}[] = []; chartPath = ''; chartLow = 0; chartHigh = 0;
  loading = false; busy = false; error = ''; marketError = ''; side = 'BUY'; quantity = 1;
  action = ''; amount = 100; requestKey = ''; private marketRequest = 0; private destroyed = new Subject<void>();
  constructor(private http: HttpClient, private dialog: MatDialog) {}
  ngOnInit(): void {this.load();}
  ngOnDestroy(): void {this.destroyed.next();this.destroyed.complete();this.dialog.closeAll();}
  load(): void {
    this.http.get<any>('/api/investments').pipe(takeUntil(this.destroyed)).subscribe({next: d => {this.cash = Number(d.cash);this.orders = d.orders;this.catalog = d.catalog;this.configured = d.configured;if (!this.selected && this.catalog.length) this.select(this.catalog[0]);},error: e => this.error = this.message(e)});
  }
  get groups(): string[] {return ['All', ...new Set(this.catalog.map(i => i.group))];}
  get instruments(): Instrument[] {const s = this.search.trim().toLowerCase();return this.catalog.filter(i => (this.group === 'All' || i.group === this.group) && (i.name.toLowerCase().includes(s) || i.symbol.toLowerCase().includes(s)));}
  units(symbol: string): number {return this.orders.filter(o => o.symbol === symbol).reduce((sum,o) => sum + (o.side === 'BUY' ? 1 : -1) * Number(o.quantity),0);}
  get holdings(): string[] {return [...new Set(this.orders.map(o => o.symbol))].filter(s => this.units(s) > 0.0000001);}
  name(symbol: string): string {return this.catalog.find(i => i.symbol === symbol)?.name ?? symbol;}
  select(i: Instrument): void {this.selected = i;this.refresh();}
  refresh(): void {
    if (!this.selected || !this.configured) return;
    const ticket = ++this.marketRequest;this.loading = true;this.marketError = '';this.quote = null;this.points = [];this.chartPath = '';
    const symbol = this.selected.symbol;
    this.http.get<Quote>('/api/investments/' + symbol + '/quote').pipe(takeUntil(this.destroyed)).subscribe({next: q => {if (ticket === this.marketRequest) {this.quote = q;this.loading = false;}},error: e => {if (ticket === this.marketRequest) {this.marketError = this.message(e);this.loading = false;}}});
    this.http.get<any>('/api/investments/' + symbol + '/chart?range=' + this.range).pipe(takeUntil(this.destroyed)).subscribe({next: d => {if (ticket !== this.marketRequest) return;this.points = [...d.values].reverse().map((v: any) => ({datetime: v.datetime,close: Number(v.close)})).filter(p => Number.isFinite(p.close));this.draw();},error: e => {if (ticket === this.marketRequest) this.marketError = this.message(e);}});
  }
  private draw(): void {
    this.chartLow = Math.min(...this.points.map(p => p.close));this.chartHigh = Math.max(...this.points.map(p => p.close));
    const spread = this.chartHigh - this.chartLow || 1;
    this.chartPath = this.points.map((p,i) => (i ? 'L' : 'M') + (20 + i / Math.max(1,this.points.length - 1) * 680).toFixed(2) + ',' + (220 - (p.close - this.chartLow) / spread * 200).toFixed(2)).join(' ');
  }
  get quoteValid(): boolean {return !!this.quote && new Date(this.quote.expiresAt).getTime() > Date.now();}
  review(action: string): void {
    this.error = '';this.action = action;this.requestKey = crypto.randomUUID();
    if (action === 'order' && !this.quoteValid) {this.marketError = 'Refresh the price before reviewing an order.';return;}
    this.dialog.open(this.confirmation,{width: '480px',maxWidth: '94vw',disableClose: false});
  }
  confirm(): void {
    if (this.busy) return;this.busy = true;this.error = '';
    const payload = this.action === 'order' ? {symbol: this.selected?.symbol,quoteId: this.quote?.id,side: this.side,quantity: this.quantity,requestKey: this.requestKey} : {amount: this.amount,requestKey: this.requestKey};
    this.http.post('/api/investments/' + (this.action === 'order' ? 'orders' : this.action),payload).pipe(takeUntil(this.destroyed)).subscribe({next: () => {this.busy = false;this.dialog.closeAll();this.load();this.changed.emit();},error: e => {this.busy = false;this.error = this.message(e);}});
  }
  private message(e: any): string {return e?.error?.message || 'The request could not be completed.';}
}
