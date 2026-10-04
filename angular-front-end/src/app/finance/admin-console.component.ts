import { Component, OnInit, OnDestroy, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Subject, takeUntil } from 'rxjs';
@Component({selector: 'app-admin-console', templateUrl: './admin-console.component.html', styleUrls: ['./workspace.component.scss']})
export class AdminConsoleComponent implements OnInit, OnDestroy {
  @ViewChild('statusDialog') statusDialog!: TemplateRef<unknown>;
  users: any[] = []; selected: any = null; search = ''; filter = 'All'; status = 'ACTIVE'; days = 30; reason = ''; busy = false; error = '';
  statuses = ['ACTIVE','FREEZE','SUSPENDED','BLOCKED']; private destroyed = new Subject<void>();
  constructor(private http: HttpClient,private dialog: MatDialog) {}
  ngOnInit(): void {this.load();}
  ngOnDestroy(): void {this.destroyed.next();this.destroyed.complete();this.dialog.closeAll();}
  load(): void {this.http.get<any[]>('/api/security/users').pipe(takeUntil(this.destroyed)).subscribe({next: rows => this.users=rows,error: e => this.error=e?.error?.message || 'Users could not be loaded.'});}
  get filtered(): any[] {const s=this.search.trim().toLowerCase();return this.users.filter(u => (this.filter==='All' || u.status===this.filter) && (u.username+' '+u.name+' '+u.iban).toLowerCase().includes(s));}
  count(status: string): number {return this.users.filter(u => u.status===status).length;}
  edit(user: any): void {this.selected=user;this.status=user.status;this.days=30;this.reason='';this.error='';this.dialog.open(this.statusDialog,{width:'520px',maxWidth:'94vw'});}
  save(): void {
    if(this.busy)return;this.busy=true;this.error='';
    this.http.post('/api/security/users/'+this.selected.id+'/status',{status:this.status,days:this.days,reason:this.reason}).pipe(takeUntil(this.destroyed)).subscribe({next:()=>{this.busy=false;this.dialog.closeAll();this.load();},error:e=>{this.busy=false;this.error=e?.error?.message || 'Status could not be updated.';}});
  }
}
