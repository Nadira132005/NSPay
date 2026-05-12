import {Component} from '@angular/core';
import {AccountModel} from "../models/account-model";
import {FrontEndService} from "../utils/front-end.service";
import {
    TransactionModel,
    TRANSACTION_STATUS_LABELS,
    TRANSACTION_SUCCESS_CODE
} from "../models/transaction-model";
import {SelectionModel} from "@angular/cdk/collections";
import {forkJoin} from "rxjs";
import {UserService} from "../utils/user.service";

@Component({
    selector: 'app-accounts-table',
    templateUrl: './accounts-table.component.html',
    styleUrls: ['./accounts-table.component.scss']
})
export class AccountsTableComponent {

    constructor(private frontEndService: FrontEndService,
                private userService: UserService) {
    }

    displayedColumnsAccounts: string[] =
        ['id', 'cardNumber', 'cardHolderName', 'amount', 'dailyTxLimit', 'dailyTxSumLimit', 'dailyTx', 'dailyTxSum'];

    displayedColumnsTransactions: string [] = ['id', 'amount', 'cardNumber', 'returnCode', 'timestamp'];

    isAccountsSelected = false;
    isTransactionsSelected = false;
    isTransactionsShowSelected = false;

    accountsData: AccountModel[] = [];
    transactionsData: TransactionModel[] = [];

    getTransactionStatusLabel(returnCode: string | number): string {
        const code = String(returnCode);
        return TRANSACTION_STATUS_LABELS[code] ?? code;
    }

    getTransactionStatusClass(returnCode: string | number): string {
        const code = String(returnCode);
        return code === TRANSACTION_SUCCESS_CODE ? 'status-success' : 'status-error';
    }

    showTransactionData() {
        this.frontEndService.getTransactions().subscribe(data => {
            this.transactionsData = data;
        })
        this.isTransactionsSelected = true;
        this.isAccountsSelected = false;
    }

    showAccountsData() {
        this.frontEndService.getAccounts().subscribe(data => {
            this.accountsData = data;
        })

        this.isTransactionsShowSelected = false;
        this.isTransactionsSelected = false;
        this.isAccountsSelected = true;
    }

    logOut() {
        this.userService.logout();
    }

    selection = new SelectionModel<any>(true, []);

    masterToggle() {
        this.isAllSelected() ?
            this.selection.clear() :
            this.accountsData.forEach(row => this.selection.select(row));
    }

    isAllSelected() {
        const numSelected = this.selection.selected.length;
        const numRows = this.accountsData.length;
        return numSelected === numRows;
    }


    showTransactionDataByAccount(selectedAccounts) {
        console.log(selectedAccounts)

        const fetchTransactionsObservables = selectedAccounts.map(account => {
            return this.frontEndService.getTransactionsOfAccount(account);
        });

        forkJoin(fetchTransactionsObservables).subscribe(allTransactions => {
            this.transactionsData = Array.prototype.concat.apply([], allTransactions);
            console.log("Transactions for selected accounts:", this.transactionsData);
        });

        this.selection = new SelectionModel<any>(true, []);

        this.isTransactionsShowSelected = true;
        this.isTransactionsSelected = true;
        this.isAccountsSelected = false;
    }

    createTransaction(selectedAccounts) {

    }

    deleteSelectedAccount(selectedAccounts) {

    }
}
