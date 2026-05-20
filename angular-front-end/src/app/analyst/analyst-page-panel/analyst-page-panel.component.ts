import { AfterViewInit, ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { take } from 'rxjs/operators';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { AccountModel } from '../../models/account-model';
import { TransactionModel, TRANSACTION_STATUS_LABELS, TRANSACTION_SUCCESS_CODE } from '../../models/transaction-model';
import { FrontEndService } from '../../utils/front-end.service';

interface AnalystTransactionRow extends TransactionModel {
    cardHolderName: string;
    accountStatus: string;
}

@Component({
    selector: 'app-analyst-page-panel',
    templateUrl: './analyst-page-panel.component.html',
    styleUrls: ['./analyst-page-panel.component.scss']
})
export class AnalystPagePanelComponent implements OnInit, AfterViewInit {
    @ViewChild(MatPaginator) paginator!: MatPaginator;
    @ViewChild(MatSort) sort!: MatSort;

    dataSource = new MatTableDataSource<AnalystTransactionRow>([]);
    accountsData: AccountModel[] = [];
    pageSize = 10;
    displayedColumns: string[] = ['id', 'timestamp', 'cardHolderName', 'cardNumber', 'amount', 'returnCode'];
    loaded = false;
    statusMessage = '';
    accountsLoaded = false;
    transactionsLoaded = false;

    filters = {
        dateFrom: '',
        dateTo: '',
        amountFrom: 0,
        amountTo: 0,
        accountFilter: 'all',
        statusFilter: 'all',
        returnCodeFilter: 'all'
    };

    amountBounds = { min: 0, max: 0 };
    accountOptions: Array<{ value: string; label: string }> = [];
    statusOptions = ['all', 'ACTIVE', 'SUSPENDED', 'BLOCKED', 'CLOSED', 'FROZEN'];
    returnCodeOptions = ['all', ...Object.entries(TRANSACTION_STATUS_LABELS).map(([code, label]) => `${code}:${label}`)];
    loading = false;
    private loadingStartedAt = 0;

    private rawTransactions: TransactionModel[] = [];

    constructor(private frontEndService: FrontEndService, private cdr: ChangeDetectorRef) {
        this.dataSource.filterPredicate = (row, filter) => {
            const parsed = JSON.parse(filter) as {
                dateFrom: string;
                dateTo: string;
                amountFrom: number;
                amountTo: number;
                accountFilter: string;
                statusFilter: string;
                returnCodeFilter: string;
            };
            const amount = Number(row.amount);
            const rowDate = this.toDateOnly(row.timestamp);
            const account = this.accountsData.find(item => item.cardNumber === row.cardNumber);
            const cardHolderName = account?.cardHolderName || '';
            const accountStatus = account?.status || 'ACTIVE';

            const amountMatches = amount >= Number(parsed.amountFrom) && amount <= Number(parsed.amountTo);
            const accountMatches = parsed.accountFilter === 'all' || row.cardNumber === parsed.accountFilter;
            const statusMatches = parsed.statusFilter === 'all' || accountStatus === parsed.statusFilter;
            const codeMatches = parsed.returnCodeFilter === 'all' || String(row.returnCode) === parsed.returnCodeFilter.split(':')[0];
            const dateFromMatches = !parsed.dateFrom || rowDate >= parsed.dateFrom;
            const dateToMatches = !parsed.dateTo || rowDate <= parsed.dateTo;

            return amountMatches && accountMatches && statusMatches && codeMatches && dateFromMatches && dateToMatches;
        };
        this.dataSource.sortingDataAccessor = (row, column) => {
            switch (column) {
                case 'cardHolderName':
                    return row.cardHolderName || '';
                case 'cardNumber':
                    return row.cardNumber || '';
                case 'timestamp':
                    return row.timestamp || '';
                case 'returnCode':
                    return Number(row.returnCode);
                default:
                    return (row as any)[column] ?? '';
            }
        };
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Analyst transactions');
        this.filters.accountFilter = 'all';
        this.filters.statusFilter = 'all';
        this.filters.returnCodeFilter = 'all';
        this.loading = true;
        this.loadingStartedAt = Date.now();
        this.loadAccounts();
        this.loadTransactions();
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
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
            length: this.dataSource.filteredData.length
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

    applyFilters() {
        this.dataSource.filter = JSON.stringify(this.filters);
    }

    onAmountFromChange(value: string | number) {
        const nextValue = Number(value);
        this.filters.amountFrom = Math.min(nextValue, this.filters.amountTo);
        this.applyFilters();
    }

    onAmountToChange(value: string | number) {
        const nextValue = Number(value);
        this.filters.amountTo = Math.max(nextValue, this.filters.amountFrom);
        this.applyFilters();
    }

    resetAmountFilter() {
        this.filters.amountFrom = this.amountBounds.min;
        this.filters.amountTo = this.amountBounds.max;
        this.applyFilters();
    }

    private loadAccounts() {
        this.frontEndService.getAccounts().subscribe({
            next: (accounts) => {
                this.accountsData = accounts.map(account => ({
                    ...account,
                    status: account.status || 'ACTIVE'
                }));
                this.accountOptions = this.accountsData.map(account => ({
                    value: account.cardNumber,
                    label: `${account.cardHolderName} - ${account.cardNumber}`
                }));
                this.accountsLoaded = true;
                this.rebuildRows();
            },
            error: () => {
                this.statusMessage = 'Could not load accounts.';
                this.accountsLoaded = true;
                this.rebuildRows();
            }
        });
    }

    private loadTransactions() {
        this.frontEndService.getTransactions().subscribe({
            next: (transactions) => {
                this.rawTransactions = transactions;
                this.transactionsLoaded = true;
                this.rebuildRows();
            },
            error: () => {
                this.statusMessage = 'Could not load transactions.';
                this.transactionsLoaded = true;
                this.rebuildRows();
            }
        });
    }

    private rebuildRows() {
        if (!this.accountsLoaded || !this.transactionsLoaded) {
            return;
        }

        const rows = this.rawTransactions.map(transaction => {
            const account = this.accountsData.find(item => item.cardNumber === transaction.cardNumber);
            return {
                ...transaction,
                cardHolderName: account?.cardHolderName || 'Unknown',
                accountStatus: account?.status || 'ACTIVE'
            };
        });

        this.dataSource.data = rows;
        const amounts = rows.map(row => Number(row.amount)).filter(amount => !Number.isNaN(amount));
        const min = amounts.length ? Math.min(...amounts) : 0;
        const max = amounts.length ? Math.max(...amounts) : 0;
        this.amountBounds = { min, max };
        this.filters.amountFrom = min;
        this.filters.amountTo = max;
        this.applyFilters();
        this.finishLoading();
    }

    private toDateOnly(value: string): string {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return date.toISOString().slice(0, 10);
    }

    private finishLoading() {
        if (!this.accountsLoaded || !this.transactionsLoaded) {
            return;
        }
        this.loading = false;
        this.loaded = true;
        this.cdr.detectChanges();
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
