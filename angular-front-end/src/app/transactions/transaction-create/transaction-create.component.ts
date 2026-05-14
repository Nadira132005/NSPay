import { Component, OnInit } from '@angular/core';
import { AccountModel } from '../../models/account-model';
import { FrontEndService } from '../../utils/front-end.service';
import { UserService } from '../../utils/user.service';
import {
    TransactionModel,
    TRANSACTION_STATUS_LABELS,
    TRANSACTION_SUCCESS_CODE
} from '../../models/transaction-model';

@Component({
    selector: 'app-transaction-create',
    templateUrl: './transaction-create.component.html',
    styleUrls: ['./transaction-create.component.scss']
})
export class TransactionCreateComponent implements OnInit {
    currentAccount: AccountModel | null = null;
    transactionsData: TransactionModel[] = [];
    transactionsLoaded = false;
    transaction = {
        amount: null as number | null
    };
    statusMessage = '';
    currentAccountCardNumber: string | null = null;

    constructor(
        private frontEndService: FrontEndService,
        private userService: UserService
    ) {
    }

    ngOnInit(): void {
        this.loadCurrentAccount();
    }

    getTransactionStatusLabel(returnCode: string | number): string {
        const code = String(returnCode);
        return TRANSACTION_STATUS_LABELS[code] ?? code;
    }

    getTransactionStatusClass(returnCode: string | number): string {
        const code = String(returnCode);
        return code === TRANSACTION_SUCCESS_CODE ? 'status-success' : 'status-error';
    }

    isAccountActive(): boolean {
        return (this.currentAccount?.status || 'ACTIVE') === 'ACTIVE';
    }

    createTransaction() {
        if (!this.currentAccountCardNumber) {
            this.statusMessage = 'No account assigned to your user.';
            return;
        }

        if (!this.isAccountActive()) {
            this.statusMessage = 'Suspended accounts cannot create transactions.';
            return;
        }

        if (this.transaction.amount == null || this.transaction.amount <= 0) {
            this.statusMessage = 'Amount must be greater than zero.';
            return;
        }

        this.frontEndService.createTransaction({
            cardNumber: this.currentAccountCardNumber,
            amount: this.transaction.amount,
            timestamp: new Date().toISOString()
        }).subscribe({
            next: (response) => {
                const label = response?.message ?? response?.code ?? 'OK';
                const prefix = response?.businessLogicViolation ? 'Unsuccessful transaction' : 'Successful transaction';
                this.statusMessage = `${prefix}: ${label}`;
                this.transaction.amount = null;
                this.loadCurrentAccount();
            },
            error: () => {
                this.statusMessage = 'Could not create transaction.';
            }
        });
    }

    private loadCurrentAccount() {
        this.userService.getCurrentUser().subscribe({
            next: (user) => {
                if (user.accountId == null) {
                    this.currentAccount = null;
                    this.statusMessage = 'No account assigned to your user.';
                    this.transactionsData = [];
                    this.transactionsLoaded = true;
                    return;
                }

                this.frontEndService.getAccounts().subscribe({
                    next: (accounts) => {
                        this.currentAccount = accounts.find(account => account.id === user.accountId) ?? null;
                        if (!this.currentAccount) {
                            this.statusMessage = 'No account assigned to your user.';
                            this.transactionsData = [];
                            this.transactionsLoaded = true;
                            return;
                        }

                        this.currentAccount.status = this.currentAccount.status || 'ACTIVE';
                        this.currentAccountCardNumber = this.currentAccount.cardNumber;
                        this.loadTransactions();
                    },
                    error: () => {
                        this.statusMessage = 'Could not load account details.';
                        this.transactionsData = [];
                        this.transactionsLoaded = true;
                    }
                });
            },
            error: () => {
                this.statusMessage = 'Could not load user details.';
                this.transactionsData = [];
                this.transactionsLoaded = true;
            }
        });
    }

    private loadTransactions() {
        this.transactionsLoaded = false;

        if (!this.currentAccountCardNumber) {
            this.transactionsData = [];
            this.statusMessage = 'No account assigned to your user.';
            this.transactionsLoaded = true;
            return;
        }

        this.frontEndService.getTransactionsOfCardNumber(this.currentAccountCardNumber).subscribe({
            next: (data) => {
                this.transactionsData = data;
                this.transactionsLoaded = true;
            },
            error: () => {
                this.statusMessage = 'Could not load transactions.';
                this.transactionsLoaded = true;
            }
        });
    }
}
