export interface DepositModel {
    id: number;
    name: string;
    balance: number;
    isPrimary: boolean;
}

export interface DepositsStateModel {
    userId: number;
    accountId: number | null;
    nextId: number;
    deposits: DepositModel[];
}
