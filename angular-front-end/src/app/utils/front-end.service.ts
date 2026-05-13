import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {AccountModel} from "../models/account-model";
import {TransactionModel} from "../models/transaction-model";
import {UserModel} from "../models/user.model";

@Injectable({
    providedIn: 'root'
})
export class FrontEndService {

    constructor(private readonly _httpClient: HttpClient) {
    }

    getAccounts(): Observable<AccountModel[]> {
        return this._httpClient.get<AccountModel[]>('/api/accounts/');
    }

    createAccount(account: any): Observable<AccountModel> {
        return this._httpClient.post<AccountModel>('/api/accounts/', account);
    }

    deleteAccount(id: number): Observable<void> {
        return this._httpClient.delete<void>(`/api/accounts/${id}`);
    }

    createTransaction(transaction: any): Observable<any> {
        return this._httpClient.post<any>('/api/accounts/transaction', transaction);
    }

    getTransactions(): Observable<TransactionModel[]> {
        return this._httpClient.get<TransactionModel[]>('/api/accounts/transactions/');
    }

    getTransactionsOfCardNumber(cardNumber: string): Observable<TransactionModel[]> {
        return this._httpClient.get<TransactionModel[]>('/api/accounts/transactions/' + cardNumber);
    }

    getTransactionsOfAccount(account: AccountModel): Observable<TransactionModel[]> {
        return this._httpClient.get<TransactionModel[]>('/api/accounts/transactions/' + account.cardNumber);
    }

    getUsers(): Observable<UserModel[]> {
        return this._httpClient.get<UserModel[]>('/api/users/');
    }

    deleteUser(id: number): Observable<void> {
        return this._httpClient.delete<void>(`/api/users/${id}`);
    }
}
