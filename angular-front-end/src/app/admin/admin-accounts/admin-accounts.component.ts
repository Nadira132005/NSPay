import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
import { FrontEndService } from '../../utils/front-end.service';
import { AccountModel } from '../../models/account-model';

@Component({
    selector: 'app-admin-accounts',
    templateUrl: './admin-accounts.component.html',
    styleUrls: ['./admin-accounts.component.scss']
})
export class AdminAccountsComponent implements OnInit {
    accountsData: AccountModel[] = [];
    statusMessage = '';
    actionConfirmOpen = false;
    pendingDeleteAccountId: number | null = null;
    statuses: string[] = ['ACTIVE', 'SUSPENDED', 'BLOCKED', 'CLOSED', 'FROZEN'];

    account = {
        cardNumber: '',
        cardHolderName: '',
        amount: null as number | null,
        dailyTxLimit: null as number | null,
        dailyTxSumLimit: null as number | null,
        dailyTx: 0,
        dailyTxSum: 0
    };

    displayedColumns: string[] = [
        'id',
        'cardNumber',
        'iban',
        'cardHolderName',
        'status',
        'amount',
        'dailyTxLimit',
        'dailyTxSumLimit',
        'dailyTx',
        'dailyTxSum',
        'actions'
    ];

    constructor(private frontEndService: FrontEndService) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Admin accounts');
        this.loadAccounts();
    }

    createAccount() {
        if (this.account.amount == null || this.account.dailyTxLimit == null || this.account.dailyTxSumLimit == null) {
            this.statusMessage = 'Fill in all account fields before creating the account.';
            return;
        }

        this.frontEndService.createAccount(this.account).subscribe({
            next: () => {
                this.trackAudit('ACTION: Create account');
                this.statusMessage = `Account ${this.account.cardNumber} was created.`;
                this.resetForm();
                this.loadAccounts();
            },
            error: (error) => {
                console.error('Create account failed:', error);
                this.statusMessage = 'Could not create account.';
            }
        });
    }

    deleteAccount(id: number) {
        this.pendingDeleteAccountId = id;
        this.actionConfirmOpen = true;
    }

    cancelActionAccount() {
        this.actionConfirmOpen = false;
        this.pendingDeleteAccountId = null;
    }

    confirmActionAccount() {
        const id = this.pendingDeleteAccountId;

        if (id == null) {
            return;
        }

        this.actionConfirmOpen = false;
        this.pendingDeleteAccountId = null;

        this.frontEndService.deleteAccount(id).subscribe({
            next: () => {
                this.trackAudit('ACTION: Delete account');
                this.statusMessage = 'Account deleted.';
                this.loadAccounts();
            },
            error: (error) => {
                console.error('Account action failed:', error);
                this.statusMessage = 'Could not delete account.';
            }
        });
    }

    updateAccountStatus(row: AccountModel, status: string) {
        if (row.id == null) {
            return;
        }

        row.status = status;
        const payload = { ...row, status };

        this.frontEndService.updateAccount(row.id, payload).subscribe({
            next: () => {
                this.trackAudit('ACTION: Update account status');
                this.statusMessage = 'Account status updated.';
                this.loadAccounts();
            },
            error: (error) => {
                console.error('Update account status failed:', error);
                this.statusMessage = 'Could not update account status.';
                this.loadAccounts();
            }
        });
    }

    private loadAccounts() {
        this.frontEndService.getAccounts().subscribe({
            next: (data) => {
                this.accountsData = data.map(account => ({
                    ...account,
                    status: account.status || 'ACTIVE'
                }));
            },
            error: (error) => {
                console.error('Load accounts failed:', error);
                this.statusMessage = 'Could not load accounts.';
            }
        });
    }

    private resetForm() {
        this.account = {
            cardNumber: '',
            cardHolderName: '',
            amount: null,
            dailyTxLimit: null,
            dailyTxSumLimit: null,
            dailyTx: 0,
            dailyTxSum: 0
        };
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
