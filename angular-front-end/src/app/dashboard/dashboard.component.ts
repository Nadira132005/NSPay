import { Component } from '@angular/core';
import { UserService } from '../utils/user.service';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
    constructor(
        private userService: UserService
    ) {
    }

    isAdmin(): boolean {
        return this.userService.getCurrentRole() === 'Admin';
    }

    logOut() {
        this.userService.logout();
    }
}
