import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {AccountModel} from "../models/account-model";

@Injectable({
    providedIn: 'root'
})
export class FrontEndService {

    constructor(private readonly _httpClient: HttpClient) {
    }

    getAccounts(): Observable<any> {
        return this._httpClient.get<any>('/api/accounts/');
    }

    getTransactions(): Observable<any> {
        return this._httpClient.get<any>('/api/accounts/transactions/');
    }

    getTransactionsOfAccount(account:AccountModel): Observable<any> {
        return this._httpClient.get<any>('/api/accounts/transactions/' + account.cardNumber);
    }
}
