package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.Instant;
public interface AccountRestrictionRepository extends JpaRepository<AccountRestriction,Long> {
    @org.springframework.transaction.annotation.Transactional @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountRestriction> findByUserId(long id);
    List<AccountRestriction> findBySuspendedUntilBefore(Instant now);
}
