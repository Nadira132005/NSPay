import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';
import { FrontEndService } from '../../utils/front-end.service';

@Component({
    selector: 'app-admin-page',
    templateUrl: './admin-page.component.html',
    styleUrls: ['./admin-page.component.scss']
})
export class AdminPageComponent implements OnInit {
    constructor(private frontEndService: FrontEndService) {
    }

    ngOnInit(): void {
        this.trackAudit('PAGE: Admin page');
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
