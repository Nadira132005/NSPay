export interface AccountModel {
    id: number,
    cardNumber: string,
    cardHolderName: string
    status?: string,
    amount: number,
    dailyTxLimit: number,
    dailyTxSumLimit: number,
    dailyTx: number,
    dailyTxSum: number,
}
