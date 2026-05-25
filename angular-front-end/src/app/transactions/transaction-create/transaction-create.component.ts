import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { take } from 'rxjs/operators';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { AccountModel } from '../../models/account-model';
import { PendingTransactionModel } from '../../models/pending-transaction.model';
import { FrontEndService } from '../../utils/front-end.service';
import { PendingTransactionsService } from '../../utils/pending-transactions.service';
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
export class TransactionCreateComponent implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild(MatPaginator) paginator!: MatPaginator;

    currentAccount: AccountModel | null = null;
    dataSource = new MatTableDataSource<TransactionModel>([]);
    pageSize = 10;
    displayedColumns: string[] = ['number', 'amount', 'cardNumber', 'state', 'timestamp'];
    transactionsLoaded = false;
    transaction = {
        amount: null as number | null,
        moment: 'now' as 'now' | 'scheduled',
        scheduledAt: this.toLocalDateTimeValue(new Date())
    };
    statusMessage = '';
    currentAccountCardNumber: string | null = null;
    minScheduledAt = this.toLocalDateTimeValue(new Date());
    private readonly pendingPollIntervalMs = 15000;
    private pendingPollHandle: number | null = null;

    constructor(
        private frontEndService: FrontEndService,
        private userService: UserService,
        private pendingTransactionsService: PendingTransactionsService
    ) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Transaction page');
        this.loadCurrentAccount();
        this.startPendingWatcher();
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
    }

    ngOnDestroy(): void {
        if (this.pendingPollHandle != null) {
            window.clearInterval(this.pendingPollHandle);
        }
    }

    onPageSizeChange() {
        const paginator = this.dataSource.paginator;
        if (!paginator) {
            return;
        }

        paginator.pageIndex = 0;
        paginator.pageSize = this.pageSize;
        paginator.page.next({
            pageIndex: 0,
            pageSize: this.pageSize,
            length: this.dataSource.data.length
        });
    }

    getTransactionStatusLabel(returnCode: string | number): string {
        const code = String(returnCode);
        return TRANSACTION_STATUS_LABELS[code] ?? code;
    }

    getTransactionStatusClass(returnCode: string | number): string {
        const code = String(returnCode);
        return code === TRANSACTION_SUCCESS_CODE ? 'status-success' : 'status-error';
    }

    getTransactionStateLabel(row: TransactionModel): string {
        if (row.status === 'pending') {
            return 'Pending';
        }

        if (row.status === 'processing') {
            return 'Processing';
        }

        return this.getTransactionStatusLabel(row.returnCode);
    }

    getTransactionStateClass(row: TransactionModel): string {
        if (row.status === 'pending' || row.status === 'processing') {
            return 'status-pending';
        }

        return this.getTransactionStatusClass(row.returnCode);
    }

    formatTimestamp(row: TransactionModel): string {
        return row.status === 'pending' || row.status === 'processing'
            ? (row.scheduledAt ?? row.timestamp)
            : row.timestamp;
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

        const timestamp = this.resolveTransactionTimestamp();
        if (!timestamp) {
            return;
        }

        if (this.transaction.moment === 'scheduled') {
            this.pendingTransactionsService.add({
                cardNumber: this.currentAccountCardNumber,
                amount: this.transaction.amount,
                scheduledAt: timestamp
            });

            this.statusMessage = 'Scheduled transaction added as pending.';
            this.transaction.amount = null;
            this.transaction.moment = 'now';
            this.transaction.scheduledAt = this.toLocalDateTimeValue(new Date());
            this.trackAudit('ACTION: Schedule transaction');
            this.loadTransactions();
            return;
        }

        this.submitToBackend({
            cardNumber: this.currentAccountCardNumber,
            amount: this.transaction.amount,
            timestamp
        });
    }

    private loadCurrentAccount() {
        this.userService.getCurrentUser().subscribe({
            next: (user) => {
                if (user.accountId == null) {
                    this.currentAccount = null;
                    this.statusMessage = 'No account assigned to your user.';
                    this.dataSource.data = [];
                    this.transactionsLoaded = true;
                    return;
                }

                this.frontEndService.getAccounts().subscribe({
                    next: (accounts) => {
                        this.currentAccount = accounts.find(account => account.id === user.accountId) ?? null;
                        if (!this.currentAccount) {
                            this.statusMessage = 'No account assigned to your user.';
                            this.dataSource.data = [];
                            this.transactionsLoaded = true;
                            return;
                        }

                        this.currentAccount.status = this.currentAccount.status || 'ACTIVE';
                        this.currentAccountCardNumber = this.currentAccount.cardNumber;
                        this.loadTransactions();
                    },
                    error: () => {
                        this.statusMessage = 'Could not load account details.';
                        this.dataSource.data = [];
                        this.transactionsLoaded = true;
                    }
                });
            },
            error: () => {
                this.statusMessage = 'Could not load user details.';
                this.dataSource.data = [];
                this.transactionsLoaded = true;
            }
        });
    }

    private loadTransactions() {
        this.transactionsLoaded = false;

        if (!this.currentAccountCardNumber) {
            this.dataSource.data = [];
            this.statusMessage = 'No account assigned to your user.';
            this.transactionsLoaded = true;
            return;
        }

        this.frontEndService.getTransactionsOfCardNumber(this.currentAccountCardNumber).subscribe({
            next: (data) => {
                this.dataSource.data = this.mergeTransactions(data, this.getPendingTransactions());
                this.transactionsLoaded = true;
                this.processDuePendingTransactions();
            },
            error: () => {
                this.statusMessage = 'Could not load transactions.';
                this.transactionsLoaded = true;
            }
        });
    }

    private submitToBackend(payload: { cardNumber: string; amount: number; timestamp: string }): void {
        this.frontEndService.createTransaction(payload).subscribe({
            next: (response) => {
                const label = response?.message ?? response?.code ?? 'OK';
                const prefix = response?.businessLogicViolation ? 'Unsuccessful transaction' : 'Successful transaction';
                this.statusMessage = `${prefix}: ${label}`;
                this.transaction.amount = null;
                this.transaction.moment = 'now';
                this.transaction.scheduledAt = this.toLocalDateTimeValue(new Date());
                this.trackAudit('ACTION: Create transaction');
                this.loadCurrentAccount();
            },
            error: () => {
                this.statusMessage = 'Could not create transaction.';
            }
        });
    }

    private getPendingTransactions(): PendingTransactionModel[] {
        if (!this.currentAccountCardNumber) {
            return [];
        }

        return this.pendingTransactionsService.listForCardNumber(this.currentAccountCardNumber);
    }

    private mergeTransactions(
        completedTransactions: TransactionModel[],
        pendingTransactions: PendingTransactionModel[]
    ): TransactionModel[] {
        const pendingRows: TransactionModel[] = pendingTransactions.map(transaction => ({
            id: -1,
            amount: transaction.amount,
            cardNumber: transaction.cardNumber,
            returnCode: Number.NaN,
            timestamp: transaction.createdAt,
            status: transaction.status,
            scheduledAt: transaction.scheduledAt,
            localId: transaction.localId
        }));

        const merged = [
            ...pendingRows,
            ...completedTransactions.map(transaction => ({
                ...transaction,
                status: 'completed' as const
            }))
        ];

        return merged.sort((a, b) => {
            if (a.status !== 'completed' && b.status === 'completed') {
                return -1;
            }
            if (a.status === 'completed' && b.status !== 'completed') {
                return 1;
            }

            return new Date(b.scheduledAt ?? b.timestamp).getTime() - new Date(a.scheduledAt ?? a.timestamp).getTime();
        });
    }

    private startPendingWatcher(): void {
        this.processDuePendingTransactions();
        this.pendingPollHandle = window.setInterval(() => this.processDuePendingTransactions(), this.pendingPollIntervalMs);
    }

    private processDuePendingTransactions(): void {
        if (!this.currentAccountCardNumber || !this.isAccountActive()) {
            return;
        }

        const pendingTransactions = this.pendingTransactionsService.listForCardNumber(this.currentAccountCardNumber);
        const dueTransactions = pendingTransactions.filter(transaction => new Date(transaction.scheduledAt).getTime() <= Date.now());

        dueTransactions.forEach(transaction => {
            if (transaction.status === 'processing') {
                return;
            }

            this.pendingTransactionsService.markProcessing(transaction.localId);
            this.frontEndService.createTransaction({
                cardNumber: transaction.cardNumber,
                amount: transaction.amount,
                timestamp: transaction.scheduledAt
            }).subscribe({
                next: () => {
                    this.pendingTransactionsService.remove(transaction.localId);
                    this.trackAudit(`ACTION: Process pending transaction ${transaction.localId}`);
                    this.loadCurrentAccount();
                },
                error: () => {
                    this.statusMessage = 'A pending transaction could not be processed.';
                    this.pendingTransactionsService.remove(transaction.localId);
                    this.loadTransactions();
                }
            });
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

    private resolveTransactionTimestamp(): string | null {
        if (this.transaction.moment === 'now') {
            return new Date().toISOString();
        }

        if (!this.transaction.scheduledAt) {
            this.statusMessage = 'Select the day and hour for the transaction.';
            return null;
        }

        const scheduledDate = new Date(this.transaction.scheduledAt);
        if (Number.isNaN(scheduledDate.getTime())) {
            this.statusMessage = 'Selected date and time is invalid.';
            return null;
        }

        if (scheduledDate.getTime() < Date.now()) {
            this.statusMessage = 'Scheduled date and time must be in the future.';
            return null;
        }

        return scheduledDate.toISOString();
    }

    private toLocalDateTimeValue(date: Date): string {
        const pad = (value: number) => String(value).padStart(2, '0');
        const year = date.getFullYear();
        const month = pad(date.getMonth() + 1);
        const day = pad(date.getDate());
        const hours = pad(date.getHours());
        const minutes = pad(date.getMinutes());
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }
}
