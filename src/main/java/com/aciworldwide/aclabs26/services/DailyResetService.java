package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyResetService {

    private static final Logger logger = LogManager.getLogger(DailyResetService.class);

    private final AccountRepository accountRepository;

    public DailyResetService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Scheduled(cron = "0 0 0 * * *",zone = "Europe/Paris")//s m h
    @Transactional
    public void resetDailyCounters() {
        logger.info("Resetting daily transaction counters for all accounts");
        for (AccountModel account : accountRepository.findAll()) {
            account.setDailyTx(0);
            account.setDailyTxSum(0.0);
            accountRepository.save(account);
        }
    }
}
