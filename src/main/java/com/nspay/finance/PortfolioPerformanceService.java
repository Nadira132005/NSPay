package com.nspay.finance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nspay.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
@Service
public class PortfolioPerformanceService {
    private final PortfolioPerformanceRepository history;private final FinanceService finance;private final AccountRepository accounts;private final InvestmentWalletRepository wallets;
    public PortfolioPerformanceService(PortfolioPerformanceRepository h,FinanceService f,AccountRepository a,InvestmentWalletRepository w){history=h;finance=f;accounts=a;wallets=w;}
    @Transactional public void record(long userId,long walletVersion,Map<String,Object> view){
        if(!"COMPLETE".equals(view.get("valuationStatus")))return;
        accounts.lockById(finance.account(userId,false).getId());
        InvestmentWallet wallet=wallets.findByUserId(userId).orElse(null);
        if(wallet==null||wallet.getVersion()!=walletVersion)return; // Do not publish an outdated valuation after a concurrent order.
        LocalDate day=LocalDate.now(ZoneOffset.UTC);PortfolioPerformance p=history.findByUserIdAndDay(userId,day).orElseGet(PortfolioPerformance::new);
        p.setUserId(userId);p.setDay(day);p.setWalletVersion(walletVersion);p.setTotalGain((BigDecimal)view.get("totalGain"));p.setRealizedGain((BigDecimal)view.get("realizedGain"));p.setUnrealizedGain((BigDecimal)view.get("unrealizedGain"));p.setHoldingsValue((BigDecimal)view.get("holdingsValue"));history.save(p);
    }
    public List<PortfolioPerformance> history(long userId,String range){
        finance.account(userId,false);int days;
        switch(range){case "1M":days=30;break;case "3M":days=90;break;case "1Y":days=365;break;default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose 1M, 3M or 1Y");}
        return history.findByUserIdAndDayGreaterThanEqualOrderByDayAsc(userId,LocalDate.now(ZoneOffset.UTC).minusDays(days));
    }
}
