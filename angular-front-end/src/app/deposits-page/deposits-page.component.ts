import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
import { AccountModel } from '../models/account-model';
import { DepositModel, DepositsStateModel } from '../models/deposit.model';
import { FrontEndService } from '../utils/front-end.service';
import { DepositsStoreService } from '../utils/deposits-store.service';
import { UserService } from '../utils/user.service';

@Component({
    selector: 'app-deposits-page',
    templateUrl: './deposits-page.component.html',
    styleUrls: ['./deposits-page.component.scss']
})
export class DepositsPageComponent implements OnInit {
    currentUserId: number | null = null;
    currentUserName = '';
    currentAccount: AccountModel | null = null;
    deposits: DepositModel[] = [];
    feedbackMessage = '';
    feedbackKind: 'success' | 'error' | 'info' = 'info';
    loading = true;
    newDepositName = '';
    newDepositAmount: number | null = null;
    transferFromId: number | null = null;
    transferToId: number | null = null;
    transferAmount: number | null = null;
    deleteConfirmOpen = false;
    depositPendingDeletion: DepositModel | null = null;

    constructor(
        private readonly frontEndService: FrontEndService,
        private readonly depositsStore: DepositsStoreService,
        private readonly userService: UserService
    ) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Deposits page');
        this.loadDeposits();
    }

    get primaryDeposit(): DepositModel | null {
        return this.deposits.find(deposit => deposit.isPrimary) ?? null;
    }

    get totalBalance(): number {
        return this.deposits.reduce((sum, deposit) => sum + deposit.balance, 0);
    }

    createDeposit(): void {
        if (this.currentUserId == null || !this.currentAccount) {
            this.setFeedback('No primary account is available for this user.', 'error');
            return;
        }

        try {
            const state = this.depositsStore.createDeposit(
                this.currentUserId,
                this.currentAccount,
                this.newDepositName,
                Number(this.newDepositAmount)
            );
            this.applyState(state);
            this.newDepositName = '';
            this.newDepositAmount = null;
            this.setFeedback('Deposit created and funded from the primary account.', 'success');
            this.trackAudit('ACTION: Created deposit');
        } catch (error) {
            this.setFeedback(this.extractErrorMessage(error), 'error');
        }
    }

    transferFunds(): void {
        if (this.currentUserId == null || !this.currentAccount) {
            this.setFeedback('No primary account is available for this user.', 'error');
            return;
        }

        if (this.transferFromId == null || this.transferToId == null) {
            this.setFeedback('Choose both deposits before transferring money.', 'error');
            return;
        }

        try {
            const fromName = this.deposits.find(deposit => deposit.id === this.transferFromId)?.name ?? 'deposit';
            const toName = this.deposits.find(deposit => deposit.id === this.transferToId)?.name ?? 'deposit';
            const state = this.depositsStore.transferBetweenDeposits(
                this.currentUserId,
                this.currentAccount,
                Number(this.transferFromId),
                Number(this.transferToId),
                Number(this.transferAmount)
            );
            this.applyState(state);
            this.transferAmount = null;
            this.setFeedback('Transfer completed between deposits.', 'success');
            this.trackAudit(`ACTION: Transfer between deposits (${fromName} -> ${toName})`);
        } catch (error) {
            this.setFeedback(this.extractErrorMessage(error), 'error');
        }
    }

    deleteDeposit(depositId: number): void {
        if (this.currentUserId == null || !this.currentAccount) {
            this.setFeedback('No primary account is available for this user.', 'error');
            return;
        }

        const deposit = this.deposits.find(item => item.id === depositId);
        if (!deposit) {
            this.setFeedback('Selected deposit could not be found.', 'error');
            return;
        }

        this.depositPendingDeletion = deposit;
        this.deleteConfirmOpen = true;
    }

    cancelDeleteDeposit(): void {
        this.deleteConfirmOpen = false;
        this.depositPendingDeletion = null;
    }

    confirmDeleteDeposit(): void {
        if (this.currentUserId == null || !this.currentAccount || !this.depositPendingDeletion) {
            this.cancelDeleteDeposit();
            return;
        }

        try {
            const deposit = this.depositPendingDeletion;
            const state = this.depositsStore.deleteDeposit(this.currentUserId, this.currentAccount, deposit.id);
            this.applyState(state);
            this.setFeedback(`"${deposit.name}" was deleted and its balance returned to the primary account.`, 'success');
            this.trackAudit(`ACTION: Deleted deposit ${deposit.name}`);
        } catch (error) {
            this.setFeedback(this.extractErrorMessage(error), 'error');
        } finally {
            this.cancelDeleteDeposit();
        }
    }

    private loadDeposits(): void {
        this.userService.getCurrentUser().subscribe({
            next: (user) => {
                this.currentUserId = user.id ?? null;
                this.currentUserName = user.name ?? '';

                if (user.accountId == null) {
                    this.currentAccount = null;
                    this.deposits = [];
                    this.setFeedback('No primary account is assigned to this user.', 'error');
                    this.loading = false;
                    return;
                }

                this.frontEndService.getAccounts().subscribe({
                    next: (accounts) => {
                        this.currentAccount = accounts.find(account => account.id === user.accountId) ?? null;

                        if (!this.currentAccount) {
                            this.deposits = [];
                            this.setFeedback('The user account could not be found.', 'error');
                            this.loading = false;
                            return;
                        }

                        const state = this.depositsStore.loadState(this.currentUserId, this.currentAccount);
                        this.applyState(state);
                        this.loading = false;
                    },
                    error: () => {
                        this.setFeedback('Could not load account details.', 'error');
                        this.loading = false;
                    }
                });
            },
            error: () => {
                this.setFeedback('Could not load user details.', 'error');
                this.loading = false;
            }
        });
    }

    private applyState(state: DepositsStateModel): void {
        this.deposits = [...state.deposits].sort((a, b) => {
            if (a.isPrimary && !b.isPrimary) {
                return -1;
            }
            if (!a.isPrimary && b.isPrimary) {
                return 1;
            }
            return a.id - b.id;
        });

        if (this.transferFromId == null) {
            this.transferFromId = this.primaryDeposit?.id ?? this.deposits[0]?.id ?? null;
        }

        if (this.transferToId == null) {
            this.transferToId = this.deposits.find(deposit => !deposit.isPrimary)?.id ?? this.deposits[0]?.id ?? null;
        }

        if (this.transferFromId != null && !this.deposits.some(deposit => deposit.id === this.transferFromId)) {
            this.transferFromId = this.primaryDeposit?.id ?? this.deposits[0]?.id ?? null;
        }

        if (this.transferToId != null && !this.deposits.some(deposit => deposit.id === this.transferToId)) {
            this.transferToId = this.deposits.find(deposit => !deposit.isPrimary)?.id ?? this.deposits[0]?.id ?? null;
        }
    }

    private setFeedback(message: string, kind: 'success' | 'error' | 'info'): void {
        this.feedbackMessage = message;
        this.feedbackKind = kind;
    }

    private extractErrorMessage(error: unknown): string {
        if (error instanceof Error) {
            return error.message;
        }
        return 'The requested operation could not be completed.';
    }

    private trackAudit(action: string): void {
        this.frontEndService.recordAudit(action)
            .pipe(take(1))
            .subscribe({
                error: () => {
                    console.warn('Audit tracking failed silently:', action);
                }
            });
    }
}
