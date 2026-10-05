package com.nspay.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition,Long> {
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<InvestmentPosition> findByUserIdOrderBySymbolAsc(long userId);
}
