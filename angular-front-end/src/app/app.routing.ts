import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
//Components
import { LoginComponent } from './login/login.component';
import {AccountsTableComponent} from "./accounts-table/accounts-table.component";
import {authorizationGuard} from "./authorization.guard";
import {DashboardComponent} from "./dashboard/dashboard.component";
import {AdminUsersComponent} from "./admin/admin-users/admin-users.component";
import {AdminPageComponent} from "./admin/admin-page/admin-page.component";
import {AdminAccountsComponent} from "./admin/admin-accounts/admin-accounts.component";
import {AdminPagePanelComponent} from "./admin/admin-page-panel/admin-page-panel.component";
import {TransactionCreateComponent} from "./transactions/transaction-create/transaction-create.component";
import {AnalystPageComponent} from "./analyst/analyst-page/analyst-page.component";
import {AnalystAccountsComponent} from "./analyst/analyst-accounts/analyst-accounts.component";
import {AnalystUsersComponent} from "./analyst/analyst-users/analyst-users.component";
import {AnalystPagePanelComponent} from "./analyst/analyst-page-panel/analyst-page-panel.component";

export const ROUTES: Routes = [
    {path: '', component: LoginComponent},
    {path: 'login', component: LoginComponent},
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [authorizationGuard],
        data: {roles: ['Admin', 'User', 'Analyst']},
        children: [
            {path: '', pathMatch: 'full', redirectTo: 'accounts'},
            {path: 'accounts', component: AccountsTableComponent, data: {roles: ['Admin', 'User', 'Analyst']}},
            {path: 'transactions', component: TransactionCreateComponent, data: {roles: ['Admin', 'User', 'Analyst']}},
            {path: 'users', component: AdminUsersComponent, canActivate: [authorizationGuard], data: {roles: ['Admin']}},
            {
                path: 'admin',
                component: AdminPageComponent,
                canActivate: [authorizationGuard],
                data: {roles: ['Admin']},
                children: [
                    {path: '', pathMatch: 'full', redirectTo: 'accounts'},
                    {path: 'accounts', component: AdminAccountsComponent, data: {roles: ['Admin']}},
                    {
                        path: 'transactions',
                        component: AdminPagePanelComponent,
                        data: {
                            roles: ['Admin']

                        }
                    },
                    {path: 'users', component: AdminUsersComponent, data: {roles: ['Admin']}},
                ]
            },
            {
                path: 'analyst',
                component: AnalystPageComponent,
                canActivate: [authorizationGuard],
                data: {roles: ['Admin', 'Analyst']},
                children: [
                    {path: '', pathMatch: 'full', redirectTo: 'accounts'},
                    {path: 'accounts', component: AnalystAccountsComponent, data: {roles: ['Admin', 'Analyst']}},
                    {path: 'transactions', component: AnalystPagePanelComponent, data: {roles: ['Admin', 'Analyst']}},
                    {path: 'users', component: AnalystUsersComponent, data: {roles: ['Admin', 'Analyst']}},
                ]
            },
        ]
    },
    {path: 'accounts', redirectTo: 'dashboard/accounts', pathMatch: 'full'},
];

export const ROUTING: ModuleWithProviders<any> = RouterModule.forRoot(ROUTES);
