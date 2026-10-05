import {Component, OnInit} from '@angular/core';
import {NgForm} from '@angular/forms';
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
  busy = false;

  constructor(
      private userService: UserService,
      private router: Router
  ) { }

  submitFromKeyboard(event: Event, form: NgForm): void {
    if ((event as KeyboardEvent).isComposing) return;
    event.preventDefault();
    form.control.markAllAsTouched();
    if (form.valid) this.onSubmit();
  }

  onSubmit(){
    if (this.busy || !this.user.name?.trim() || !this.user.password) return;
    this.busy = true;
    this.statusMessage = '';
    this.userService.loginUser(this.user).subscribe({//asynchronous
      next: (resData) => {
        this.busy = false;
        this.userService.toHomePage(resData);
      },
      error: () => {
        this.busy = false;
        this.statusMessage = 'Could not sign in. Check your credentials and that the backend is running.';
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
    this.router.navigateByUrl('/dashboard/overview');
  }
}
