import {Component,OnInit } from '@angular/core';
import {AccountModel} from '../models/account-model';
import {FrontEndService} from '../utils/front-end.service';
import {UserService} from '../utils/user.service';

@Component({
    selector: 'app-accounts-table',
    templateUrl: './accounts-table.component.html',
    styleUrls: ['./accounts-table.component.scss']
})
export class AccountsTableComponent implements OnInit {
    accountsData: AccountModel[] = [];
    currentAccount: AccountModel | null = null;
    currentUserName = '';
    statusMessage = '';

    constructor(
        private frontEndService: FrontEndService,
        private userService: UserService
    ) {
    }

    ngOnInit(): void {
        this.loadCurrentAccount();
    }

    private loadCurrentAccount() {
        this.userService.getCurrentUser().subscribe({
            next: (user) => {
                this.currentUserName = user.name ?? '';

                if (user.accountId == null) {
                    this.currentAccount = null;
                    this.statusMessage = 'No account is assigned to this user.';
                    return;
                }

                this.frontEndService.getAccounts().subscribe({
                    next: (data) => {
                        this.accountsData = data;
                        this.currentAccount = this.accountsData.find(account => account.id === user.accountId) ?? null;

                        if (!this.currentAccount) {
                            this.statusMessage = 'No account is assigned to this user.';
                        }
                    },
                    error: () => {
                        this.statusMessage = 'Could not load account details.';
                    }
                });
            },
            error: () => {
                this.statusMessage = 'Could not load user details.';
            }
        });
    }
}
