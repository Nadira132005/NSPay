export interface TransactionModel {
    id: number,
    amount: number,
    cardNumber: string,
    returnCode: number,
    timestamp: string,
    status?: 'completed' | 'pending' | 'processing',
    scheduledAt?: string,
    localId?: string
}

export const TRANSACTION_STATUS_LABELS: Record<string, string> = {
    '00': 'Success',
    '13': 'Inactive account',
    '14': 'Invalid card number',
    '51': 'Insufficient funds',
    '61': 'Daily transaction sum exceeded',
    '65': 'Daily transaction limit exceeded'
};

export const TRANSACTION_SUCCESS_CODE = '00';
