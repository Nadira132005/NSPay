package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface InvestmentWalletRepository extends JpaRepository<InvestmentWallet,Long> {@org.springframework.transaction.annotation.Transactional @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE) Optional<InvestmentWallet> findByUserId(long id);}
