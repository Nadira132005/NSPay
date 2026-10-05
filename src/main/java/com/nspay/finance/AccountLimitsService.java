package com.nspay.finance;

import com.nspay.entities.*;
import com.nspay.repository.*;
import com.nspay.services.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;

@Service
public class AccountLimitsService {
    private final AccountRepository accounts;private final UserRepository users;private final DailyPaymentUsage usage;private final PaymentSignalService signals;private final AuditService audit;
    public AccountLimitsService(AccountRepository accounts,UserRepository users,DailyPaymentUsage usage,PaymentSignalService signals,AuditService audit){this.accounts=accounts;this.users=users;this.usage=usage;this.signals=signals;this.audit=audit;}
    @io.swagger.v3.oas.annotations.media.Schema(name="AccountDailyLimits")
    @Data public static class Limits {
        @NotNull @Min(1) @Max(1000000) private Integer dailyTxLimit;
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal dailyTxSumLimit;
    }
    @Transactional(readOnly=true) public Map<String,Object> list(long actor,int page,int size,String search,String status,String sort,String direction){
        signals.admin(actor);if(page<0||size<1||size>100||!Set.of("id","cardHolderName","amount","dailyTxLimit","dailyTxSumLimit").contains(sort)||!Set.of("asc","desc").contains(direction)||!Set.of("All","ACTIVE","FREEZE","SUSPENDED","BLOCKED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid account filter or sort");
        Specification<AccountModel> filter=(r,q,cb)->{var p=new ArrayList<javax.persistence.criteria.Predicate>();if(!status.equals("All"))p.add(cb.equal(r.get("status"),AccountStatus.valueOf(status)));if(search!=null&&!search.isBlank()){String text="%"+search.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";p.add(cb.or(cb.like(cb.lower(r.get("cardHolderName")),text,'\\'),cb.like(cb.lower(r.get("iban")),text,'\\')));}return cb.and(p.toArray(new javax.persistence.criteria.Predicate[0]));};
        Page<AccountModel> result=accounts.findAll(filter,PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(direction),sort).and(Sort.by("id"))));List<Map<String,Object>> rows=new ArrayList<>();for(AccountModel a:result)rows.add(view(a));Map<String,Object> response=new LinkedHashMap<>();response.put("items",rows);response.put("total",result.getTotalElements());response.put("page",page);response.put("size",size);return response;
    }
    @Transactional public Map<String,Object> update(long actor,long accountId,Limits request){
        signals.admin(actor);if(request.getDailyTxLimit()==null||request.getDailyTxLimit()<1||request.getDailyTxLimit()>1000000||request.getDailyTxSumLimit()==null||request.getDailyTxSumLimit().signum()<=0||request.getDailyTxSumLimit().scale()>2||request.getDailyTxSumLimit().precision()-request.getDailyTxSumLimit().scale()>10)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a positive whole payment count and EUR amount with at most two decimal places");AccountModel a=accounts.lockById(accountId);if(a==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account was not found");
        int oldCount=a.getDailyTxLimit();double oldSum=a.getDailyTxSumLimit();a.setDailyTxLimit(request.getDailyTxLimit());a.setDailyTxSumLimit(request.getDailyTxSumLimit().doubleValue());usage.sync(a);accounts.save(a);
        users.findByAccountId(accountId).ifPresent(owner->signals.refreshLimits(owner.getId(),a));
        audit.record(users.findById(actor).orElseThrow().getName(),"ACCOUNT_LIMITS: #"+accountId+" count "+oldCount+" -> "+a.getDailyTxLimit()+"; EUR "+oldSum+" -> "+a.getDailyTxSumLimit());return view(a);
    }
    private Map<String,Object> view(AccountModel a){Map<String,Object> r=new LinkedHashMap<>();r.put("id",a.getId());r.put("name",a.getCardHolderName());r.put("iban",a.getIban());String card=a.getCardNumber();r.put("card","•••• "+card.substring(Math.max(0,card.length()-4)));r.put("status",a.getStatus());r.put("balance",a.getAmount());r.put("usage",usage.current(a));users.findByAccountId(a.getId()).ifPresent(u->{r.put("userId",u.getId());r.put("username",u.getName());});return r;}
}
