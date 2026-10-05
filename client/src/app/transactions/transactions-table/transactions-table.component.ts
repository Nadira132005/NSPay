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
    selector: 'app-transactions-table',
    templateUrl: './transactions-table.component.html',
    styleUrls: ['./transactions-table.component.scss']
})
export class TransactionsTableComponent implements OnInit {
    accountsData: AccountModel[] = [];
    transactionsData: TransactionModel[] = [];
    statusMessage = '';
    currentRole = this.userService.getCurrentRole();
    currentAccountCardNumber = this.userService.getCurrentAccountCardNumber();

    constructor(
        private frontEndService: FrontEndService,
        private userService: UserService
    ) {
    }

    ngOnInit(): void {
        this.loadAccounts();
        this.loadTransactions();
    }

    isAdmin(): boolean {
        return this.currentRole === 'Admin';
    }

    getTransactionStatusLabel(returnCode: string | number): string {
        const code = String(returnCode);
        return TRANSACTION_STATUS_LABELS[code] ?? code;
    }

    getTransactionStatusClass(returnCode: string | number): string {
        const code = String(returnCode);
        return code === TRANSACTION_SUCCESS_CODE ? 'status-success' : 'status-error';
    }

    private loadTransactions() {
        this.frontEndService.getTransactions().subscribe({
            next: (data) => {
                this.transactionsData = data;
            },
            error: () => {
                this.statusMessage = 'Could not load transactions.';
            }
        });
    }

    private loadAccounts() {
        this.frontEndService.getAccounts().subscribe({
            next: (data) => {
                this.accountsData = data;
            },
            error: () => {
                this.statusMessage = 'Could not load accounts.';
            }
        });
    }
}
