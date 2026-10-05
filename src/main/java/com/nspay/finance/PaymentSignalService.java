package com.nspay.finance;

import com.nspay.entities.*;
import com.nspay.enums.UserRole;
import com.nspay.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** Bounded, durable per-payment scores. No automatic adverse decisions. */
@Service
public class PaymentSignalService {
    @org.springframework.beans.factory.annotation.Autowired private DailyPaymentUsage usage;
    private final TransactionRepository transactions;private final AccountRepository accounts;private final UserRepository users;
    private final SecurityReportRepository reports;private final PaymentSignalRepository signals;private final TransactionTemplate template;
    public PaymentSignalService(TransactionRepository transactions,AccountRepository accounts,UserRepository users,SecurityReportRepository reports,PaymentSignalRepository signals,PlatformTransactionManager manager){
        this.transactions=transactions;this.accounts=accounts;this.users=users;this.reports=reports;this.signals=signals;this.template=new TransactionTemplate(manager);this.template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public void admin(long actor){if(users.findById(actor).map(UserModel::getRole).orElse(null)!=UserRole.Admin)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator access required");}
    public void backfill(){for(TransactionModel tx:transactions.unscored(PageRequest.of(0,200)))template.executeWithoutResult(status->captureInternal(tx));}
    @Transactional
    public PaymentSignal capture(TransactionModel tx){return captureInternal(tx);}
    private PaymentSignal captureInternal(TransactionModel tx){
        AccountModel account=accounts.findByCardNumber(tx.getCardNumber());
        if(account==null)return null;
        UserModel owner=users.findByAccountId(account.getId()).orElse(null);if(owner==null)return null;
        accounts.lockById(account.getId());
        Optional<PaymentSignal> existing=signals.findByTransactionId(tx.getId());if(existing.isPresent())return existing.get();
        PaymentSignal signal=new PaymentSignal();signal.setUserId(owner.getId());signal.setUsername(owner.getName());signal.setTransactionId(tx.getId());
        signal.setAmount(BigDecimal.valueOf(tx.getAmount()==null?0:tx.getAmount()));signal.setPaidAt(tx.getTimestamp()==null?Instant.now():tx.getTimestamp().toInstant());signal.setTransactionType(tx.getTransactionType()==null||"PAYMENT".equals(tx.getTransactionType())?"CARD_PAYMENT":tx.getTransactionType());signal.setRecipientIban(tx.getCounterpartyIban());
        List<TransactionModel> past=transactions.scoreHistory(tx.getCardNumber(),java.sql.Timestamp.from(signal.getPaidAt()),PageRequest.of(0,50));signal.setHistorySamples(past.size());
        double[] amounts=past.stream().mapToDouble(t->t.getAmount()==null?0:t.getAmount()).filter(a->a>0).sorted().toArray();
        double median=amounts.length==0?0:amounts[amounts.length/2];
        signal.setAmountScore(median<=0?0:clamp((signal.getAmount().doubleValue()/median-1)*25));
        int hour=signal.getPaidAt().atZone(ZoneOffset.UTC).getHour();
        long sameHour=past.stream().filter(t->t.getTimestamp().toInstant().atZone(ZoneOffset.UTC).getHour()==hour).count();
        signal.setTimingScore(past.size()<5?(hour<5?25:0):clamp(100*(1-(double)sameHour/past.size())*0.5));
        signal.setVelocityScore(clamp(past.stream().filter(t->!t.getTimestamp().toInstant().isBefore(signal.getPaidAt().minusSeconds(3600))).count()*20));
        signal.setRecipientScore(signal.getRecipientIban()==null?0:past.stream().anyMatch(t->Objects.equals(t.getCounterpartyIban(),signal.getRecipientIban()))?0:50);
        Optional<SecurityReport> report=reports.findByTransactionId(tx.getId());signal.setReported(report.isPresent());
        if(report.isPresent()&&!"OPEN".equals(report.get().getStatus())){SecurityReport prior=report.get();signal.setReviewStatus("CLEARED".equals(prior.getStatus())?"DISMISSED":"SUSPENDED");signal.setReviewedBy(prior.getReviewedBy());signal.setReviewedAt(prior.getReviewedAt());signal.setPublicNote(prior.getPublicNote());signal.setPrivateNote(prior.getPrivateNote());}
        int count=signal.getRecipientIban()==null?0:(int)reports.recipientReporters(signal.getRecipientIban());signal.setReportCount(count);
        applyUsage(signal,usage.through(account,signal.getPaidAt(),tx.getId()));
        signal.setReportingScore(Math.max(signal.isReported()&&!"DISMISSED".equals(signal.getReviewStatus())?60:0,Math.min(100,count*40)));signal.setMisalignmentScore(combined(signal));return signals.saveAndFlush(signal);
    }
    @Transactional
    public void reported(TransactionModel tx){
        PaymentSignal signal=captureInternal(tx);if(signal==null)return;
        signal.setReported(true);if(!"DISMISSED".equals(signal.getReviewStatus()))signal.setReportingScore(Math.max(60,signal.getReportingScore()));signal.setMisalignmentScore(combined(signal));signals.saveAndFlush(signal);
        refreshRecipientAfterCommit(signal.getRecipientIban());
    }
    @Transactional
    public void dismissed(PaymentSignal signal){
        signal.setReportingScore(0);signal.setMisalignmentScore(combined(signal));signals.saveAndFlush(signal);
        refreshRecipientAfterCommit(signal.getRecipientIban());
    }
    private void refreshRecipientAfterCommit(String iban){
        if(iban==null)return;
        // Release the account/signal locks before updating related customers' scores.
        // Otherwise simultaneous reports on the same recipient can lock each other's rows.
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){template.executeWithoutResult(status->{int count=(int)reports.recipientReporters(iban);signals.updateRecipientReports(iban,count,Math.min(100,count*40));});}});
    }
    public Map<String,Object> queue(long actor,int page,int size,String search,String state,String reported,String sort,String direction,int minScore,String metric,String type){
        admin(actor);
        if(page<0||size<1||size>100||minScore<0||minScore>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination or score range");
        Set<String> metrics=Set.of("misalignmentScore","amountScore","timingScore","velocityScore","recipientScore","reportingScore","limitScore");
        if(!metrics.contains(metric)||(!metrics.contains(sort)&&!Set.of("paidAt","amount","username","reportCount").contains(sort))||!Set.of("asc","desc").contains(direction)||!Set.of("all","reported","unreported").contains(reported)||!Set.of("All","OPEN","DISMISSED","FREEZE","SUSPENDED","BLOCKED").contains(state))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid review filter or sort");
        backfill();migrateLimits();
        Specification<PaymentSignal> filter=(root,query,cb)->{
            List<javax.persistence.criteria.Predicate> predicates=new ArrayList<>();
            predicates.add(cb.ge(root.get(metric),minScore));
            if(!"All".equals(state))predicates.add(cb.equal(root.get("reviewStatus"),state));
            if(!"all".equals(reported))predicates.add(cb.equal(root.get("reported"),"reported".equals(reported)));
            if(type!=null&&!type.isBlank())predicates.add(cb.equal(root.get("transactionType"),type));
            if(search!=null&&!search.isBlank()){
                String text="%"+search.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("username")),text,'\\'),cb.like(cb.lower(root.get("recipientIban")),text,'\\')));
            }
            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
        Page<PaymentSignal> result=signals.findAll(filter,PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(direction),sort).and(Sort.by("id"))));
        Map<String,Object> view=new LinkedHashMap<>();view.put("items",result.getContent());view.put("total",result.getTotalElements());view.put("page",page);view.put("size",size);view.put("indexing",!transactions.unscored(PageRequest.of(0,1)).isEmpty()||!signals.findByModelVersion("payment-signals-v1",PageRequest.of(0,1)).isEmpty());return view;
    }
    @Transactional
    public Map<String,Object> evidence(long actor,long transactionId){
        admin(actor);TransactionModel tx=transactions.findById(transactionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment was not found"));
        PaymentSignal signal=captureInternal(tx);if(signal==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment account was not found");
        List<Map<String,Object>> rows=new ArrayList<>();for(SecurityReport report:reports.evidence(transactionId,signal.getRecipientIban(),PageRequest.of(0,50))){Map<String,Object> row=new LinkedHashMap<>();row.put("transactionId",report.getTransactionId());row.put("reporter",users.findById(report.getReporterId()).map(UserModel::getName).orElse("Closed account"));row.put("reason",report.getReason());row.put("status",report.getStatus());row.put("createdAt",report.getCreatedAt());rows.add(row);}
        Map<String,Object> result=new LinkedHashMap<>();result.put("payment",signal);result.put("reports",rows);return result;
    }
    private void applyUsage(PaymentSignal signal,DailyPaymentUsage.Usage daily){
        signal.setDailyTx(daily.getDailyTx());signal.setDailyTxSum(daily.getDailyTxSum());signal.setDailyTxLimit(daily.getDailyTxLimit());signal.setDailyTxSumLimit(daily.getDailyTxSumLimit());signal.setLimitScore(daily.getLimitScore());signal.setModelVersion("payment-signals-v2");
    }
    private void migrateLimits(){for(PaymentSignal s:signals.findByModelVersion("payment-signals-v1",PageRequest.of(0,200)))template.executeWithoutResult(status->{AccountModel a=users.findById(s.getUserId()).map(UserModel::getAccountId).map(accounts::lockById).orElse(null);if(a==null)return;PaymentSignal locked=signals.lockByTransactionId(s.getTransactionId()).orElseThrow();if("OPEN".equals(locked.getReviewStatus())){applyUsage(locked,usage.through(a,locked.getPaidAt(),locked.getTransactionId()));locked.setMisalignmentScore(combined(locked));}else locked.setModelVersion("payment-signals-v2");signals.save(locked);});}
    @Transactional public void refreshLimits(long userId,AccountModel account){
        int page=0;org.springframework.data.domain.Slice<PaymentSignal> batch;
        do{batch=signals.findByUserIdAndReviewStatus(userId,"OPEN",PageRequest.of(page++,200,Sort.by("id")));for(PaymentSignal s:batch){applyUsage(s,"payment-signals-v1".equals(s.getModelVersion())?usage.through(account,s.getPaidAt(),s.getTransactionId()):DailyPaymentUsage.values(s.getDailyTx(),s.getDailyTxSum(),account.getDailyTxLimit(),account.getDailyTxSumLimit()));s.setMisalignmentScore(combined(s));}signals.saveAll(batch.getContent());}while(batch.hasNext());
    }
    private int clamp(double score){return (int)Math.max(0,Math.min(100,Math.round(score)));}
    private int combined(PaymentSignal s){return Math.min(100,(int)Math.round(s.getAmountScore()*.35+s.getTimingScore()*.15+s.getVelocityScore()*.15+s.getRecipientScore()*.15+s.getReportingScore()*.20+s.getLimitScore()*.25));}
}
