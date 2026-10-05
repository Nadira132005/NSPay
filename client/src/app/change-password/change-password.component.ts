import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../utils/user.service';

@Component({
    selector: 'app-change-password',
    templateUrl: './change-password.component.html',
    styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent {
    password = '';
    confirmPassword = '';
    statusMessage = '';
    saving = false;

    constructor(private userService: UserService, private router: Router) {
        if (!this.userService.isLoggedIn() || !this.userService.isPasswordChangeRequired()) {
            this.router.navigateByUrl('/dashboard/overview');
        }
    }

    submit(): void {
        this.statusMessage = '';
        if (this.password.length < 8) {
            this.statusMessage = 'Choose a password with at least 8 characters.';
            return;
        }
        if (this.password !== this.confirmPassword) {
            this.statusMessage = 'The passwords do not match.';
            return;
        }
        this.saving = true;
        this.userService.changePassword(this.password).subscribe({
            next: response => {
                this.userService.storeLogin(response);
                this.router.navigateByUrl('/dashboard/overview');
            },
            error: () => {
                this.saving = false;
                this.statusMessage = 'Could not update your password. Please try again.';
            }
        });
    }
}
