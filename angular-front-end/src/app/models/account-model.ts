export interface AccountModel {
    id: number,
    cardNumber: string,
    cardHolderName: string
    amount: number,
    dailyTxLimit: number,
    dailyTxSumLimit: number,
    dailyTx: number,
    dailyTxSum: number,
}
