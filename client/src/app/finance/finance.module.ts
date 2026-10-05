import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { PriceChartComponent } from './price-chart.component';
import { InvestmentsComponent } from './investments.component';
import { AdminConsoleComponent } from './admin-console.component';
import { SecurityPanelComponent } from './security-panel.component';
import { FinanceComponent } from './finance.component';
import { StatusBadgeComponent } from './status-badge.component';
import { CreditReviewComponent } from './credit-review.component';
import { AccountLimitsComponent } from './account-limits.component';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';

@NgModule({
  declarations: [FinanceComponent, InvestmentsComponent, PriceChartComponent, AdminConsoleComponent, SecurityPanelComponent, StatusBadgeComponent, CreditReviewComponent, AccountLimitsComponent],
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatStepperModule, MatProgressBarModule, MatDialogModule, MatSnackBarModule, MatDatepickerModule, MatNativeDateModule, MatPaginatorModule, MatSortModule,
    RouterModule.forChild([{ path: '', component: FinanceComponent }])]
})
export class FinanceModule {}
