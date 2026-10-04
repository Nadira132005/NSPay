import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Router} from "@angular/router";
import {LoginResponseModel, UserModel, UserRole} from "../models/user.model";
import {Observable} from "rxjs";

@Injectable({providedIn: 'root'})
export class UserService {
    private readonly tokenCookieName = 'auth_token';

    constructor(
        private http: HttpClient,
        private router: Router
    ) {
    }

    toLogin() {
        this.router.navigateByUrl('/login')
    }

    loginUser(userData: UserModel): Observable<LoginResponseModel> {//asynchronous. to subscribe to
        return this.http.post<LoginResponseModel>(`/api/login/`, userData);
    }

    changePassword(password: string): Observable<LoginResponseModel> {
        return this.http.put<LoginResponseModel>('/api/users/me/password', { password });
    }

    createUser(userData: UserModel) {
        const headers = new HttpHeaders({
            'userRole': this.getCurrentRole() ?? ''// missing role => empty string
        });
        return this.http.post('/api/users/', userData, {headers, responseType: 'text'});
    }

    updateUser(id: number, userData: UserModel) {
        const headers = new HttpHeaders({
            'userRole': this.getCurrentRole() ?? ''
        });
        return this.http.put(`/api/users/${id}`, userData, {headers});
    }

    getUsers(): Observable<UserModel[]> {
        return this.http.get<UserModel[]>('/api/users/');
    }

    getCurrentUser(): Observable<UserModel> {
        return this.http.get<UserModel>('/api/users/me');
    }

    deleteUser(id: number): Observable<void> {
        return this.http.delete<void>(`/api/users/${id}`);
    }

    toHomePage(resData: LoginResponseModel) {
        this.storeLogin(resData);
        this.router.navigateByUrl(resData.passwordChangeRequired ? '/change-password' : '/dashboard/accounts');
    }

    storeLogin(resData: LoginResponseModel) {
        this.setCookie(this.tokenCookieName, resData.token, 1);
    }

    isPasswordChangeRequired(): boolean {
        const token = this.getAuthToken();
        if (!token || this.isJwtExpired(token)) return false;
        return this.decodeJwtPayload(token)?.passwordChangeRequired === true;
    }

    getCurrentRole(): UserRole | null {
        const token = this.getAuthToken();
        if (!token || this.isJwtExpired(token)) {
            return null;
        }
        const payload = this.decodeJwtPayload(token);
        return (payload?.role as UserRole | undefined) ?? null;
    }

    getCurrentUserName(): string | null {
        const token = this.getAuthToken();
        if (!token || this.isJwtExpired(token)) {
            return null;
        }
        const payload = this.decodeJwtPayload(token);
        return (payload?.sub as string | undefined) ?? null;
    }

    getCurrentAccountCardNumber(): string | null {
        const token = this.getAuthToken();
        if (!token || this.isJwtExpired(token)) {
            return null;
        }
        const payload = this.decodeJwtPayload(token);
        return (payload?.accountCardNumber as string | undefined) ?? null;
    }

    isLoggedIn(): boolean {
        const token = this.getAuthToken();
        return !!token && !this.isJwtExpired(token);//!(boolean value inverted)
    }

    getAuthToken(): string | null {
        return this.getCookie(this.tokenCookieName);
    }

    logout() {
        this.deleteCookie(this.tokenCookieName);
        this.router.navigateByUrl('/');
    }

    private setCookie(name: string, val: string, days: number) {
        const date = new Date();
        const value = val;
        // Set it expire in the requested number of days
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));

        // Set it
        document.cookie = name+"="+value+"; expires="+date.toUTCString()+"; path=/";
    }

    private getCookie(name: string): string | null {
        const value = "; " + document.cookie;
        const parts = value.split("; " + name + "=");

        if (parts.length == 2) {
            return parts.pop()?.split(";").shift() ?? null;
        }
        return null;
    }

    private deleteCookie(name: string): void {
        const date = new Date();
        // Set it expire in -1 days
        date.setTime(date.getTime() + (-1 * 24 * 60 * 60 * 1000));

        // Set it
        document.cookie = name+"=; expires="+date.toUTCString()+"; path=/";
    }

    private decodeJwtPayload(token: string): any | null {
        const parts = token.split('.');
        if (parts.length !== 3) {
            return null;
        }
        try {
            const payload = this.normalizeBase64(parts[1]);
            return JSON.parse(atob(payload));
        } catch (error) {
            return null;
        }
    }

    private normalizeBase64(value: string): string {
        const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
        const padding = base64.length % 4;
        return padding === 0 ? base64 : base64 + '='.repeat(4 - padding);
    }
    private isJwtExpired(token: string): boolean {
        const payload = this.decodeJwtPayload(token);
        if (!payload?.exp) {
            return false;
        }
        return Date.now() >= Number(payload.exp) * 1000;//in seconds
    }

}
