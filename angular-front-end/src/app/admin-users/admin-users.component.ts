import { Component } from '@angular/core';
import { UserModel, UserRole } from '../models/user.model';
import { UserService } from '../utils/user.service';

@Component({
    selector: 'app-admin-users',
    templateUrl: './admin-users.component.html',
    styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent {
    user: UserModel = {
        name: '',
        password: ''
    };

    roles: UserRole[] = ['Admin', 'User', 'Analyst'];
    statusMessage = '';

    constructor(private userService: UserService) {
    }

    createUser() {
        this.userService.createUser(this.user).subscribe({
            next: () => {
                this.statusMessage = `User "${this.user.name}" was created.`;
                this.user = {
                    name: '',
                    password: ''
                };// clears the form after user creation
            },
            error: (error) => {
                console.error('Create user failed:', error);
                if (error?.status === 403) {
                    this.statusMessage = 'Only Admin can create users.';
                    return;
                }
                if (error?.status === 409) {
                    this.statusMessage = 'User name already exists.';
                    return;
                }
                this.statusMessage = 'Could not create user.';
            }
        });
    }
}
