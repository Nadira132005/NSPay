import { NgModule} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppComponent } from './app.component';
import { AccountsTableComponent } from './accounts-table/accounts-table.component';
import {MatTableModule} from '@angular/material/table';
import {HttpClientModule} from "@angular/common/http";
import { LoginComponent } from './login/login.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ROUTING} from "./app.routing";
import {CdkMenuItemCheckbox} from "@angular/cdk/menu";

@NgModule({
  declarations: [
    AppComponent,
    AccountsTableComponent,
    LoginComponent
  ],
    imports: [
        BrowserModule,
        MatTableModule,
        HttpClientModule,
        ReactiveFormsModule,
        FormsModule,
        ROUTING,
        CdkMenuItemCheckbox
    ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule { }
