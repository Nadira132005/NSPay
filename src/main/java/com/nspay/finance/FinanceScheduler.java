package com.nspay.finance;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Instant;

@Component
@ConditionalOnProperty(name="app.schedules.enabled",havingValue="true",matchIfMissing=true)
public class FinanceScheduler {
    private final ScheduledTransferRepository schedules;
    private final FinanceService finance;
    public FinanceScheduler(ScheduledTransferRepository schedules,FinanceService finance) { this.schedules=schedules; this.finance=finance; }
    @Scheduled(fixedDelayString="${app.schedules.interval-ms:30000}",initialDelayString="${app.schedules.interval-ms:30000}")
    public void executeDue() {
        for (ScheduledTransfer item:schedules.findTop50ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("PENDING",Instant.now())) {
            try { finance.executeSchedule(item.getId()); }
            catch (RuntimeException exception) { finance.failSchedule(item.getId()); }
        }
    }
}
