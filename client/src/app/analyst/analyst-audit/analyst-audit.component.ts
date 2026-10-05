import { Component, OnInit } from '@angular/core';
import { FrontEndService } from '../../utils/front-end.service';
import { AuditModel } from '../../models/audit.model';

@Component({
    selector: 'app-analyst-audit',
    templateUrl: './analyst-audit.component.html',
    styleUrls: ['./analyst-audit.component.scss']
})
export class AnalystAuditComponent implements OnInit {
    auditEntries: AuditModel[] = [];
    statusMessage = '';
    displayedColumns: string[] = ['id', 'user', 'actiune', 'timestamp'];

    constructor(private frontEndService: FrontEndService) {
    }

    ngOnInit(): void {
        this.loadAuditEntries();
    }

    private loadAuditEntries() {
        this.frontEndService.getAuditEntries().subscribe({
            next: (entries) => {
                this.auditEntries = entries;
            },
            error: () => {
                this.statusMessage = 'Could not load audit entries.';
            }
        });
    }
}
