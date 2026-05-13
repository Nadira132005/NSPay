import { Component, OnInit } from '@angular/core';
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
    deleteConfirmOpen = false;
    pendingDeleteAccountId: number | null = null;

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
        'cardHolderName',
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
        this.loadAccounts();
    }

    createAccount() {
        if (this.account.amount == null || this.account.dailyTxLimit == null || this.account.dailyTxSumLimit == null) {
            this.statusMessage = 'Fill in all account fields before creating the account.';
            return;
        }

        this.frontEndService.createAccount(this.account).subscribe({
            next: () => {
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
        this.deleteConfirmOpen = true;
    }

    cancelDeleteAccount() {
        this.deleteConfirmOpen = false;
        this.pendingDeleteAccountId = null;
    }

    confirmDeleteAccount() {
        if (this.pendingDeleteAccountId == null) {
            return;
        }

        const id = this.pendingDeleteAccountId;
        this.deleteConfirmOpen = false;
        this.pendingDeleteAccountId = null;

        this.frontEndService.deleteAccount(id).subscribe({
            next: () => {
                this.statusMessage = 'Account deleted.';
                this.loadAccounts();
            },
            error: (error) => {
                console.error('Delete account failed:', error);
                this.statusMessage = 'Could not delete account.';
            }
        });
    }

    private loadAccounts() {
        this.frontEndService.getAccounts().subscribe({
            next: (data) => {
                this.accountsData = data;
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
}
