import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
import { UserService } from '../utils/user.service';
import { FrontEndService } from '../utils/front-end.service';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
    logoutConfirmOpen = false;
    constructor(
        private userService: UserService,
        private frontEndService: FrontEndService
    ) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Dashboard');
    }

    isAdmin(): boolean {
        return this.userService.getCurrentRole() === 'Admin';
    }

    isAnalyst(): boolean {
        return this.userService.getCurrentRole() === 'Analyst';
    }

    openLogoutConfirm() {
        this.logoutConfirmOpen = true;
    }

    closeLogoutConfirm() {
        this.logoutConfirmOpen = false;
    }

    confirmLogout() {
        this.logoutConfirmOpen = false;
        this.trackAudit('ACTION: Logout confirmed');
        this.userService.logout();
    }

    private trackAudit(action: string) {
        this.frontEndService.recordAudit(action)
            .pipe(take(1))
            .subscribe({
            error: () => {
                console.warn('Audit tracking failed silently:', action);
            }
        });
    }
}
