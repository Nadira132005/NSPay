import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Subject, takeUntil, timer } from 'rxjs';
import { UserService } from '../utils/user.service';
import { PricePoint } from './price-chart.component';
interface Instrument {symbol: string; name: string; group: string; currency: string;}
interface Quote {id: string; symbol: string; price: number; eurPrice: number; change: number; exchangeRate: number; priceAt: string; exchangeRateAt: string; expiresAt: string; marketOpen: boolean; provider: string; marketProvider: string; synthetic: boolean; exchangeRateSource: string;}
interface Order {id: number; symbol: string; side: string; quantity: number; price: number; total: number; createdAt: string; status: string;}
interface Holding {symbol: string; provider: string; quantity: number; costBasis: number; averageCost: number; marketValue: number | null; price: number | null; eurPrice: number | null; unrealizedGain: number | null; unrealizedGainPercent: number | null; allocationPercent: number | null; priceAt: string | null; valuationStatus: string;}
interface Portfolio {cash: number; holdings: Holding[]; orders: Order[]; orderCount: number; catalog: Instrument[]; configured: boolean; totalValue: number | null; holdingsValue: number | null; unrealizedGain: number | null; unrealizedGainPercent: number | null; realizedGain: number; totalGain: number | null; valuationStatus: string; valuationError: string | null; valuedAt: string; priceAt: string | null; provider: string;streaming: boolean;}
@Component({selector: 'app-investments', templateUrl: './investments.component.html', styleUrls: ['./workspace.component.scss','./investments.component.scss','./investment-order.scss']})
export class InvestmentsComponent implements OnInit, OnDestroy {
  @Input() mainBalance = 0; @Input() active = true; @Output() changed = new EventEmitter<void>();
  @ViewChild('confirmation') confirmation!: TemplateRef<unknown>;
  cash = 0; orders: Order[] = []; catalog: Instrument[] = []; holdings: Holding[] = []; configured = false; portfolio: Portfolio | null = null;
  selected: Instrument | null = null; quote: Quote | null = null; quotes: Record<string,Quote> = {}; range = '1M'; group = 'All'; search = '';
  readonly groupIcons: Record<string,string> = {All:'🌐',Technology:'💻',Consumer:'🛍️',Finance:'🏦',ETFs:'📈'};
  profitPoints: PricePoint[] = [];profitRange = '1M';profitError = '';streaming = false;streamStatus = 'CONNECTING';
  private stream: EventSource | null = null;private liveProfit: PricePoint[] = [];private livePrices: Record<string,PricePoint[]> = {};private historyProfit: PricePoint[] = [];
  private lastFrameAt = 0;
  points: PricePoint[] = []; loading = false; chartLoading = false; busy = false; error = ''; marketError = ''; pricesError = ''; side = 'BUY'; quantity = 1;
  action = ''; amount = 100; requestKey = ''; orderPage = 0; orderCount = 0; historyLoading = false;
  reviewedQuote: Quote | null = null; reviewedQuantity = 0; reviewedSide = ''; private dialogOpen = false;
  private portfolioRequest = 0; private marketRequest = 0; private chartRequest = 0; private destroyed = new Subject<void>();
  constructor(private http: HttpClient, private dialog: MatDialog, private user: UserService) {}
  ngOnInit(): void {
    this.load();this.loadPrices();this.loadPerformance();
    timer(5000,5000).pipe(takeUntil(this.destroyed)).subscribe(() => {
      if (this.streaming && !document.hidden && Date.now() - this.lastFrameAt > 15000) {this.streamStatus = 'RECONNECTING';this.connectStream();}
    });
    timer(60000,60000).pipe(takeUntil(this.destroyed)).subscribe(() => {if (!document.hidden && !this.busy && !this.dialogOpen) {this.load(false);if (!this.streaming) {this.loadPrices();this.loadPerformance();if (this.selected) this.refresh();}}});
  }
  ngOnDestroy(): void {this.stream?.close();this.destroyed.next();this.destroyed.complete();this.dialog.closeAll();}
  load(resetHistory = true): void {
    const ticket = ++this.portfolioRequest;
    this.http.get<Portfolio>('/api/investments').pipe(takeUntil(this.destroyed)).subscribe({next: d => {
      if (ticket !== this.portfolioRequest) return;
      if (d.streaming && !this.streaming) {this.streaming = true;this.range = 'LIVE';this.profitRange = 'LIVE';this.profitPoints = [];this.connectStream();}
      this.applyPortfolio(d);this.catalog = d.catalog;this.configured = d.configured;this.orderCount = d.orderCount;
      if (resetHistory || this.orderPage === 0) {this.orders = d.orders;this.orderPage = 0;}
      if (!this.selected && this.catalog.length) this.select(this.catalog[0]);
    },error: e => this.error = this.message(e)});
  }
  loadPerformance(): void {
    if (this.profitRange === 'LIVE') {this.profitPoints = [...this.liveProfit];return;}
    const range = this.profitRange;this.http.get<{day: string;totalGain: number}[]>('/api/investments/performance?range=' + this.profitRange).pipe(takeUntil(this.destroyed)).subscribe({next: d => {if (this.profitRange !== range) return;this.historyProfit = d.map(p => ({datetime:p.day+'T12:00:00Z',close:Number(p.totalGain)}));this.profitPoints = [...this.historyProfit];this.profitError = '';},error:e => this.profitError=this.message(e)});
  }
  loadPrices(): void {
    this.http.get<Record<string,Quote>>('/api/investments/quotes').pipe(takeUntil(this.destroyed)).subscribe({next: prices => {this.quotes = prices;this.pricesError = '';},error: e => this.pricesError = this.message(e)});
  }
  history(page: number): void {
    if (page < 0 || page * 20 >= this.orderCount || this.historyLoading) return;this.historyLoading = true;
    this.http.get<{content: Order[];totalElements: number}>('/api/investments/orders?page=' + page + '&size=20').pipe(takeUntil(this.destroyed)).subscribe({next: d => {this.orders = d.content;this.orderCount = d.totalElements;this.orderPage = page;this.historyLoading = false;},error: e => {this.historyLoading = false;this.error = this.message(e);}});
  }
  get groups(): string[] {return ['All', ...new Set(this.catalog.map(i => i.group))];}
  get instruments(): Instrument[] {const s = this.search.trim().toLowerCase();return this.catalog.filter(i => (this.group === 'All' || i.group === this.group) && (i.name.toLowerCase().includes(s) || i.symbol.toLowerCase().includes(s)));}
  units(symbol: string): number {return this.holdings.filter(h => h.symbol === symbol && (!this.quote || h.provider === this.quote.marketProvider)).reduce((n,h) => n + Number(h.quantity),0);}
  name(symbol: string): string {return this.catalog.find(i => i.symbol === symbol)?.name ?? symbol;}
  chooseHolding(h: Holding): void {const instrument = this.catalog.find(i => i.symbol === h.symbol);if (instrument) this.select(instrument);}
  select(i: Instrument): void {this.selected = i;this.refresh();}
  setRange(range: string): void {this.range = range;this.loadChart();}
  refresh(): void {
    if (!this.selected || !this.configured) return;
    const ticket = ++this.marketRequest;this.loading = true;this.marketError = '';this.quote = this.quotes[this.selected.symbol] ?? null;
    this.http.get<Quote>('/api/investments/' + this.selected.symbol + '/quote').pipe(takeUntil(this.destroyed)).subscribe({next: q => {if (ticket === this.marketRequest) {this.quote = q;this.loading = false;}},error: e => {if (ticket === this.marketRequest) {this.marketError = this.message(e);this.loading = false;}}});
    this.loadChart();
  }
  private loadChart(): void {
    if (!this.selected || !this.configured) return;const ticket = ++this.chartRequest;if (this.range === 'LIVE') this.points = [...(this.livePrices[this.selected.symbol] ?? [])];this.chartLoading = true;if (this.range !== 'LIVE') this.points = [];this.marketError = '';
    this.http.get<any>('/api/investments/' + this.selected.symbol + '/chart?range=' + this.range).pipe(takeUntil(this.destroyed)).subscribe({next: d => {if (ticket !== this.chartRequest) return;this.points = [...d.values].reverse().map((v: any) => ({datetime: /Z$|[+-]\d\d:\d\d$/.test(v.datetime) ? v.datetime : v.datetime.replace(' ','T') + (v.datetime.length === 10 ? 'T00:00:00Z' : 'Z'),close: Number(v.close)})).filter(p => Number.isFinite(p.close));if (this.range === 'LIVE' && this.selected) {const buffered = this.livePrices[this.selected.symbol] ?? [];const last = this.points[this.points.length-1]?.datetime ?? '';this.points = [...this.points,...buffered.filter(p => p.datetime > last)].slice(-600);this.livePrices[this.selected.symbol] = [...this.points];}this.chartLoading = false;},error: e => {if (ticket === this.chartRequest) {this.marketError = this.message(e);this.chartLoading = false;}}});
  }
  get quoteValid(): boolean {return this.valid(this.quote);}
  valid(q: Quote | null): boolean {return !!q && new Date(q.expiresAt).getTime() > Date.now();}
  get canReview(): boolean {return this.active && (!this.streaming || this.streamStatus === 'LIVE') && this.quoteValid && this.quantity > 0 && (this.side === 'BUY' ? (this.quote?.eurPrice ?? 0) * this.quantity <= this.cash : this.quantity <= this.units(this.selected?.symbol ?? ''));}
  review(action: string): void {
    this.error = '';this.action = action;this.requestKey = crypto.randomUUID();
    if (action === 'order' && this.streaming && this.streamStatus !== 'LIVE') {this.error = 'Orders resume when live prices reconnect.';return;}
    if (action === 'order' && !this.quoteValid) {this.marketError = 'Refresh the price before reviewing an order.';return;}
    if (action === 'order' && this.streaming) {
      this.loading = true;const symbol = this.selected!.symbol;
      this.http.get<Quote>('/api/investments/' + symbol + '/quote').pipe(takeUntil(this.destroyed)).subscribe({next:q => {this.loading = false;this.openReview(action,q);},error:e => {this.loading = false;this.error = this.message(e);}});return;
    }
    this.openReview(action,this.quote);
  }
  private openReview(action: string, quote: Quote | null): void {
    this.action = action;this.reviewedQuote = quote;this.reviewedQuantity = this.quantity;this.reviewedSide = this.side;this.dialogOpen = true;
    this.dialog.open(this.confirmation,{width: '480px',maxWidth: '94vw',disableClose: false}).afterClosed().pipe(takeUntil(this.destroyed)).subscribe(() => this.dialogOpen = false);
  }
  confirm(): void {
    if (this.busy) return;
    if (this.action === 'order' && !this.valid(this.reviewedQuote)) {this.error = 'Quote expired. Close this dialog, refresh the price and review again.';return;}
    this.busy = true;this.error = '';
    const payload = this.action === 'order' ? {symbol: this.reviewedQuote?.symbol,quoteId: this.reviewedQuote?.id,side: this.reviewedSide,quantity: this.reviewedQuantity,requestKey: this.requestKey} : {amount: this.amount,requestKey: this.requestKey};
    this.http.post('/api/investments/' + (this.action === 'order' ? 'orders' : this.action),payload).pipe(takeUntil(this.destroyed)).subscribe({next: () => {this.busy = false;this.dialog.closeAll();this.load();this.loadPrices();this.loadPerformance();this.changed.emit();},error: e => {this.busy = false;this.error = this.message(e);}});
  }
  private connectStream(): void {
    this.lastFrameAt = Date.now();
    this.stream?.close();this.stream = new EventSource('/api/investments/stream');
    this.stream.addEventListener('prices',event => {
      this.lastFrameAt = Date.now();
      const frame = JSON.parse((event as MessageEvent).data);this.streamStatus = frame.status;if (frame.status === 'LIVE') {this.pricesError = '';if (this.marketError.includes('reconnecting')) this.marketError = '';}
      if (!this.portfolio) return;
      this.applyPortfolio({...this.portfolio,...frame.portfolio});
      for (const [symbol,value] of Object.entries(frame.prices) as [string,any][]) {
        const old = this.quotes[symbol] ?? this.quote;
        const q: Quote = {...old!,id:'',symbol,price:Number(value.price),eurPrice:Number(value.price)*Number(frame.exchangeRate),exchangeRate:Number(frame.exchangeRate),exchangeRateAt:frame.at,exchangeRateSource:'Configured sample rate',marketProvider:frame.marketProvider,change:Number(value.change),priceAt:value.priceAt,expiresAt:new Date(new Date(value.priceAt).getTime()+15000).toISOString(),marketOpen:true,provider:'CuckooTrade',synthetic:true};
        this.quotes[symbol] = q;if (this.selected?.symbol === symbol) this.quote = q;
        const buffer = this.livePrices[symbol] ?? [];
        if (!buffer.length || buffer[buffer.length-1].datetime < value.priceAt) this.livePrices[symbol] = [...buffer,{datetime:value.priceAt,close:q.price}].slice(-600);
      }
      if (this.range === 'LIVE' && this.selected) this.points = [...(this.livePrices[this.selected.symbol] ?? [])];
      if (frame.portfolio.totalGain !== null) {
        this.liveProfit = [...this.liveProfit,{datetime:frame.at,close:Number(frame.portfolio.totalGain)}].slice(-600);
        if (this.profitRange === 'LIVE') this.profitPoints = [...this.liveProfit];
        else {const day=frame.at.slice(0,10);this.profitPoints = [...this.historyProfit.filter(p => !p.datetime.startsWith(day)),{datetime:frame.at,close:Number(frame.portfolio.totalGain)}];}
      }
    });
    this.stream.onerror = () => {this.streamStatus = 'RECONNECTING';if (!this.user.isLoggedIn()) {this.stream?.close();this.user.toLogin();}};
  }
  retryPrices(): void {
    this.streamStatus = 'CONNECTING';this.connectStream();this.load(false);this.loadPrices();this.refresh();
  }
  private applyPortfolio(next: Portfolio): void {
    const previous = this.portfolio;
    // Display the last complete valuation only while the underlying owned accounting state is unchanged.
    const accounting = (p: Portfolio) => JSON.stringify([p.cash,p.realizedGain,p.holdings.map(h => [h.symbol,h.provider,h.quantity,h.costBasis]).sort((a,b) => JSON.stringify(a).localeCompare(JSON.stringify(b)))]);
    if (this.streaming && next.valuationStatus === 'INCOMPLETE' && previous?.totalValue != null && accounting(previous) === accounting(next)) {
      next = {...next,holdings:previous.holdings,totalValue:previous.totalValue,holdingsValue:previous.holdingsValue,unrealizedGain:previous.unrealizedGain,unrealizedGainPercent:previous.unrealizedGainPercent,totalGain:previous.totalGain,priceAt:previous.priceAt,valuedAt:previous.valuedAt};
    }
    this.portfolio = next;this.cash = Number(next.cash);this.holdings = next.holdings;
  }
  private message(e: any): string {return e?.error?.message || 'The request could not be completed.';}
}
