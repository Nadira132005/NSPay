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
import { InvestmentsComponent } from './investments.component';
import { AdminConsoleComponent } from './admin-console.component';
import { SecurityPanelComponent } from './security-panel.component';
import { FinanceComponent } from './finance.component';

@NgModule({
  declarations: [FinanceComponent, InvestmentsComponent, AdminConsoleComponent, SecurityPanelComponent],
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatStepperModule, MatProgressBarModule, MatDialogModule, MatSnackBarModule,
    RouterModule.forChild([{ path: '', component: FinanceComponent }])]
})
export class FinanceModule {}
