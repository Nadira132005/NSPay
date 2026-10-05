package com.nspay.finance;

import com.nspay.entities.*;
import com.nspay.enums.UserRole;
import com.nspay.repository.*;
import com.nspay.services.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Account first, then profile: the same locks serialize verification, restrictions and credit eligibility. */
@Service
public class OnboardingService {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final CustomerProfileRepository profiles;
    private final CreditDocumentRepository documents;
    private final AuditService audit;
    public OnboardingService(UserRepository users,AccountRepository accounts,CustomerProfileRepository profiles,CreditDocumentRepository documents,AuditService audit){
        this.users=users;this.accounts=accounts;this.profiles=profiles;this.documents=documents;this.audit=audit;
    }
    private UserModel user(long id){return users.findById(id).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"User was not found"));}
    private AccountModel lockAccount(long id){
        Long accountId=user(id).getAccountId();
        if(accountId==null)throw fail(HttpStatus.CONFLICT,"No account is assigned to this user");
        AccountModel account=accounts.lockById(accountId);
        if(account==null)throw fail(HttpStatus.NOT_FOUND,"Account was not found");
        return account;
    }
    public boolean available(CustomerProfile profile){
        return profile==null||"NOT_STARTED".equals(profile.getOnboardingStatus())||("PROFILE_COMPLETE".equals(profile.getOnboardingStatus())&&!hasIdentity(profile.getUserId()));
    }
    public boolean verified(CustomerProfile profile){return profile!=null&&"VERIFIED".equals(profile.getOnboardingStatus());}
    private boolean hasIdentity(long user){return documents.existsByUserIdAndApplicationIdIsNullAndKind(user,"IDENTITY");}
    @Transactional(readOnly=true)
    public Map<String,Object> own(long id){
        UserModel owner=user(id);
        AccountModel account=owner.getAccountId()==null?null:accounts.findById(owner.getAccountId()).orElse(null);
        if(account==null||account.getStatus()!=AccountStatus.ACTIVE)throw fail(HttpStatus.FORBIDDEN,"Onboarding requires an active account");
        CustomerProfile profile=profiles.findByUserId(id).orElse(null);
        if(!available(profile))throw fail(HttpStatus.CONFLICT,"Onboarding is already submitted and cannot be edited");
        Map<String,Object> state=new LinkedHashMap<>();state.put("profile",profile);state.put("step",profile==null||"NOT_STARTED".equals(profile.getOnboardingStatus())?"DETAILS":"IDENTITY");return state;
    }
    @Transactional
    public CustomerProfile saveDetails(long id,FinanceRequests.Profile request){
        if(lockAccount(id).getStatus()!=AccountStatus.ACTIVE)throw fail(HttpStatus.FORBIDDEN,"Onboarding requires an active account");
        CustomerProfile profile=profiles.lockByUserId(id).orElse(null);
        if(profile!=null&&!"NOT_STARTED".equals(profile.getOnboardingStatus()))throw fail(HttpStatus.CONFLICT,"Personal details have already been saved and are locked");
        if(request.getDateOfBirth().isAfter(LocalDate.now().minusYears(18)))throw fail(HttpStatus.BAD_REQUEST,"Onboarding requires age 18 or above");
        if(profile==null){profile=new CustomerProfile();profile.setUserId(id);}
        profile.setEmail(request.getEmail().trim());profile.setPhone(request.getPhone().trim());profile.setCountry(request.getCountry().trim());
        profile.setDateOfBirth(request.getDateOfBirth());profile.setOccupation(request.getOccupation().trim());profile.setOnboardingStatus("PROFILE_COMPLETE");
        audit.record(user(id).getName(),"ONBOARDING_DETAILS_SUBMITTED");return profiles.save(profile);
    }
    @Transactional
    public CustomerProfile requireIdentityUpload(long id,String kind){
        if(lockAccount(id).getStatus()!=AccountStatus.ACTIVE)throw fail(HttpStatus.FORBIDDEN,"Onboarding requires an active account");
        CustomerProfile profile=profiles.lockByUserId(id).orElseThrow(() -> fail(HttpStatus.CONFLICT,"Save your personal details first"));
        if(!"IDENTITY".equals(kind))throw fail(HttpStatus.BAD_REQUEST,"Onboarding accepts an identity document only");
        if(!"PROFILE_COMPLETE".equals(profile.getOnboardingStatus())||hasIdentity(id))throw fail(HttpStatus.CONFLICT,"The identity document has already been submitted and is locked");
        return profile;
    }
    @Transactional
    public void identitySubmitted(CustomerProfile profile){
        profile.setOnboardingStatus("UNDER_REVIEW");profiles.save(profile);
        audit.record(user(profile.getUserId()).getName(),"ONBOARDING_IDENTITY_SUBMITTED");
    }
    @Transactional
    public void requireCreditEligibility(long id){
        if(user(id).getRole()==UserRole.Admin)throw fail(HttpStatus.FORBIDDEN,"Administrators review customer credit applications");
        if(lockAccount(id).getStatus()!=AccountStatus.ACTIVE)throw fail(HttpStatus.FORBIDDEN,"Credit applications require an active account");
        CustomerProfile profile=profiles.lockByUserId(id).orElse(null);
        if(!verified(profile))throw fail(HttpStatus.FORBIDDEN,"Identity verification is required before applying for credit");
    }
    private void admin(long actor){if(user(actor).getRole()!=UserRole.Admin)throw fail(HttpStatus.FORBIDDEN,"Only administrators can review identity verification");}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> reviews(long actor){
        admin(actor);List<Map<String,Object>> result=new ArrayList<>();
        for(CustomerProfile profile:profiles.findTop100ByOrderByCreatedAtDesc()){
            if(profile.getUserId()!=actor&&users.existsById(profile.getUserId()))result.add(reviewView(profile.getUserId()));
        }return result;
    }
    private Map<String,Object> reviewView(long subject){
        UserModel owner=user(subject);Map<String,Object> row=new LinkedHashMap<>();row.put("userId",owner.getId());row.put("username",owner.getName());
        row.put("profile",profiles.findByUserId(subject).orElse(null));row.put("documents",documents.findByUserIdAndApplicationIdIsNullOrderByCreatedAtAsc(subject));return row;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> inspect(long actor,long subject){admin(actor);noSelfReview(actor,subject);return reviewView(subject);}
    @Transactional
    public CustomerProfile review(long actor,long subject,FinanceRequests.OnboardingDecision decision){
        admin(actor);noSelfReview(actor,subject);lockAccount(subject);
        CustomerProfile profile=profiles.lockByUserId(subject).orElseThrow(() -> fail(HttpStatus.NOT_FOUND,"Profile was not found"));
        if(!hasIdentity(subject)||!Arrays.asList("PROFILE_COMPLETE","UNDER_REVIEW","VERIFIED","DECLINED").contains(profile.getOnboardingStatus()))throw fail(HttpStatus.CONFLICT,"Personal details and an identity document must be submitted before review");
        if(decision.getVersion()==null||profile.getVersion()!=decision.getVersion())throw fail(HttpStatus.CONFLICT,"The profile changed. Reload it before reviewing");
        if(!Arrays.asList("VERIFIED","DECLINED").contains(decision.getDecision()))throw fail(HttpStatus.BAD_REQUEST,"Choose verification or denial");
        if(decision.getDecision().equals(profile.getOnboardingStatus()))throw fail(HttpStatus.CONFLICT,"This verification decision is already recorded");
        profile.setOnboardingStatus(decision.getDecision());profile.setReviewedAt(Instant.now());profile.setReviewedBy(actor);profile.setReviewNote(decision.getNote().trim());
        audit.record(user(actor).getName(),"ONBOARDING_"+decision.getDecision()+": user="+subject+", note="+decision.getNote().trim());return profiles.save(profile);
    }
    private static ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
    private void noSelfReview(long actor,long subject){if(actor==subject)throw fail(HttpStatus.FORBIDDEN,"Administrators cannot inspect or decide their own identity verification");}
}
