import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
//Components
import { LoginComponent } from './login/login.component';
import {AccountsTableComponent} from "./accounts-table/accounts-table.component";

export const ROUTES: Routes = [
    {path: 'accounts', component: AccountsTableComponent},
    {path: '', component: LoginComponent},
    {path: 'login', component: LoginComponent},
];

export const ROUTING: ModuleWithProviders<any> = RouterModule.forRoot(ROUTES);
