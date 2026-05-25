import { Injectable } from '@angular/core';
import { AccountModel } from '../models/account-model';
import { DepositsStateModel, DepositModel } from '../models/deposit.model';

@Injectable({
    providedIn: 'root'
})
export class DepositsStoreService {
    private readonly storagePrefix = 'payments-deposits';

    loadState(userId: number | null, account: AccountModel | null): DepositsStateModel {
        if (userId == null) {
            return this.createState(0, account);
        }

        const storedState = this.readState(userId);
        if (!storedState || storedState.accountId !== account?.id) {
            const freshState = this.createState(userId, account);
            this.writeState(userId, freshState);
            return freshState;
        }

        const repairedState = this.repairState(userId, storedState, account);
        this.writeState(userId, repairedState);
        return repairedState;
    }

    getPrimaryBalance(userId: number | null, account: AccountModel | null): number {
        const state = this.loadState(userId, account);
        return state.deposits.find(deposit => deposit.isPrimary)?.balance ?? 0;
    }

    createDeposit(userId: number | null, account: AccountModel | null, name: string, amount: number): DepositsStateModel {
        const state = this.loadState(userId, account);
        const primaryDeposit = this.requirePrimaryDeposit(state);
        const depositName = name.trim();
        const normalizedAmount = this.normalizeAmount(amount);

        if (!depositName) {
            throw new Error('Deposit name is required.');
        }

        if (normalizedAmount <= 0) {
            throw new Error('Initial transfer amount must be greater than zero.');
        }

        if (primaryDeposit.balance < normalizedAmount) {
            throw new Error('Primary account does not have enough funds for this deposit.');
        }

        const newDeposit: DepositModel = {
            id: state.nextId,
            name: depositName,
            balance: normalizedAmount,
            isPrimary: false
        };

        primaryDeposit.balance = this.normalizeAmount(primaryDeposit.balance - normalizedAmount);
        state.nextId += 1;
        state.deposits = [...state.deposits, newDeposit];
        this.writeState(userId, state);
        return state;
    }

    transferBetweenDeposits(
        userId: number | null,
        account: AccountModel | null,
        fromDepositId: number,
        toDepositId: number,
        amount: number
    ): DepositsStateModel {
        const state = this.loadState(userId, account);
        const normalizedAmount = this.normalizeAmount(amount);

        if (fromDepositId === toDepositId) {
            throw new Error('Select two different deposits.');
        }

        if (normalizedAmount <= 0) {
            throw new Error('Transfer amount must be greater than zero.');
        }

        const fromDeposit = state.deposits.find(deposit => deposit.id === fromDepositId);
        const toDeposit = state.deposits.find(deposit => deposit.id === toDepositId);

        if (!fromDeposit || !toDeposit) {
            throw new Error('Selected deposits could not be found.');
        }

        if (fromDeposit.balance < normalizedAmount) {
            throw new Error(`"${fromDeposit.name}" does not have enough funds.`);
        }

        fromDeposit.balance = this.normalizeAmount(fromDeposit.balance - normalizedAmount);
        toDeposit.balance = this.normalizeAmount(toDeposit.balance + normalizedAmount);
        this.writeState(userId, state);
        return state;
    }

    deleteDeposit(
        userId: number | null,
        account: AccountModel | null,
        depositId: number
    ): DepositsStateModel {
        const state = this.loadState(userId, account);
        const depositIndex = state.deposits.findIndex(deposit => deposit.id === depositId);

        if (depositIndex === -1) {
            throw new Error('Selected deposit could not be found.');
        }

        const deposit = state.deposits[depositIndex];
        if (deposit.isPrimary) {
            throw new Error('Primary account cannot be deleted.');
        }

        const primaryDeposit = this.requirePrimaryDeposit(state);
        primaryDeposit.balance = this.normalizeAmount(primaryDeposit.balance + deposit.balance);
        state.deposits.splice(depositIndex, 1);
        this.writeState(userId, state);
        return state;
    }

    private createState(userId: number, account: AccountModel | null): DepositsStateModel {
        return {
            userId,
            accountId: account?.id ?? null,
            nextId: 2,
            deposits: [
                {
                    id: 1,
                    name: account?.cardNumber ? `Primary account ${account.cardNumber}` : 'Primary account',
                    balance: account?.amount ?? 0,
                    isPrimary: true
                }
            ]
        };
    }

    private readState(userId: number): DepositsStateModel | null {
        const rawState = localStorage.getItem(this.getStorageKey(userId));
        if (!rawState) {
            return null;
        }

        try {
            return JSON.parse(rawState) as DepositsStateModel;
        } catch {
            return null;
        }
    }

    private writeState(userId: number | null, state: DepositsStateModel): void {
        if (userId == null) {
            return;
        }

        localStorage.setItem(this.getStorageKey(userId), JSON.stringify(state));
    }

    private repairState(userId: number, state: DepositsStateModel, account: AccountModel | null): DepositsStateModel {
        const normalizedState: DepositsStateModel = {
            ...state,
            userId,
            accountId: account?.id ?? state.accountId,
            deposits: Array.isArray(state.deposits) ? state.deposits : []
        };

        let primaryDeposit = normalizedState.deposits.find(deposit => deposit.isPrimary);
        if (!primaryDeposit) {
            primaryDeposit = {
                id: 1,
                name: account?.cardNumber ? `Primary account ${account.cardNumber}` : 'Primary account',
                balance: account?.amount ?? 0,
                isPrimary: true
            };
            normalizedState.deposits = [primaryDeposit, ...normalizedState.deposits.filter(deposit => !deposit.isPrimary)];
        }

        normalizedState.nextId = Math.max(
            normalizedState.nextId || 2,
            ...normalizedState.deposits.map(deposit => deposit.id + 1),
            2
        );

        return normalizedState;
    }

    private requirePrimaryDeposit(state: DepositsStateModel): DepositModel {
        const deposit = state.deposits.find(item => item.isPrimary);
        if (!deposit) {
            throw new Error('Primary account deposit is not available.');
        }
        return deposit;
    }

    private getStorageKey(userId: number): string {
        return `${this.storagePrefix}:${userId}`;
    }

    private normalizeAmount(value: number): number {
        return Math.round((Number(value) + Number.EPSILON) * 100) / 100;
    }
}
