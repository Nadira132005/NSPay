import { Component } from '@angular/core';
import { UserService } from '../utils/user.service';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
    logoutConfirmOpen = false;
    constructor(
        private userService: UserService
    ) {
    }

    isAdmin(): boolean {
        return this.userService.getCurrentRole() === 'Admin';
    }

    openLogoutConfirm() {
        this.logoutConfirmOpen = true;
    }

    closeLogoutConfirm() {
        this.logoutConfirmOpen = false;
    }

    confirmLogout() {
        this.logoutConfirmOpen = false;
        this.userService.logout();
    }
}
