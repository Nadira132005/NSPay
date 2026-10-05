import { Component, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Sort } from '@angular/material/sort';
import { PageEvent } from '@angular/material/paginator';
import { Subject, Subscription, takeUntil } from 'rxjs';
import { FinanceApiService, DocumentRecord } from '../utils/finance-api.service';
@Component({selector:'app-credit-review',templateUrl:'./credit-review.component.html',styleUrls:['./workspace.component.scss','./review-tables.scss']})
export class CreditReviewComponent implements OnInit,OnDestroy {
  @ViewChild('reviewDialog') reviewDialog!:TemplateRef<unknown>;
  items:any[]=[];total=0;page=0;size=20;search='';status='UNDER_REVIEW';sort='createdAt';direction='desc';loading=false;busy=false;error='';selected:any=null;note='';documents:DocumentRecord[]=[];documentsLoading=false;
  private destroyed=new Subject<void>();private request?:Subscription;
  constructor(private http:HttpClient,private api:FinanceApiService,private dialog:MatDialog,private toast:MatSnackBar){}
  ngOnInit():void{this.load();}
  ngOnDestroy():void{this.destroyed.next();this.destroyed.complete();this.request?.unsubscribe();this.dialog.closeAll();}
  load():void{
    this.request?.unsubscribe();this.loading=true;this.error='';
    const params=new HttpParams().set('page',this.page).set('size',this.size).set('search',this.search).set('status',this.status).set('sort',this.sort).set('direction',this.direction);
    this.request=this.http.get<any>('/api/finance/credit/reviews',{params}).pipe(takeUntil(this.destroyed)).subscribe({next:d=>{this.items=d.items;this.total=d.total;this.loading=false},error:e=>{this.loading=false;this.error=this.message(e)}});
  }
  apply():void{this.page=0;this.load();}
  reset():void{this.search='';this.status='UNDER_REVIEW';this.apply();}
  sorted(event:Sort):void{this.sort=event.active;this.direction=event.direction||'desc';this.apply();}
  paged(event:PageEvent):void{this.page=event.pageIndex;this.size=event.pageSize;this.load();}
  inspect(row:any):void{
    this.selected=row;this.note='';this.documents=[];this.documentsLoading=true;this.error='';
    this.dialog.open(this.reviewDialog,{width:'680px',maxWidth:'94vw'});
    this.api.creditDocuments(row.application.id).pipe(takeUntil(this.destroyed)).subscribe({next:d=>{this.documents=d;this.documentsLoading=false},error:e=>{this.documentsLoading=false;this.error=this.message(e)}});
  }
  decide(decision:string):void{
    if(this.busy||!this.selected||this.selected.application.status!=='UNDER_REVIEW'||this.note.trim().length<5)return;
    this.busy=true;this.error='';this.api.post('credit/'+this.selected.application.id+'/review',{decision,note:this.note}).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();this.toast.open('Credit decision recorded.','Dismiss',{duration:4000})},error:e=>{this.busy=false;this.error=this.message(e)}});
  }
  private message(e:any):string{return e?.error?.message||'Credit applications could not be loaded.';}
}
