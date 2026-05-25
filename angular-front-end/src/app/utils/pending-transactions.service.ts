import { Injectable } from '@angular/core';
import { PendingTransactionModel } from '../models/pending-transaction.model';

@Injectable({
    providedIn: 'root'
})
export class PendingTransactionsService {
    private readonly storageKey = 'payments-pending-transactions';

    listForCardNumber(cardNumber: string): PendingTransactionModel[] {
        return this.readAll().filter(transaction => transaction.cardNumber === cardNumber);
    }

    add(transaction: Omit<PendingTransactionModel, 'localId' | 'createdAt' | 'status'>): PendingTransactionModel {
        const pendingTransaction: PendingTransactionModel = {
            ...transaction,
            localId: this.createLocalId(),
            createdAt: new Date().toISOString(),
            status: 'pending'
        };

        const transactions = this.readAll();
        transactions.push(pendingTransaction);
        this.writeAll(transactions);
        return pendingTransaction;
    }

    markProcessing(localId: string): void {
        const transactions = this.readAll();
        const transaction = transactions.find(item => item.localId === localId);
        if (!transaction) {
            return;
        }

        transaction.status = 'processing';
        this.writeAll(transactions);
    }

    remove(localId: string): void {
        this.writeAll(this.readAll().filter(item => item.localId !== localId));
    }

    private readAll(): PendingTransactionModel[] {
        const raw = localStorage.getItem(this.storageKey);
        if (!raw) {
            return [];
        }

        try {
            const parsed = JSON.parse(raw) as PendingTransactionModel[];
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    private writeAll(transactions: PendingTransactionModel[]): void {
        localStorage.setItem(this.storageKey, JSON.stringify(transactions));
    }

    private createLocalId(): string {
        return `pending-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    }
}
