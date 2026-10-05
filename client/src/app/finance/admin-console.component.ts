import { Component, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Subject, takeUntil } from 'rxjs';
import { UserService } from '../utils/user.service';
@Component({selector: 'app-admin-console', templateUrl: './admin-console.component.html', styleUrls: ['./workspace.component.scss','./review-tables.scss']})
export class AdminConsoleComponent implements OnInit, OnDestroy {
  @ViewChild('statusDialog') statusDialog!: TemplateRef<unknown>;
  @ViewChild('verificationDialog') verificationDialog!: TemplateRef<unknown>;
  verificationUser: any = null; verificationRecord: any = null; verificationNote = ''; verificationLoading = false;
  users: any[] = []; selected: any = null; search = ''; filter = 'All'; status = 'ACTIVE'; days = 30; reason = ''; busy = false; error = '';
  publicNote='';privateNote='';verificationFilter='All';
  statuses = ['ACTIVE','FREEZE','SUSPENDED','BLOCKED']; private destroyed = new Subject<void>();
  constructor(private http: HttpClient,private dialog: MatDialog,public auth:UserService) {}
  ngOnInit(): void {this.load();}
  ngOnDestroy(): void {this.destroyed.next();this.destroyed.complete();this.dialog.closeAll();}
  load(): void {this.http.get<any[]>('/api/security/users').pipe(takeUntil(this.destroyed)).subscribe({next: rows => this.users=rows,error: e => this.error=e?.error?.message || 'Users could not be loaded.'});}
  get filtered(): any[] {const s=this.search.trim().toLowerCase();return this.users.filter(u => (this.filter==='All' || u.status===this.filter) && (this.verificationFilter==='All'||u.onboardingStatus===this.verificationFilter) && (u.username+' '+u.name+' '+u.iban).toLowerCase().includes(s));}
  statusLabel(status:string):string{return status==='FREEZE'?'Frozen':status.charAt(0)+status.slice(1).toLowerCase();}
  count(status: string): number {return this.users.filter(u => u.status===status).length;}
  verificationLabel(status: string): string {return ({NOT_STARTED:'Not started',PROFILE_COMPLETE:'Details saved',UNDER_REVIEW:'Awaiting review',VERIFIED:'Verified',DECLINED:'Declined'} as Record<string,string>)[status]??'Not started';}
  inspectVerification(user: any): void {
    if(user.username===this.auth.getCurrentUserName())return;
    this.verificationUser=user;this.verificationRecord=null;this.verificationNote='';this.error='';this.verificationLoading=true;
    this.dialog.open(this.verificationDialog,{width:'640px',maxWidth:'94vw'});
    this.http.get('/api/finance/onboarding/users/'+user.id).pipe(takeUntil(this.destroyed)).subscribe({next:record=>{this.verificationRecord=record;this.verificationLoading=false;},error:e=>{this.verificationLoading=false;this.error=e?.error?.message||'Onboarding could not be loaded.';}});
  }
  get canReviewVerification(): boolean {return this.verificationUser?.username!==this.auth.getCurrentUserName() && !!this.verificationRecord?.profile && this.verificationRecord.documents.some((d:any)=>d.kind==='IDENTITY') && this.verificationNote.trim().length>=5 && this.verificationNote.trim().length<=250;}
  reviewVerification(decision: string): void {
    if(this.busy||!this.canReviewVerification)return;this.busy=true;this.error='';
    this.http.post('/api/finance/onboarding/users/'+this.verificationUser.id+'/review',{decision,note:this.verificationNote,version:this.verificationRecord.profile.version}).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();},error:e=>{this.busy=false;this.error=e?.error?.message||'Verification could not be updated.';}});
  }
  edit(user: any): void {this.selected=user;this.status=user.status;this.days=30;this.reason='';this.publicNote='';this.privateNote='';this.error='';this.dialog.open(this.statusDialog,{width:'520px',maxWidth:'94vw'});}
  save(): void {
    if(this.busy)return;this.busy=true;this.error='';
    this.http.post('/api/security/users/'+this.selected.id+'/status',{status:this.status,days:this.days,reason:this.reason,publicNote:this.publicNote,privateNote:this.privateNote}).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();},error:e=>{this.busy=false;this.error=e?.error?.message || 'Status could not be updated.';}});
  }
}
