import {Component, OnInit} from '@angular/core';
import {Router} from "@angular/router";
import {UserService} from "../utils/user.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  user: any = {}
  statusMessage = '';

  constructor(
      private userService: UserService,
      private router: Router
  ) { }

  onSubmit(){
    this.statusMessage = '';
    this.userService.loginUser(this.user).subscribe({//asynchronous
      next: (resData) => {
        this.userService.toHomePage(resData);
      },
      error: () => {
        this.statusMessage = 'Invalid credentials';
      }
    });
  }

  ngOnInit(): void {
    if (!this.userService.isLoggedIn()) {
      return;
    }
    if (this.userService.isPasswordChangeRequired()) {
      this.router.navigateByUrl('/change-password');
      return;
    }
    this.router.navigateByUrl('/dashboard/accounts');
  }
}
