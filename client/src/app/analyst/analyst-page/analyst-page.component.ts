import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
import { FrontEndService } from '../../utils/front-end.service';

@Component({
    selector: 'app-analyst-page',
    templateUrl: './analyst-page.component.html',
    styleUrls: ['./analyst-page.component.scss']
})
export class AnalystPageComponent implements OnInit {
    constructor(private frontEndService: FrontEndService) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Analyst page');
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
