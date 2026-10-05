package com.nspay.finance;

import com.nspay.entities.AccountModel;
import com.nspay.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.sql.Timestamp;
import lombok.Data;

/** The ledger is authoritative; only successful external outgoing payments consume daily usage. */
@Service
public class DailyPaymentUsage {
    public static final ZoneId ZONE=ZoneId.of("Europe/Bucharest");
    private final TransactionRepository transactions;
    public DailyPaymentUsage(TransactionRepository transactions){this.transactions=transactions;}
    @io.swagger.v3.oas.annotations.media.Schema(name="DailyPaymentUsage",description="Successful card/outgoing-transfer usage for the Europe/Bucharest banking day. Advisory thresholds never reject a payment. Incoming and internal wallet movements do not consume them.")
    @Data public static class Usage {
        private int dailyTx;private double dailyTxSum;private int dailyTxLimit;private double dailyTxSumLimit;
        private double countPercent;private double amountPercent;private int limitScore;private String level;
        private Instant resetsAt;private String timezone=ZONE.getId();
    }
    public Usage current(AccountModel a){return through(a,Instant.now(),Long.MAX_VALUE);}
    public Usage through(AccountModel a,Instant at,long transactionId){
        LocalDate date=at.atZone(ZONE).toLocalDate();
        Object[] raw=transactions.dailyPaymentUsage(a.getCardNumber(),Timestamp.from(date.atStartOfDay(ZONE).toInstant()),Timestamp.from(at),transactionId).get(0);
        Usage u=values(((Number)raw[0]).intValue(),((Number)raw[1]).doubleValue(),a.getDailyTxLimit(),a.getDailyTxSumLimit());
        u.setResetsAt(date.plusDays(1).atStartOfDay(ZONE).toInstant());return u;
    }
    public static Usage values(int count,double sum,int countLimit,double sumLimit){
        Usage u=new Usage();u.setDailyTx(count);u.setDailyTxSum(Math.round(sum*100)/100.0);u.setDailyTxLimit(countLimit);u.setDailyTxSumLimit(sumLimit);
        u.setCountPercent(countLimit<=0?(count>0?200:0):100.0*count/countLimit);u.setAmountPercent(sumLimit<=0?(sum>0?200:0):100.0*sum/sumLimit);
        double percent=Math.max(u.getCountPercent(),u.getAmountPercent());
        u.setLevel(percent>100?"EXCEEDED":percent>=100?"REACHED":percent>=80?"NEAR":"NORMAL");
        u.setLimitScore(percent<80?0:percent<100?(int)Math.round(25+(percent-80)*1.75):(int)Math.min(100,Math.round(60+(percent-100)*2)));
        return u;
    }
    /** Called after recording the ledger entry, while the account lock is held. */
    public void sync(AccountModel a){Usage u=current(a);a.setDailyTx(u.getDailyTx());a.setDailyTxSum(u.getDailyTxSum());}
}
