export interface PendingTransactionModel {
    localId: string;
    cardNumber: string;
    amount: number;
    scheduledAt: string;
    createdAt: string;
    status: 'pending' | 'processing';
}
