import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from "@angular/router";

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

    loginUser(userData) {
        this.http.post(`/api/login/`, userData)
            .subscribe(resData => {
                if (resData === null) {
                    alert("Invalid Credentials");
                } else {
                    this.toHomePage(resData)
                }
            })
    }

    toHomePage(resData) {
        window.sessionStorage.setItem("token", resData.token)
        window.sessionStorage.setItem("userId", resData.userId)
        this.router.navigate([this.returnUrl])
    }
}
