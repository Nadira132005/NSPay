import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Subject, takeUntil } from 'rxjs';
import { Activity } from '../utils/finance-api.service';
@Component({selector:'app-security-panel',templateUrl:'./security-panel.component.html',styleUrls:['./workspace.component.scss']})
export class SecurityPanelComponent implements OnInit,OnDestroy {
  @Input() transactions: Activity[]=[];@Input() admin=false;@Input() status='ACTIVE';@Output() changed=new EventEmitter<void>();
  @ViewChild('decisionDialog') decisionDialog!: TemplateRef<unknown>;
  reports:any[]=[];history:any[]=[];restriction:any={};selected:any=null;action='report';reason='';days=30;busy=false;error='';private destroyed=new Subject<void>();
  constructor(private http:HttpClient,private dialog:MatDialog){}
  ngOnInit():void{this.load();}
  ngOnDestroy():void{this.destroyed.next();this.destroyed.complete();this.dialog.closeAll();}
  load():void{
    this.http.get<any>('/api/security').pipe(takeUntil(this.destroyed)).subscribe({next:d=>{if(!this.admin)this.reports=d.reports;this.history=d.history;this.restriction=d.restriction;},error:e=>this.error=this.message(e)});
    if(this.admin)this.http.get<any[]>('/api/security/reports').pipe(takeUntil(this.destroyed)).subscribe({next:r=>this.reports=r,error:e=>this.error=this.message(e)});
  }
  open(selected:any,action:string):void{this.selected=selected;this.action=action;this.reason='';this.days=30;this.error='';this.dialog.open(this.decisionDialog,{width:'520px',maxWidth:'94vw'});}
  reported(id:number):boolean{return this.reports.some(r=>r.transactionId===id);}
  confirm():void{if(this.busy)return;this.busy=true;const path=this.action==='report'?'transactions/'+this.selected.id+'/report':'reports/'+this.selected.id+'/review';this.http.post('/api/security/'+path,{reason:this.reason,confirmed:this.action==='confirm',days:this.days}).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();this.changed.emit();},error:e=>{this.busy=false;this.error=this.message(e);}});}
  private message(e:any):string{return e?.error?.message || 'The request could not be completed.';}
}
