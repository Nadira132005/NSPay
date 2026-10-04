package com.nspay.finance;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix="app.schedules",name="enabled",havingValue="true",matchIfMissing=true)
@Component public class SuspensionScheduler {
    private final AccountRestrictionRepository restrictions;private final SecurityService security;
    public SuspensionScheduler(AccountRestrictionRepository r,SecurityService s){restrictions=r;security=s;}
    @Scheduled(fixedDelay=30000) public void expire(){for(AccountRestriction r:restrictions.findBySuspendedUntilBefore(Instant.now()))security.expire(r.getUserId());}
}
