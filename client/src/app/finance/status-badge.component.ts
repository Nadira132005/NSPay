import { Component, Input } from '@angular/core';

@Component({selector:'app-status-badge',template:`<span class="badge" [class]="'badge '+tone">
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="icon"/></svg>{{ label }}</span>`,styles:[`
:host{display:inline-block}.badge{display:inline-flex;align-items:center;gap:6px;padding:5px 9px;border-radius:20px;font-size:12px;font-weight:500;line-height:1.3;white-space:nowrap}.badge svg{width:15px;height:15px;flex-shrink:0}.neutral{background:#edf0ef;color:#52605b}.success{background:#e0f2e6;color:#18663c}.pending{background:#fff0cd;color:#845c08}.frozen{background:#e2effc;color:#225c92}.danger{background:#fde7e4;color:#9b3028}.details{background:#eee8fa;color:#64468b}.suspended{background:#ffeadb;color:#90450c}
`]})
export class StatusBadgeComponent {
  @Input() value: string | null | undefined='';
  get label():string{return ({ACTIVE:'Active',FREEZE:'Frozen',SUSPENDED:'Suspended',BLOCKED:'Blocked',NOT_STARTED:'Not started',PROFILE_COMPLETE:'Details saved',UNDER_REVIEW:'Awaiting review',VERIFIED:'Verified',DECLINED:'Declined',APPROVED:'Approved',DRAFT:'Draft',OPEN:'Awaiting review',CONFIRMED:'Action taken',CLEARED:'Dismissed',DISMISSED:'Dismissed'} as Record<string,string>)[this.value??'']??this.value??'Unknown';}
  get tone():string{const v=this.value??'';return ['ACTIVE','VERIFIED','APPROVED'].includes(v)?'success':v==='FREEZE'?'frozen':v==='SUSPENDED'?'suspended':v==='PROFILE_COMPLETE'?'details':['BLOCKED','DECLINED'].includes(v)?'danger':['UNDER_REVIEW','OPEN','CONFIRMED'].includes(v)?'pending':'neutral';}
  get icon():string{if(this.value==='FREEZE')return 'M12 2v20M3.3 7l17.4 10M3.3 17 20.7 7M9 4l3 3 3-3M9 20l3-3 3 3';if(this.value==='SUSPENDED')return 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0M9 8v8M15 8v8';if(this.value==='PROFILE_COMPLETE')return 'M6 3h9l3 3v15H6zM14 3v5h4M9 12h6M9 16h6';if(this.tone==='success')return 'M21 12a9 9 0 1 1-5-8M8 11l4 4 9-10';if(this.tone==='danger')return 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0M6 6l12 12';if(this.tone==='pending')return 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0M12 7v5l3 2';return 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0M12 8v.1M12 11v5';}
}
