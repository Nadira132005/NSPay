import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
//Components
import { LoginComponent } from './login/login.component';
import {AccountsTableComponent} from "./accounts-table/accounts-table.component";
import {authorizationGuard} from "./authorization.guard";
import {DashboardComponent} from "./dashboard/dashboard.component";
import {AdminUsersComponent} from "./admin/admin-users/admin-users.component";
import {AdminPageComponent} from "./admin/admin-page/admin-page.component";
import {AdminPagePanelComponent} from "./admin/admin-page-panel/admin-page-panel.component";
import {AnalystPageComponent} from "./analyst/analyst-page/analyst-page.component";
import {AnalystAccountsComponent} from "./analyst/analyst-accounts/analyst-accounts.component";
import {AnalystUsersComponent} from "./analyst/analyst-users/analyst-users.component";
import {AnalystPagePanelComponent} from "./analyst/analyst-page-panel/analyst-page-panel.component";
import {AnalystAuditComponent} from "./analyst/analyst-audit/analyst-audit.component";
import {ChangePasswordComponent} from "./change-password/change-password.component";
import {onboardingGuard} from './onboarding.guard';

export const ROUTES: Routes = [
    {path: '', component: LoginComponent},
    {path: 'login', component: LoginComponent},
    {path: 'change-password', component: ChangePasswordComponent},
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [authorizationGuard],
        data: {roles: ['Admin', 'User', 'Analyst']},
        children: [
            {path: '', pathMatch: 'full', redirectTo: 'overview'},
            ...['overview', 'savings', 'payments', 'trading', 'credit', 'security'].map(section => ({path: section, loadChildren: () => import('./finance/finance.module').then(m => m.FinanceModule), data: {section}})),
            {path:'onboarding',loadChildren:() => import('./finance/finance.module').then(m => m.FinanceModule),canActivate:[onboardingGuard],data:{section:'onboarding'}},
            {path: 'accounts', redirectTo: 'overview', pathMatch: 'full'},
            {path: 'deposits', redirectTo: 'savings', pathMatch: 'full'},
            {path: 'transactions', redirectTo: 'payments', pathMatch: 'full'},
            {path: 'users', component: AdminUsersComponent, canActivate: [authorizationGuard], data: {roles: ['Admin']}},
            {path: 'admin', pathMatch: 'full', loadChildren: () => import('./finance/finance.module').then(m => m.FinanceModule), canActivate: [authorizationGuard], data: {roles: ['Admin'], section: 'admin'}},
            {path: 'admin/create-account', redirectTo: 'admin/accounts', pathMatch: 'full'},
            {path: 'admin/accounts', loadChildren: () => import('./finance/finance.module').then(m => m.FinanceModule), canActivate: [authorizationGuard], data: {roles: ['Admin'], section: 'accounts'}},
            {path: 'admin/users', component: AdminUsersComponent, canActivate: [authorizationGuard], data: {roles: ['Admin']}},
            {path: 'admin/transactions', component: AdminPagePanelComponent, canActivate: [authorizationGuard], data: {roles: ['Admin']}},
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
                    {path: 'audit', component: AnalystAuditComponent, data: {roles: ['Admin', 'Analyst']}},
                ]
            },
        ]
    },
    {path: 'accounts', redirectTo: 'dashboard/accounts', pathMatch: 'full'},
];

export const ROUTING: ModuleWithProviders<any> = RouterModule.forRoot(ROUTES);
