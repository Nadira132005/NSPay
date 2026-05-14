import { NgModule} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AppComponent } from './app.component';
import { AccountsTableComponent } from './accounts-table/accounts-table.component';
import {MatTableModule} from '@angular/material/table';
import {HttpClientModule} from "@angular/common/http";
import { LoginComponent } from './login/login.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ROUTING} from "./app.routing";
import {CdkMenuItemCheckbox} from "@angular/cdk/menu";
import { DashboardComponent } from './dashboard/dashboard.component';
import { AdminUsersComponent } from './admin/admin-users/admin-users.component';
import { AdminPageComponent } from './admin/admin-page/admin-page.component';
import { AdminAccountsComponent } from './admin/admin-accounts/admin-accounts.component';
import { AdminPagePanelComponent } from './admin/admin-page-panel/admin-page-panel.component';
import { TransactionCreateComponent } from './transactions/transaction-create/transaction-create.component';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatSliderModule } from '@angular/material/slider';
import { AnalystPageComponent } from './analyst/analyst-page/analyst-page.component';
import { AnalystAccountsComponent } from './analyst/analyst-accounts/analyst-accounts.component';
import { AnalystUsersComponent } from './analyst/analyst-users/analyst-users.component';
import { AnalystPagePanelComponent } from './analyst/analyst-page-panel/analyst-page-panel.component';

@NgModule({
  declarations: [
    AppComponent,
    AccountsTableComponent,
    LoginComponent,
    DashboardComponent,
    AdminUsersComponent,
    AdminPageComponent,
    AdminAccountsComponent,
    AdminPagePanelComponent,
    TransactionCreateComponent,
    AnalystPageComponent,
    AnalystAccountsComponent,
    AnalystUsersComponent,
    AnalystPagePanelComponent
  ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatSliderModule,
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
