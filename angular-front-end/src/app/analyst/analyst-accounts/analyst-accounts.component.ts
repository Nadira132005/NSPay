import { Component, OnInit } from '@angular/core';
import { AccountModel } from '../../models/account-model';
import { FrontEndService } from '../../utils/front-end.service';

@Component({
    selector: 'app-analyst-accounts',
    templateUrl: './analyst-accounts.component.html',
    styleUrls: ['./analyst-accounts.component.scss']
})
export class AnalystAccountsComponent implements OnInit {
    accountsData: AccountModel[] = [];
    filteredAccounts: AccountModel[] = [];
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
        this.statusFilter = 'all';
        this.loadAccounts();
    }

    applyFilters() {
        this.filteredAccounts = this.accountsData.filter(account => {
            const rowStatus = account.status || 'ACTIVE';
            const amount = Number(account.amount);
            return (this.statusFilter === 'all' || rowStatus === this.statusFilter)
                && amount >= Number(this.amountFrom)
                && amount <= Number(this.amountTo);
        });
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
