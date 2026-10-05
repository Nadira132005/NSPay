import { Component, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { Subject, Subscription, takeUntil, timer } from 'rxjs';
import { UserService } from '../utils/user.service';
@Component({selector:'app-account-limits',templateUrl:'./account-limits.component.html',styleUrls:['./workspace.component.scss','./review-tables.scss','./account-limits.component.scss']})
export class AccountLimitsComponent implements OnInit,OnDestroy {
  @ViewChild('limitsDialog') limitsDialog!:TemplateRef<unknown>;
  rows:any[]=[];total=0;page=0;size=20;search='';status='All';sort='cardHolderName';direction='asc';loading=false;busy=false;error='';selected:any=null;count:number|null=null;amount:number|null=null;
  private destroyed=new Subject<void>();private request?:Subscription;
  constructor(private http:HttpClient,private dialog:MatDialog,private toast:MatSnackBar,public auth:UserService){}
  ngOnInit():void{this.load();timer(15000,15000).pipe(takeUntil(this.destroyed)).subscribe(()=>{if(!document.hidden&&!this.selected&&!this.loading)this.load()});}
  ngOnDestroy():void{this.destroyed.next();this.destroyed.complete();this.request?.unsubscribe();this.dialog.closeAll();}
  load():void{this.request?.unsubscribe();this.loading=true;this.error='';const params=new HttpParams().set('page',this.page).set('size',this.size).set('search',this.search).set('status',this.status).set('sort',this.sort).set('direction',this.direction);this.request=this.http.get<any>('/api/account-limits',{params}).pipe(takeUntil(this.destroyed)).subscribe({next:d=>{this.rows=d.items;this.total=d.total;this.loading=false},error:e=>{this.error=e?.error?.message||'Accounts could not be loaded.';this.loading=false}});}
  apply():void{this.page=0;this.load();}
  reset():void{this.search='';this.status='All';this.apply();}
  sorted(s:Sort):void{this.sort=s.active;this.direction=s.direction||'asc';this.apply();}
  paged(e:PageEvent):void{this.page=e.pageIndex;this.size=e.pageSize;this.load();}
  edit(row:any):void{this.selected=row;this.count=row.usage.dailyTxLimit;this.amount=row.usage.dailyTxSumLimit;this.error='';this.dialog.open(this.limitsDialog,{width:'480px',maxWidth:'94vw'}).afterClosed().pipe(takeUntil(this.destroyed)).subscribe(()=>this.selected=null);}
  get valid():boolean{return this.count!==null&&Number.isInteger(this.count)&&this.count>=1&&this.count<=1000000&&this.amount!==null&&Number.isFinite(this.amount)&&this.amount>=0.01&&this.amount<=9999999999.99&&Math.abs(this.amount*100-Math.round(this.amount*100))<0.0001;}
  save():void{if(this.busy||!this.valid||!this.selected)return;this.busy=true;this.http.put('/api/account-limits/'+this.selected.id,{dailyTxLimit:this.count,dailyTxSumLimit:this.amount}).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();this.toast.open('Daily limits updated.','Dismiss',{duration:4000})},error:e=>{this.busy=false;this.error=e?.error?.message||'Limits could not be updated.'}});}
  bar(percent:number):number{return Math.min(100,Math.max(0,percent));}
  label(level:string):string{return ({NORMAL:'Within limits',NEAR:'Near limit',REACHED:'Limit reached',EXCEEDED:'Over limit'} as Record<string,string>)[level]||'Within limits';}
}
