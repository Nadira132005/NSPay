import { Component } from '@angular/core';
import { UserService } from '../utils/user.service';
@Component({selector:'app-dashboard', templateUrl:'./dashboard.component.html',styleUrls:['./dashboard.component.scss']})
export class DashboardComponent {
  menuOpen = false;
  navigation = [
    { path: 'overview', label: 'Overview', icon: '◫' },
    { path: 'payments', label: 'Payments', icon: '↗' },
    { path: 'savings', label: 'Savings', icon: '◎' },
    { path: 'trading', label: 'Invest', icon: '⌁' },
    { path: 'credit', label: 'Credit', icon: '✳' },
    { path: 'security', label: 'Security', icon: '◈' }
  ];
  constructor(public userService: UserService) {}
  isAdmin(): boolean { return this.userService.getCurrentRole() === 'Admin'; }
  isAnalyst(): boolean { return this.userService.getCurrentRole() === 'Analyst'; }
}
