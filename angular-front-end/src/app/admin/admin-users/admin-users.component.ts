import { Component, OnInit } from '@angular/core';
import { AccountModel } from '../../models/account-model';
import { UserModel, UserRole } from '../../models/user.model';
import { UserService } from '../../utils/user.service';
import { FrontEndService } from '../../utils/front-end.service';

@Component({
    selector: 'app-admin-users',
    templateUrl: './admin-users.component.html',
    styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
    user: UserModel = {
        name: '',
        password: '',
        accountId: null
    };

    accountsData: AccountModel[] = [];
    usersData: UserModel[] = [];
    roles: UserRole[] = ['Admin', 'User', 'Analyst'];
    statusMessage = '';
    deleteConfirmOpen = false;
    pendingDeleteUserId: number | null = null;

    constructor(
        private userService: UserService,
        private frontEndService: FrontEndService
    ) {
    }

    ngOnInit(): void {
        this.loadAccounts();
        this.loadUsers();
    }

    createUser() {
        this.userService.createUser(this.user).subscribe({
            next: () => {
                this.statusMessage = `User "${this.user.name}" was created.`;
                this.user = {
                    name: '',
                    password: '',
                    accountId: this.accountsData[0]?.id ?? null
                };
                this.loadUsers();
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

    getAccountLabel(accountId: number | null | undefined): string {
        if (accountId == null) {
            return 'No account';
        }
        const account = this.accountsData.find(item => item.id === accountId);
        return account ? `${account.cardHolderName} - ${account.cardNumber}` : `Account #${accountId}`;
    }

    deleteUser(id: number) {
        this.pendingDeleteUserId = id;
        this.deleteConfirmOpen = true;
    }

    cancelDeleteUser() {
        this.deleteConfirmOpen = false;
        this.pendingDeleteUserId = null;
    }

    confirmDeleteUser() {
        if (this.pendingDeleteUserId == null) {
            return;
        }

        const id = this.pendingDeleteUserId;
        this.deleteConfirmOpen = false;
        this.pendingDeleteUserId = null;

        this.userService.deleteUser(id).subscribe({
            next: () => {
                this.statusMessage = 'User deleted.';
                this.loadUsers();
            },
            error: (error) => {
                console.error('Delete user failed:', error);
                this.statusMessage = 'Could not delete user.';
            }
        });
    }

    updateUserAccount(user: UserModel) {
        if (user.id == null) {
            return;
        }

        this.userService.updateUser(user.id, user).subscribe({
            next: () => {
                this.statusMessage = 'User account updated.';
                this.loadUsers();
            },
            error: (error) => {
                console.error('Update user failed:', error);
                this.statusMessage = 'Could not update user account.';
            }
        });
    }

    private loadUsers() {
        this.userService.getUsers().subscribe({
            next: (users) => {
                this.usersData = users;
            },
            error: (error) => {
                console.error('Load users failed:', error);
                this.statusMessage = 'Could not load users.';
            }
        });
    }

    private loadAccounts() {
        this.frontEndService.getAccounts().subscribe({
            next: (accounts) => {
                this.accountsData = accounts;
                if (this.user.accountId == null) {
                    this.user.accountId = accounts[0]?.id ?? null;
                }
            }
        });
    }
}
