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
    public SecurityService(UserRepository u,AccountRepository a,TransactionRepository t,AccountRestrictionRepository r,SecurityReportRepository s,StatusChangeRepository h) {
        users=u;accounts=a;transactions=t;restrictions=r;reports=s;history=h;
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
    private AccountRestriction transition(long actor,long subject,AccountModel account,AccountStatus status,Integer days,String reason) {
        return transition(actor,subject,account,status,days,reason,false);
    }
    private AccountRestriction transition(long actor,long subject,AccountModel account,AccountStatus status,Integer days,String reason,boolean newCase) {
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
        StatusChange h=new StatusChange();h.setUserId(subject);h.setActorId(actor);h.setPreviousStatus(previous.name());h.setNewStatus(status.name());h.setReason(reason.trim());h.setSuspensionCount(r.getSuspensionCount());h.setSuspendedUntil(r.getSuspendedUntil());history.save(h);
        return r;
    }
    @Transactional
    public SecurityReport report(long actor,long transactionId,String reason) {
        if(reason==null || reason.trim().length()<5 || reason.length()>250) throw fail(HttpStatus.BAD_REQUEST,"Provide a reason between 5 and 250 characters");
        TransactionModel tx=transactions.findById(transactionId).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"Transaction not found"));
        UserModel reporter=user(actor);
        AccountModel own=reporter.getAccountId()==null?null:accounts.findById(reporter.getAccountId()).orElse(null);
        if(reporter.getRole()!=UserRole.Admin && (own==null || !Objects.equals(own.getCardNumber(),tx.getCardNumber()))) throw fail(HttpStatus.FORBIDDEN,"You can only report transactions on your account");
        AccountModel target=accounts.findByCardNumber(tx.getCardNumber());
        if(target==null) throw fail(HttpStatus.NOT_FOUND,"Transaction account not found");
        UserModel subject=users.findByAccountId(target.getId()).orElseThrow(() -> fail(HttpStatus.CONFLICT,"Account has no user"));
        AccountModel locked=lock(subject.getId());
        Optional<SecurityReport> existing=reports.findByTransactionId(transactionId);
        if(existing.isPresent()) return existing.get();
        SecurityReport report=new SecurityReport();report.setUserId(subject.getId());report.setReporterId(actor);report.setTransactionId(transactionId);report.setReason(reason.trim());
        if(locked.getStatus()==AccountStatus.ACTIVE) transition(actor,subject.getId(),locked,AccountStatus.FREEZE,null,reason);
        return reports.save(report);
    }
    @Transactional
    public SecurityReport review(long actor,long reportId,boolean confirmed,Integer days,String note) {
        admin(actor);SecurityReport report=reports.findById(reportId).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"Report not found"));
        AccountModel account=lock(report.getUserId());
        // Refresh after acquiring the shared account lock: concurrent decisions must see the committed review.
        entityManager.refresh(report,javax.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!"OPEN".equals(report.getStatus())) throw fail(HttpStatus.CONFLICT,"This report has already been reviewed");
        if(confirmed) {
            if(days==null || days<30 || days>90 || note==null || note.trim().length()<5 || note.length()>250) throw fail(HttpStatus.BAD_REQUEST,"Provide a review note and a suspension of 30 to 90 days");
            if(account.getStatus()!=AccountStatus.BLOCKED) transition(actor,report.getUserId(),account,AccountStatus.SUSPENDED,days,note,true);
        }
        else {
            if(note==null || note.trim().length()<5 || note.length()>250) throw fail(HttpStatus.BAD_REQUEST,"Provide a review note between 5 and 250 characters");
            boolean anotherOpen=reports.openReports(report.getUserId()).stream().anyMatch(r -> !r.getId().equals(reportId) && "OPEN".equals(r.getStatus()));
            if(!anotherOpen && account.getStatus()==AccountStatus.FREEZE) transition(actor,report.getUserId(),account,AccountStatus.ACTIVE,null,note);
        }
        report.setStatus(confirmed?"CONFIRMED":"CLEARED");report.setDecisionNote(note);report.setReviewedBy(actor);report.setReviewedAt(Instant.now());return reports.save(report);
    }
    @Transactional
    public void expire(long subject) {
        AccountModel account=lock(subject);AccountRestriction r=state(subject);
        if(account.getStatus()==AccountStatus.SUSPENDED && r.getSuspendedUntil()!=null && !r.getSuspendedUntil().isAfter(Instant.now())) {
            boolean open=reports.openReports(subject).stream().anyMatch(report -> "OPEN".equals(report.getStatus()));
            transition(0,subject,account,open?AccountStatus.FREEZE:AccountStatus.ACTIVE,null,"Suspension period ended");
        }
    }
    public Map<String,Object> own(long id) { Map<String,Object> result=new LinkedHashMap<>();result.put("restriction",state(id));result.put("reports",reports.findByUserIdOrderByCreatedAtDesc(id));result.put("history",history.findByUserIdOrderByCreatedAtDesc(id));return result; }
    public List<Map<String,Object>> directory(long actor) {
        admin(actor);List<Map<String,Object>> result=new ArrayList<>();
        users.findAll().forEach(u -> { Map<String,Object> row=new LinkedHashMap<>();row.put("id",u.getId());row.put("username",u.getName());row.put("name",u.getFirstName()+" "+u.getLastName());row.put("role",u.getRole());row.put("restriction",state(u.getId()));
            if(u.getAccountId()!=null) accounts.findById(u.getAccountId()).ifPresent(a -> {row.put("status",a.getStatus());row.put("balance",a.getAmount());row.put("iban",a.getIban());String card=a.getCardNumber();row.put("card","•••• •••• •••• "+card.substring(Math.max(0,card.length()-4)));});result.add(row); });return result;
    }
    public List<SecurityReport> queue(long actor) {admin(actor);return reports.findTop100ByOrderByCreatedAtDesc();}
    private static ResponseStatusException fail(HttpStatus s,String m) {return new ResponseStatusException(s,m);}
}
