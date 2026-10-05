package com.nspay.services;

import com.nspay.entities.AccountModel;
import com.nspay.repository.AccountRepository;
import com.nspay.finance.DailyPaymentUsage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyResetService {
    private final AccountRepository accounts;private final DailyPaymentUsage usage;
    @org.springframework.beans.factory.annotation.Autowired public DailyResetService(AccountRepository accounts,DailyPaymentUsage usage){this.accounts=accounts;this.usage=usage;}
    /** Compatibility operation. Usage automatically rolls over in ledger queries, even after downtime. */
    @Transactional public void resetDailyCounters(){for(AccountModel a:accounts.findAll()){AccountModel locked=accounts.lockById(a.getId());usage.sync(locked);}}
}
