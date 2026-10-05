package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
public interface PortfolioPerformanceRepository extends JpaRepository<PortfolioPerformance,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PortfolioPerformance> findByUserIdAndDay(long userId,LocalDate day);
    List<PortfolioPerformance> findByUserIdAndDayGreaterThanEqualOrderByDayAsc(long userId,LocalDate since);
}
