import { AfterViewInit, ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { UserModel } from '../../models/user.model';
import { AccountModel } from '../../models/account-model';
import { FrontEndService } from '../../utils/front-end.service';
import { UserService } from '../../utils/user.service';

interface AnalystUserRow extends UserModel {
    accountLabel: string;
    accountStatus: string;
}

@Component({
    selector: 'app-analyst-users',
    templateUrl: './analyst-users.component.html',
    styleUrls: ['./analyst-users.component.scss']
})
export class AnalystUsersComponent implements OnInit, AfterViewInit {
    @ViewChild(MatPaginator) paginator!: MatPaginator;
    @ViewChild(MatSort) sort!: MatSort;

    dataSource = new MatTableDataSource<AnalystUserRow>([]);
    accountsData: AccountModel[] = [];
    pageSize = 10;
    displayedColumns: string[] = ['id', 'name', 'role', 'account'];
    roles: string[] = ['Admin', 'User', 'Analyst'];
    roleFilter = 'all';
    searchFilter = '';
    loading = false;
    loaded = false;
    statusMessage = '';
    private loadingStartedAt = 0;

    constructor(
        private frontEndService: FrontEndService,
        private userService: UserService,
        private cdr: ChangeDetectorRef
    ) {
        this.dataSource.filterPredicate = (row, filter) => {
            const parsed = JSON.parse(filter) as { role: string; search: string };
            const roleMatches = parsed.role === 'all' || row.role === parsed.role;
            const search = parsed.search.trim().toLowerCase();
            const searchMatches = !search
                || row.name.toLowerCase().includes(search)
                || row.accountLabel.toLowerCase().includes(search)
                || row.accountStatus.toLowerCase().includes(search);
            return roleMatches && searchMatches;
        };
        this.dataSource.sortingDataAccessor = (row, column) => {
            switch (column) {
                case 'role':
                    return row.role || '';
                case 'account':
                    return row.accountLabel || '';
                default:
                    return (row as any)[column] ?? '';
            }
        };
    }

    ngOnInit(): void {
        this.roleFilter = 'all';
        this.loading = true;
        this.loadingStartedAt = Date.now();
        this.loadData();
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

    applyFilters() {
        this.dataSource.filter = JSON.stringify({
            role: this.roleFilter,
            search: this.searchFilter
        });
    }

    private loadData() {
        this.loaded = false;
        this.frontEndService.getAccounts().subscribe({
            next: (accounts) => {
                this.accountsData = accounts;
                this.userService.getUsers().subscribe({
                    next: (users) => {
                        const rows = users.map(user => {
                            const account = this.accountsData.find(item => item.id === user.accountId);
                            return {
                                ...user,
                                role: user.role || 'User',
                                accountLabel: account ? `${account.cardHolderName} - ${account.cardNumber}` : 'No account',
                                accountStatus: account?.status || 'ACTIVE'
                            };
                        });
                        this.dataSource.data = rows;
                        this.applyFilters();
                        this.finishLoading();
                    },
                    error: () => {
                        this.statusMessage = 'Could not load users.';
                        this.finishLoading();
                    }
                });
            },
            error: () => {
                this.statusMessage = 'Could not load accounts.';
                this.finishLoading();
            }
        });
    }

    private finishLoading() {
        this.loading = false;
        this.loaded = true;
        this.cdr.detectChanges();
    }
}
