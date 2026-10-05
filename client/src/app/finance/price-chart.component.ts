import { Component, Input, OnChanges } from '@angular/core';
export interface PricePoint { datetime: string; close: number; }
@Component({selector: 'app-price-chart', templateUrl: './price-chart.component.html', styleUrls: ['./price-chart.component.scss']})
export class PriceChartComponent implements OnChanges {
  @Input() points: PricePoint[] = []; @Input() symbol = ''; @Input() range = '1M'; @Input() currency = 'USD'; @Input() kind = 'price';
  readonly gradientId = 'chart-area-' + Math.random().toString(36).slice(2);
  line = ''; area = ''; low = 0; high = 0; index: number | null = null;
  ticks: { y: number; value: number }[] = []; dates: { x: number; label: string; anchor: string }[] = [];
  ngOnChanges(): void {
    this.index = null;if (!this.points.length) {this.line = '';this.area = '';return;}
    const prices = this.points.map(p => p.close);const min = Math.min(...prices), max = Math.max(...prices);
    const pad = Math.max((max - min) * .15, max * .002, .01);this.low = min - pad;this.high = max + pad;
    this.line = this.points.map((p,i) => (i ? 'L' : 'M') + this.x(i).toFixed(2) + ',' + this.y(p.close).toFixed(2)).join(' ');
    this.area = this.line + ' L' + this.x(this.points.length - 1) + ',242 L76,242 Z';
    this.ticks = [.15,.5,.85].map(r => ({y: 20 + r * 222,value: this.high - r * (this.high - this.low)}));
    this.dates = [0,Math.floor((this.points.length - 1) / 2),this.points.length - 1].map((i,n) => ({x: this.x(i),label: this.date(this.points[i].datetime),anchor: ['start','middle','end'][n]}));
  }
  x(i: number): number {return 76 + i / Math.max(1,this.points.length - 1) * 662;}
  y(price: number): number {return 242 - (price - this.low) / (this.high - this.low) * 222;}
  get focused(): PricePoint | null {return this.index === null ? null : this.points[this.index];}
  get change(): number {const p = this.focused ?? this.points[this.points.length - 1];return p && this.points[0]?.close ? (p.close / this.points[0].close - 1) * 100 : 0;}
  pointer(event: PointerEvent): void {
    const box = (event.currentTarget as SVGSVGElement).getBoundingClientRect();
    const x = (event.clientX - box.left) / box.width * 760;
    this.index = Math.max(0,Math.min(this.points.length - 1,Math.round((x - 76) / 662 * (this.points.length - 1))));
  }
  keyboard(event: KeyboardEvent): void {
    if (event.key === 'Escape') {this.index = null;return;}
    if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) return;event.preventDefault();
    this.index = event.key === 'Home' ? 0 : event.key === 'End' ? this.points.length - 1 : Math.max(0,Math.min(this.points.length - 1,(this.index ?? this.points.length - 1) + (event.key === 'ArrowLeft' ? -1 : 1)));
  }
  readoutDate(value: string): string {return new Intl.DateTimeFormat('en-GB',{day:'2-digit',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit',timeZone:this.kind === 'profit' ? 'UTC' : 'America/New_York'}).format(new Date(value));}
  private date(value: string): string {
    const d = new Date(value);return Number.isNaN(d.getTime()) ? value : new Intl.DateTimeFormat('en-GB',['1D','LIVE'].includes(this.range) ? {hour: '2-digit',minute: '2-digit',timeZone: this.kind === 'profit' ? 'UTC' : 'America/New_York'} : {day: '2-digit',month: 'short',timeZone: this.kind === 'profit' ? 'UTC' : 'America/New_York'}).format(d);
  }
}
