import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
import { FrontEndService } from '../../utils/front-end.service';
import { TransactionModel, TRANSACTION_STATUS_LABELS, TRANSACTION_SUCCESS_CODE } from '../../models/transaction-model';

@Component({
    selector: 'app-admin-page-panel',
    templateUrl: './admin-page-panel.component.html',
    styleUrls: ['./admin-page-panel.component.scss']
})
export class AdminPagePanelComponent implements OnInit {
    title = 'Transactions';
    transactionsData: TransactionModel[] = [];

    displayedColumns: string[] = ['id', 'amount', 'cardNumber', 'returnCode', 'timestamp'];

    constructor(
        private frontEndService: FrontEndService
    ) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Admin transactions');
        this.loadTransactions();
    }

    getTransactionStatusLabel(returnCode: string | number): string {
        const code = String(returnCode);
        return TRANSACTION_STATUS_LABELS[code] ?? code;
    }

    getTransactionStatusClass(returnCode: string | number): string {
        const code = String(returnCode);
        return code === TRANSACTION_SUCCESS_CODE ? 'status-success' : 'status-error';
    }

    private loadTransactions() {
        this.frontEndService.getTransactions().subscribe({
            next: (data) => {
                this.transactionsData = data;
            }
        });
    }

    private trackAudit(action: string) {
        this.frontEndService.recordAudit(action)
            .pipe(take(1))
            .subscribe({
            error: () => {
                console.warn('Audit tracking failed silently:', action);
            }
        });
    }
}
