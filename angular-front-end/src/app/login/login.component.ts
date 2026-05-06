import {Component, OnInit} from '@angular/core';
import {UserService} from "../utils/user.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  user: any = {}

  constructor(private userService: UserService) { }

  onSubmit(){
    this.userService.loginUser(this.user)
  }

  ngOnInit(): void {
    this.userService.toLogin();
  }
}
