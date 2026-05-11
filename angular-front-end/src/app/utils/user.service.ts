import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Router} from "@angular/router";
import {LoginResponseModel, UserModel, UserRole} from "../models/user.model";
import {Observable} from "rxjs";

@Injectable({providedIn: 'root'})
export class UserService {
    returnUrl: string = "accounts";
    loginPage: string = "login";

    constructor(
        private http: HttpClient,
        private router: Router
    ) {
    }

    toLogin() {
        this.router.navigate([this.loginPage])
    }

    loginUser(userData: UserModel): Observable<LoginResponseModel> {//asynchronous. to subscribe to
        return this.http.post<LoginResponseModel>(`/api/login/`, userData);
    }

    createUser(userData: UserModel) {
        const headers = new HttpHeaders({
            'userRole': this.getCurrentRole() ?? ''// missing role => empty string
        });
        return this.http.post('/api/users/', userData, {headers, responseType: 'text'});
    }

    toHomePage(resData: LoginResponseModel) {
        window.sessionStorage.setItem("token", resData.token)
        window.sessionStorage.setItem("userId", String(resData.userId))
        window.sessionStorage.setItem("role", resData.role)
        this.router.navigateByUrl('/dashboard/accounts')// or navigate(['dashboard', 'accounts']);
    }

    getCurrentRole(): UserRole | null {
        return window.sessionStorage.getItem("role") as UserRole | null;
    }

    isLoggedIn(): boolean {
        return !!window.sessionStorage.getItem("token");//!(boolean value inverted)
    }

    clearSession() {
        window.sessionStorage.removeItem("token");
        window.sessionStorage.removeItem("userId");
        window.sessionStorage.removeItem("role");
    }

    logout() {
        this.clearSession();
        this.router.navigateByUrl('/');
    }
}
