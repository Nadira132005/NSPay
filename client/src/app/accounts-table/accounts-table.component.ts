import {Component,OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
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
    displayedAmount = 0;
    currentUserName = '';
    statusMessage = '';

    constructor(
        private frontEndService: FrontEndService,
        private userService: UserService
    ) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Account page');
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
                            return;
                        }

                        this.displayedAmount = this.currentAccount.amount;
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
