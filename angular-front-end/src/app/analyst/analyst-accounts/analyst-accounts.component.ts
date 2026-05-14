import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { AccountModel } from '../../models/account-model';
import { FrontEndService } from '../../utils/front-end.service';

@Component({
    selector: 'app-analyst-accounts',
    templateUrl: './analyst-accounts.component.html',
    styleUrls: ['./analyst-accounts.component.scss']
})
export class AnalystAccountsComponent implements OnInit, AfterViewInit {
    @ViewChild(MatPaginator) paginator!: MatPaginator;

    dataSource = new MatTableDataSource<AccountModel>([]);
    accountsData: AccountModel[] = [];
    pageSize = 10;
    displayedColumns: string[] = [
        'id',
        'cardNumber',
        'cardHolderName',
        'status',
        'amount',
        'dailyTxLimit',
        'dailyTxSumLimit',
        'dailyTx',
        'dailyTxSum'
    ];
    statusFilter = 'all';
    amountFrom = 0;
    amountTo = 0;
    amountBounds = { min: 0, max: 0 };
    statuses = ['all', 'ACTIVE', 'SUSPENDED', 'BLOCKED', 'CLOSED', 'FROZEN'];
    statusMessage = '';

    constructor(private frontEndService: FrontEndService) {}

    ngOnInit(): void {
        this.dataSource.filterPredicate = (account, filter) => {
            const parsed = JSON.parse(filter) as { status: string; amountFrom: number; amountTo: number };
            const rowStatus = account.status || 'ACTIVE';
            const amount = Number(account.amount);
            return (parsed.status === 'all' || rowStatus === parsed.status)
                && amount >= Number(parsed.amountFrom)
                && amount <= Number(parsed.amountTo);
        };
        this.statusFilter = 'all';
        this.loadAccounts();
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
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

    applyFilters() {
        this.dataSource.filter = JSON.stringify({
            status: this.statusFilter,
            amountFrom: this.amountFrom,
            amountTo: this.amountTo
        });
        this.dataSource.paginator?.firstPage();
    }

    onAmountFromChange(value: string | number) {
        const nextValue = Number(value);
        this.amountFrom = Math.min(nextValue, this.amountTo);
        this.applyFilters();
    }

    onAmountToChange(value: string | number) {
        const nextValue = Number(value);
        this.amountTo = Math.max(nextValue, this.amountFrom);
        this.applyFilters();
    }

    resetAmountFilter() {
        this.amountFrom = this.amountBounds.min;
        this.amountTo = this.amountBounds.max;
        this.applyFilters();
    }

    private loadAccounts() {
        this.frontEndService.getAccounts().subscribe({
            next: (accounts) => {
                this.accountsData = accounts.map(account => ({
                    ...account,
                    status: account.status || 'ACTIVE'
                }));
                this.dataSource.data = this.accountsData;

                const amounts = accounts.map(account => Number(account.amount)).filter(amount => !Number.isNaN(amount));
                const min = amounts.length ? Math.min(...amounts) : 0;
                const max = amounts.length ? Math.max(...amounts) : 0;
                this.amountBounds = { min, max };
                this.amountFrom = min;
                this.amountTo = max;
                this.applyFilters();
            },
            error: () => {
                this.statusMessage = 'Could not load accounts.';
            }
        });
    }
}
