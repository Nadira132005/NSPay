package com.nspay.finance;
import com.nspay.entities.*;
import com.nspay.enums.UserRole;
import com.nspay.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
@Service
public class SecurityService {
    @javax.persistence.PersistenceContext private javax.persistence.EntityManager entityManager;
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AccountRestrictionRepository restrictions;
    private final SecurityReportRepository reports;
    private final StatusChangeRepository history;
    private final CustomerProfileRepository profiles;
    private final PaymentSignalService scoring;private final PaymentSignalRepository signals;
    private final com.nspay.services.AuditService audit;
    public SecurityService(UserRepository u,AccountRepository a,TransactionRepository t,AccountRestrictionRepository r,SecurityReportRepository s,StatusChangeRepository h,CustomerProfileRepository profiles,PaymentSignalService scoring,PaymentSignalRepository signals,com.nspay.services.AuditService audit) {
        users=u;accounts=a;transactions=t;restrictions=r;reports=s;history=h;
        this.profiles=profiles;
        this.scoring=scoring;this.signals=signals;this.audit=audit;
    }
    public UserModel user(long id) { return users.findById(id).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"User not found")); }
    public void admin(long id) { if(user(id).getRole()!=UserRole.Admin) throw fail(HttpStatus.FORBIDDEN,"Administrator access required"); }
    private AccountModel lock(long id) {
        Long accountId=user(id).getAccountId();
        if(accountId==null) throw fail(HttpStatus.CONFLICT,"User has no account");
        AccountModel account=accounts.lockById(accountId);
        if(account==null) throw fail(HttpStatus.NOT_FOUND,"Account not found");
        return account;
    }
    private AccountRestriction state(long id) {
        return restrictions.findByUserId(id).orElseGet(() -> { AccountRestriction r=new AccountRestriction();r.setUserId(id);return r; });
    }
    @Transactional
    public AccountRestriction setStatus(long actor,long subject,AccountStatus status,Integer days,String reason) {
        admin(actor);return transition(actor,subject,lock(subject),status,days,reason);
    }
    @Transactional
    public AccountRestriction setStatus(long actor,long subject,AccountStatus status,Integer days,String reason,String publicNote,String privateNote){
        admin(actor);validateNotes(publicNote,privateNote);return transition(actor,subject,lock(subject),status,days,reason,false,publicNote,privateNote,null,null);
    }
    private AccountRestriction transition(long actor,long subject,AccountModel account,AccountStatus status,Integer days,String reason) {
        return transition(actor,subject,account,status,days,reason,false);
    }
    private AccountRestriction transition(long actor,long subject,AccountModel account,AccountStatus status,Integer days,String reason,boolean newCase) {
        return transition(actor,subject,account,status,days,reason,newCase,null,null,null,null);
    }
    private AccountRestriction transition(long actor,long subject,AccountModel account,AccountStatus status,Integer days,String reason,boolean newCase,String publicNote,String privateNote,Integer score,Long transactionId) {
        if(reason==null || reason.trim().length()<5 || reason.length()>250) throw fail(HttpStatus.BAD_REQUEST,"Provide a reason between 5 and 250 characters");
        if(status==null) throw fail(HttpStatus.BAD_REQUEST,"Choose a status");
        if(status==AccountStatus.SUSPENDED && (days==null || days<30 || days>90)) throw fail(HttpStatus.BAD_REQUEST,"Suspensions must last 30 to 90 days");
        AccountRestriction r=state(subject);
        if(account.getStatus()==status && !newCase) return r;
        AccountStatus previous=account.getStatus();
        r.setSuspendedUntil(null);
        if(status==AccountStatus.SUSPENDED) {
            r.setSuspensionCount(r.getSuspensionCount()+1);
            if(r.getSuspensionCount()>=2) status=AccountStatus.BLOCKED;
            else r.setSuspendedUntil(Instant.now().plus(Duration.ofDays(days)));
        }
        r.setReason(reason.trim());account.setStatus(status);accounts.save(account);restrictions.save(r);
        StatusChange h=new StatusChange();h.setUserId(subject);h.setActorId(actor);h.setPreviousStatus(previous.name());h.setNewStatus(status.name());h.setReason(reason.trim());h.setSuspensionCount(r.getSuspensionCount());h.setSuspendedUntil(r.getSuspendedUntil());h.setPublicNote(clean(publicNote));h.setPrivateNote(clean(privateNote));h.setMisalignmentScore(score);h.setTransactionId(transactionId);history.save(h);
        audit.record(actor==0?"system":user(actor).getName(),"SECURITY_STATUS: user="+subject+", status="+status+", transaction="+transactionId);
        return r;
    }
    @Transactional
    public SecurityReport report(long actor,long transactionId,String reason) {
        if(reason==null || reason.trim().length()<5 || reason.length()>250) throw fail(HttpStatus.BAD_REQUEST,"Provide a reason between 5 and 250 characters");
        TransactionModel tx=transactions.findById(transactionId).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"Transaction not found"));
        UserModel reporter=user(actor);
        AccountModel own=reporter.getAccountId()==null?null:accounts.findById(reporter.getAccountId()).orElse(null);
        if(reporter.getRole()!=UserRole.User)throw fail(HttpStatus.FORBIDDEN,"Customer reports are for regular users; administrators use the payment review queue");
        if(own==null || !Objects.equals(own.getCardNumber(),tx.getCardNumber())) throw fail(HttpStatus.NOT_FOUND,"Transaction not found");
        AccountModel target=accounts.findByCardNumber(tx.getCardNumber());
        if(target==null) throw fail(HttpStatus.NOT_FOUND,"Transaction account not found");
        UserModel subject=users.findByAccountId(target.getId()).orElseThrow(() -> fail(HttpStatus.CONFLICT,"Account has no user"));
        AccountModel locked=lock(subject.getId());
        Optional<SecurityReport> existing=reports.findByTransactionId(transactionId);
        if(existing.isPresent()) return existing.get();
        SecurityReport report=new SecurityReport();report.setUserId(subject.getId());report.setReporterId(actor);report.setTransactionId(transactionId);report.setReason(reason.trim());
        PaymentSignal prior=scoring.capture(tx);
        if(prior!=null&&!"OPEN".equals(prior.getReviewStatus())){report.setStatus("DISMISSED".equals(prior.getReviewStatus())?"CLEARED":"CONFIRMED");report.setReviewedBy(prior.getReviewedBy());report.setReviewedAt(prior.getReviewedAt());report.setPublicNote(prior.getPublicNote());report.setPrivateNote(prior.getPrivateNote());}
        SecurityReport saved=reports.saveAndFlush(report);scoring.reported(tx);
        audit.record(reporter.getName(),"SECURITY_REPORT_SUBMITTED: transaction="+transactionId);return saved;
    }
    @Transactional
    public SecurityReport review(long actor,long reportId,boolean confirmed,Integer days,String note) {
        admin(actor);SecurityReport report=reports.findById(reportId).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"Report not found"));
        reviewPayment(actor,report.getTransactionId(),confirmed?(days==null?AccountStatus.FREEZE:AccountStatus.SUSPENDED):null,days,note,null,null);
        return reports.findById(reportId).orElseThrow();
    }
    @Transactional
    public PaymentSignal reviewPayment(long actor,long transactionId,AccountStatus decision,Integer days,String reason,String publicNote,String privateNote){
        admin(actor);validateNotes(publicNote,privateNote);
        if(reason==null||reason.trim().length()<5||reason.length()>250)throw fail(HttpStatus.BAD_REQUEST,"Provide a decision reason between 5 and 250 characters");
        if(decision==AccountStatus.ACTIVE)throw fail(HttpStatus.BAD_REQUEST,"Dismiss this payment or choose a restriction; restore access from Administration");
        TransactionModel tx=transactions.findById(transactionId).orElseThrow(()->fail(HttpStatus.NOT_FOUND,"Transaction not found"));
        PaymentSignal captured=scoring.capture(tx);if(captured==null)throw fail(HttpStatus.NOT_FOUND,"Payment account not found");
        AccountModel account=lock(captured.getUserId());
        PaymentSignal signal=signals.lockByTransactionId(transactionId).orElseThrow();
        if(!"OPEN".equals(signal.getReviewStatus()))throw fail(HttpStatus.CONFLICT,"This payment has already been reviewed");
        // A new case may advance suspension counts, but cannot weaken an existing restriction.
        AccountStatus applied=decision;
        if(decision!=null){
            if(account.getStatus()==AccountStatus.BLOCKED || (account.getStatus()==AccountStatus.SUSPENDED&&decision==AccountStatus.FREEZE))applied=account.getStatus();
            else {transition(actor,signal.getUserId(),account,decision,days,reason,decision==AccountStatus.SUSPENDED,publicNote,privateNote,signal.getMisalignmentScore(),transactionId);applied=account.getStatus();}
        }
        signal.setDecisionScore(signal.getMisalignmentScore());signal.setReviewStatus(applied==null?"DISMISSED":applied.name());signal.setReviewedBy(actor);signal.setReviewedAt(Instant.now());signal.setPublicNote(clean(publicNote));signal.setPrivateNote(clean(privateNote));
        Optional<SecurityReport> existing=reports.findByTransactionId(transactionId);
        if(existing.isPresent()){SecurityReport report=existing.get();report.setStatus(decision==null?"CLEARED":"CONFIRMED");report.setDecisionNote(reason.trim());report.setPublicNote(clean(publicNote));report.setPrivateNote(clean(privateNote));report.setReviewedBy(actor);report.setReviewedAt(signal.getReviewedAt());reports.saveAndFlush(report);}
        if(decision==null)scoring.dismissed(signal);
        audit.record(user(actor).getName(),"PAYMENT_REVIEW: transaction="+transactionId+", decision="+signal.getReviewStatus());return signals.save(signal);
    }
    @Transactional
    public void expire(long subject) {
        AccountModel account=lock(subject);AccountRestriction r=state(subject);
        if(account.getStatus()==AccountStatus.SUSPENDED && r.getSuspendedUntil()!=null && !r.getSuspendedUntil().isAfter(Instant.now())) {
            transition(0,subject,account,AccountStatus.ACTIVE,null,"Suspension period ended");
        }
    }
    public Map<String,Object> own(long id) { if(user(id).getRole()!=UserRole.User)throw fail(HttpStatus.FORBIDDEN,"Customer security activity is for regular users");Map<String,Object> result=new LinkedHashMap<>();result.put("restriction",state(id));result.put("reports",reports.findByUserIdOrderByCreatedAtDesc(id));result.put("history",history.findByUserIdOrderByCreatedAtDesc(id));result.put("activity",activity(id));return result; }
    public List<Map<String,Object>> activity(long viewer){
        List<Map<String,Object>> result=new ArrayList<>();
        for(StatusChange h:history.findTop50ByActorIdGreaterThanOrderByCreatedAtDesc(0)){
            if(users.findById(h.getActorId()).map(UserModel::getRole).orElse(null)!=UserRole.Admin)continue;
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",h.getId());row.put("username",users.findById(h.getUserId()).map(UserModel::getName).orElse("Closed account"));row.put("status",h.getNewStatus());row.put("createdAt",h.getCreatedAt());row.put("misalignmentScore",h.getMisalignmentScore());row.put("publicNote",h.getPublicNote());
            if(h.getUserId()==viewer){row.put("privateNote",h.getPrivateNote());row.put("reason",h.getReason());row.put("transactionId",h.getTransactionId());}result.add(row);
        }return result;
    }
    private void validateNotes(String publicNote,String privateNote){if((publicNote!=null&&publicNote.length()>250)||(privateNote!=null&&privateNote.length()>250))throw fail(HttpStatus.BAD_REQUEST,"Notes must be at most 250 characters");}
    private String clean(String note){return note==null||note.isBlank()?null:note.trim();}
    public List<Map<String,Object>> directory(long actor) {
        admin(actor);List<Map<String,Object>> result=new ArrayList<>();
        users.findAll().forEach(u -> { Map<String,Object> row=new LinkedHashMap<>();row.put("id",u.getId());row.put("username",u.getName());row.put("name",u.getFirstName()+" "+u.getLastName());row.put("role",u.getRole());row.put("restriction",state(u.getId()));
            row.put("onboardingStatus",profiles.findByUserId(u.getId()).map(CustomerProfile::getOnboardingStatus).orElse("NOT_STARTED"));
            if(u.getAccountId()!=null) accounts.findById(u.getAccountId()).ifPresent(a -> {row.put("status",a.getStatus());row.put("balance",a.getAmount());row.put("iban",a.getIban());String card=a.getCardNumber();row.put("card","•••• •••• •••• "+card.substring(Math.max(0,card.length()-4)));});result.add(row); });return result;
    }
    public List<SecurityReport> queue(long actor) {admin(actor);return reports.findTop100ByOrderByCreatedAtDesc();}
    private static ResponseStatusException fail(HttpStatus s,String m) {return new ResponseStatusException(s,m);}
}
