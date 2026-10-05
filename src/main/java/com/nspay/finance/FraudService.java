package com.nspay.finance;
import com.nspay.repository.*;
import com.nspay.entities.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@Service
public class FraudService {
    @org.springframework.beans.factory.annotation.Autowired private DailyPaymentUsage usage;
    private final FraudScoringEngine engine;
    private final RiskAssessmentRepository assessments;
    private final TransactionRepository transactions;
    private final UserRepository users;
    private final AccountRepository accounts;
    public FraudService(FraudScoringEngine engine, RiskAssessmentRepository assessments,
                        TransactionRepository transactions, UserRepository users, AccountRepository accounts) {
        this.engine=engine; this.assessments=assessments; this.transactions=transactions; this.users=users; this.accounts=accounts;
    }
    @Transactional
    public RiskAssessment assess(long userId, String recipient, BigDecimal amount) {
        return assessments.save(preview(userId,recipient,amount));
    }
    @Transactional(readOnly=true)
    public RiskAssessment preview(long userId, String recipient, BigDecimal amount) {
        UserModel user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        AccountModel account = user.getAccountId()==null ? null : accounts.findById(user.getAccountId()).orElse(null);
        if (account==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No account is assigned");
        RiskAssessment result=engine.assess(userId,recipient,amount,transactions.findAllByCardNumber(account.getCardNumber()));
        DailyPaymentUsage.Usage current=usage.current(account);
        DailyPaymentUsage.Usage projected=DailyPaymentUsage.values(current.getDailyTx()+1,current.getDailyTxSum()+amount.doubleValue(),account.getDailyTxLimit(),account.getDailyTxSumLimit());
        if(projected.getLimitScore()>0){result.setScore(Math.min(100,result.getScore()+(int)Math.round(projected.getLimitScore()*.25)));result.setLevel(result.getScore()>=60?"HIGH":result.getScore()>=35?"MEDIUM":"LOW");result.setReasons(result.getReasons()+"; Daily payment usage "+projected.getLevel().toLowerCase(java.util.Locale.ROOT));}return result;
    }
}
