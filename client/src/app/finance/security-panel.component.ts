import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Sort } from '@angular/material/sort';
import { PageEvent } from '@angular/material/paginator';
import { Subject, Subscription, takeUntil, timer } from 'rxjs';
import { Activity } from '../utils/finance-api.service';
@Component({selector:'app-security-panel',templateUrl:'./security-panel.component.html',styleUrls:['./workspace.component.scss','./review-tables.scss']})
export class SecurityPanelComponent implements OnInit,OnDestroy {
  @Input() transactions:Activity[]=[];@Input() admin=false;@Input() status='ACTIVE';@Output() changed=new EventEmitter<void>();
  @ViewChild('decisionDialog') decisionDialog!:TemplateRef<unknown>;
  reports:any[]=[];activity:any[]=[];restriction:any={};payments:any[]=[];selected:any=null;evidence:any=null;
  reason='';publicNote='';privateNote='';decision='FREEZE';days=30;busy=false;loading=false;error='';
  page=0;size=20;total=0;search='';state='All';reportFilter='all';sort='misalignmentScore';direction='desc';minScore=0;metric='misalignmentScore';type='';indexing=false;
  metrics=[{key:'misalignmentScore',label:'Misalignment'},{key:'amountScore',label:'Amount deviation'},{key:'timingScore',label:'Unusual timing'},{key:'velocityScore',label:'Payment frequency'},{key:'recipientScore',label:'Recipient novelty'},{key:'reportingScore',label:'Customer reports'},{key:'limitScore',label:'Daily limit usage'}];
  private destroyed=new Subject<void>();private request?:Subscription;
  constructor(private http:HttpClient,private dialog:MatDialog,private toast:MatSnackBar){}
  ngOnInit():void{this.load();timer(15000,15000).pipe(takeUntil(this.destroyed)).subscribe(()=>{if(!document.hidden&&!this.busy&&!this.loading&&!this.selected)this.load()});}
  ngOnDestroy():void{this.destroyed.next();this.destroyed.complete();this.request?.unsubscribe();this.dialog.closeAll();}
  load():void{
    this.request?.unsubscribe();this.loading=true;this.error='';
    const query=new HttpParams().set('page',this.page).set('size',this.size).set('search',this.search).set('state',this.state).set('reported',this.reportFilter).set('sort',this.sort).set('direction',this.direction).set('minScore',this.minScore).set('metric',this.metric).set('type',this.type);
    this.request=this.http.get<any>(this.admin?'/api/security/payments':'/api/security',{params:this.admin?query:undefined}).pipe(takeUntil(this.destroyed)).subscribe({next:d=>{this.loading=false;if(this.admin){this.payments=d.items;this.total=d.total;this.indexing=d.indexing;}else{this.reports=d.reports;this.activity=d.activity;this.restriction=d.restriction;}},error:e=>{this.loading=false;this.error=this.message(e)}});
  }
  apply():void{this.page=0;this.load();}
  resetFilters():void{this.search='';this.state='All';this.reportFilter='all';this.minScore=0;this.metric='misalignmentScore';this.type='';this.apply();}
  sorted(event:Sort):void{this.sort=event.active;this.direction=event.direction||'desc';this.apply();}
  paged(event:PageEvent):void{this.page=event.pageIndex;this.size=event.pageSize;this.load();}
  open(selected:any):void{
    this.selected=selected;this.reason='';this.publicNote='';this.privateNote='';this.decision='FREEZE';this.days=30;this.evidence=null;this.error='';
    const ref=this.dialog.open(this.decisionDialog,{width:this.admin?'700px':'480px',maxWidth:'94vw'});
    ref.afterClosed().pipe(takeUntil(this.destroyed)).subscribe(()=>this.selected=null);
    if(this.admin)this.http.get('/api/security/transactions/'+selected.transactionId+'/evidence').pipe(takeUntil(this.destroyed)).subscribe({next:e=>this.evidence=e,error:e=>this.error=this.message(e)});
  }
  reported(id:number):boolean{return this.reports.some(r=>r.transactionId===id);}
  scoreClass(score:number):string{return score>=70?'score-high':score>=35?'score-medium':'score-low';}
  confirm():void{
    if(this.busy||!this.selected)return;this.busy=true;this.error='';
    const path=this.admin?'transactions/'+this.selected.transactionId+'/review':'transactions/'+this.selected.id+'/report';
    const body=this.admin?{status:this.decision==='DISMISSED'?null:this.decision,reason:this.reason,days:this.days,publicNote:this.publicNote,privateNote:this.privateNote}:{reason:this.reason};
    this.http.post('/api/security/'+path,body).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();this.changed.emit();this.toast.open(this.admin?'Payment decision recorded.':'Report submitted for administrator review.','Dismiss',{duration:4000});},error:e=>{this.busy=false;this.error=this.message(e)}});
  }
  actionLabel(status:string):string{return ({FREEZE:'froze',SUSPENDED:'suspended',BLOCKED:'blocked',ACTIVE:'restored'} as Record<string,string>)[status]??'updated';}
  private message(e:any):string{return e?.error?.message||'The request could not be completed.';}
}
