package com.nspay.finance;

import com.nspay.entities.*;
import com.nspay.repository.*;
import com.nspay.services.*;
import com.nspay.dto.TransferRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.io.IOException;

@Service
public class FinanceService {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final SavingsPocketRepository pockets;
    private final ScheduledTransferRepository schedules;
    private final TradeOrderRepository trades;
    private final CreditApplicationRepository credits;
    private final CreditDocumentRepository documents;
    private final CustomerProfileRepository profiles;
    private final AuditService audit;
    private final TransferService transfers;
    private final DocumentScanService scanner;

    public FinanceService(UserRepository users, AccountRepository accounts, TransactionRepository transactions,
        SavingsPocketRepository pockets, ScheduledTransferRepository schedules, TradeOrderRepository trades,
        CreditApplicationRepository credits, CreditDocumentRepository documents, CustomerProfileRepository profiles,
        AuditService audit, TransferService transfers, DocumentScanService scanner) {
        this.users=users; this.accounts=accounts; this.transactions=transactions; this.pockets=pockets;
        this.schedules=schedules; this.trades=trades; this.credits=credits; this.documents=documents;
        this.profiles=profiles; this.audit=audit; this.transfers=transfers; this.scanner=scanner;
    }

    public UserModel user(long id) {
        return users.findById(id).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "User was not found"));
    }
    public AccountModel account(long id, boolean lock) {
        Long accountId=user(id).getAccountId();
        if (accountId==null) throw error(HttpStatus.BAD_REQUEST, "No account is assigned to your user");
        AccountModel account=lock ? accounts.lockById(accountId) : accounts.findById(accountId).orElse(null);
        if (account==null) throw error(HttpStatus.BAD_REQUEST, "Account was not found");
        if (lock && account.getStatus()!=AccountStatus.ACTIVE) throw error(HttpStatus.BAD_REQUEST, "Account must be active");
        return account;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> overview(long id) {
        Map<String,Object> result=new LinkedHashMap<>();
        AccountModel account=account(id, false);
        result.put("user",user(id)); result.put("account",account); result.put("currency","EUR");
        result.put("pockets",pockets.findByUserIdOrderByCreatedAtDesc(id));
        result.put("scheduled",schedules.findByUserIdOrderByCreatedAtDesc(id));
        result.put("trades",trades.findByUserIdOrderByCreatedAtDesc(id));
        result.put("credits",credits.findByUserIdOrderByCreatedAtDesc(id));
        result.put("profile",profiles.findByUserId(id).orElse(null));
        result.put("documents",documents.findByUserIdOrderByCreatedAtDesc(id));
        result.put("transactions",transactions.findAllByCardNumber(account.getCardNumber()).stream()
            .sorted(Comparator.comparing(TransactionModel::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(100).collect(Collectors.toList()));
        result.put("quotes",Collections.emptyList());
        return result;
    }
    @Transactional
    public SavingsPocket createPocket(long id, FinanceRequests.Pocket request) {
        AccountModel account=account(id,true);
        debit(account,request.getInitialAmount());
        SavingsPocket pocket=new SavingsPocket(); pocket.setUserId(id); pocket.setName(request.getName().trim());
        pocket.setTargetAmount(request.getTargetAmount()); pocket.setBalance(cents(request.getInitialAmount()));
        pockets.save(pocket);
        if (request.getInitialAmount().signum()>0) ledger(account,request.getInitialAmount(),"SAVINGS_OUT",null);
        audit.record(user(id).getName(),"SAVINGS_CREATED: "+pocket.getName());
        return pocket;
    }
    @Transactional
    public SavingsPocket movePocket(long id,long pocketId,BigDecimal amount,boolean withdraw) {
        AccountModel account=account(id,true);
        SavingsPocket pocket=pockets.lockById(pocketId).filter(p -> p.getUserId()==id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Savings pocket was not found"));
        if (withdraw) {
            if (pocket.getBalance().compareTo(amount)<0) throw error(HttpStatus.BAD_REQUEST,"Savings pocket has insufficient funds");
            pocket.setBalance(pocket.getBalance().subtract(amount)); credit(account,amount);
        } else { debit(account,amount); pocket.setBalance(pocket.getBalance().add(amount)); }
        ledger(account,amount,withdraw ? "SAVINGS_IN" : "SAVINGS_OUT",null);
        audit.record(user(id).getName(),withdraw ? "SAVINGS_WITHDRAWAL" : "SAVINGS_DEPOSIT");
        return pockets.save(pocket);
    }
    @Transactional
    public void closePocket(long id,long pocketId) {
        AccountModel account=account(id,true);
        SavingsPocket pocket=pockets.lockById(pocketId).filter(p -> p.getUserId()==id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Savings pocket was not found"));
        credit(account,pocket.getBalance());
        if (pocket.getBalance().signum()>0) ledger(account,pocket.getBalance(),"SAVINGS_IN",null);
        pockets.delete(pocket); audit.record(user(id).getName(),"SAVINGS_CLOSED");
    }
    @Transactional
    public ScheduledTransfer schedule(long id,FinanceRequests.Payment request) {
        if (request.getScheduledAt()==null || !request.getScheduledAt().isAfter(Instant.now()))
            throw error(HttpStatus.BAD_REQUEST,"Choose a future execution time");
        AccountModel sender=account(id,false);
        String iban=request.getRecipientIban().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        AccountModel recipient=accounts.findByIban(iban);
        if (recipient==null || sender.getId()==recipient.getId()) throw error(HttpStatus.BAD_REQUEST,"Choose another existing recipient IBAN");
        ScheduledTransfer item=new ScheduledTransfer(); item.setUserId(id); item.setRecipientIban(iban);
        item.setAmount(request.getAmount()); item.setScheduledAt(request.getScheduledAt());
        audit.record(user(id).getName(),"TRANSFER_SCHEDULED"); return schedules.save(item);
    }
    @Transactional
    public void cancelSchedule(long id,long scheduleId) {
        ScheduledTransfer item=schedules.lockById(scheduleId).filter(p -> p.getUserId()==id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Scheduled transfer was not found"));
        if (!"PENDING".equals(item.getStatus())) throw error(HttpStatus.CONFLICT,"Only pending transfers can be cancelled");
        item.setStatus("CANCELLED"); schedules.save(item);
    }
    @Transactional
    public void executeSchedule(long scheduleId) {
        ScheduledTransfer item=schedules.lockById(scheduleId).orElse(null);
        if (item==null || !"PENDING".equals(item.getStatus()) || item.getScheduledAt().isAfter(Instant.now())) return;
        TransferRequestDTO request=new TransferRequestDTO(); request.setRecipientIban(item.getRecipientIban()); request.setAmount(item.getAmount().doubleValue());
        transfers.transfer(item.getUserId(),request);
        item.setStatus("COMPLETED"); item.setExecutedAt(Instant.now()); schedules.save(item);
        audit.record(user(item.getUserId()).getName(),"SCHEDULED_TRANSFER_COMPLETED");
    }
    @Transactional
    public void failSchedule(long scheduleId) {
        ScheduledTransfer item=schedules.lockById(scheduleId).orElse(null);
        if (item!=null && "PENDING".equals(item.getStatus())) {
            item.setStatus("FAILED"); item.setFailureReason("Transfer could not execute. Check account status, limits, and available funds."); schedules.save(item);
        }
    }
    /** Legacy orders must use the quoted investment API. */
    public TradeOrder trade(long id,FinanceRequests.Trade request) {
        throw error(HttpStatus.GONE,"Use the investment order endpoint with a current market quote");
    }
    @Transactional
    public CustomerProfile saveProfile(long id,FinanceRequests.Profile request) {
        account(id,true);
        if (request.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) throw error(HttpStatus.BAD_REQUEST,"Onboarding requires age 18 or above");
        CustomerProfile profile=profiles.findByUserId(id).orElseGet(CustomerProfile::new); profile.setUserId(id);
        profile.setEmail(request.getEmail()); profile.setPhone(request.getPhone()); profile.setCountry(request.getCountry());
        profile.setDateOfBirth(request.getDateOfBirth()); profile.setOccupation(request.getOccupation()); profile.setOnboardingStatus("PROFILE_COMPLETE");
        audit.record(user(id).getName(),"ONBOARDING_PROFILE_SAVED"); return profiles.save(profile);
    }
    @Transactional
    public CreditApplication createCredit(long id,FinanceRequests.Credit request) {
        account(id,true);
        CreditApplication credit=new CreditApplication(); credit.setUserId(id); credit.setAmount(request.getAmount());
        credit.setMonthlyIncome(request.getMonthlyIncome()); credit.setTermMonths(request.getTermMonths()); credit.setPurpose(request.getPurpose().trim());
        audit.record(user(id).getName(),"CREDIT_DRAFT_CREATED"); return credits.save(credit);
    }
    @Transactional
    public CreditApplication submitCredit(long id,long applicationId) {
        CreditApplication credit=credits.lockById(applicationId).filter(c -> c.getUserId()==id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Credit application was not found"));
        if (!"DRAFT".equals(credit.getStatus())) throw error(HttpStatus.CONFLICT,"Application has already been submitted");
        if (!profiles.findByUserId(id).isPresent()) throw error(HttpStatus.BAD_REQUEST,"Complete your profile before submitting");
        List<CreditDocument> docs=documents.findByApplicationIdAndUserId(applicationId,id);
        if (docs.stream().noneMatch(d -> "IDENTITY".equals(d.getKind())) || docs.stream().noneMatch(d -> "INCOME".equals(d.getKind())))
            throw error(HttpStatus.BAD_REQUEST,"Upload identity and income documents before submitting");
        credit.setStatus("UNDER_REVIEW"); audit.record(user(id).getName(),"CREDIT_SUBMITTED"); return credits.save(credit);
    }
    @Transactional
    public CreditApplication reviewCredit(long reviewer,long applicationId,FinanceRequests.Review review) {
        if (user(reviewer).getRole()!=com.nspay.enums.UserRole.Admin) throw error(HttpStatus.FORBIDDEN,"Only administrators can review credit");
        CreditApplication credit=credits.lockById(applicationId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Application was not found"));
        if (!"UNDER_REVIEW".equals(credit.getStatus())) throw error(HttpStatus.CONFLICT,"Application is not awaiting review");
        if ("REVIEWED".equals(review.getDecision())) throw error(HttpStatus.BAD_REQUEST,"Choose approval or decline");
        credit.setStatus(review.getDecision()); credit.setReviewNote(review.getNote());
        audit.record(user(reviewer).getName(),"CREDIT_REVIEW: "+applicationId); return credits.save(credit);
    }
    @Transactional
    public CreditDocument upload(long id,Long applicationId,String kind,MultipartFile file) throws IOException {
        if (!Arrays.asList("IDENTITY","INCOME").contains(kind)) throw error(HttpStatus.BAD_REQUEST,"Choose identity or income document");
        account(id,true);
        if (applicationId!=null) {
            CreditApplication credit=credits.lockById(applicationId).filter(c -> c.getUserId()==id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Application was not found"));
            if (!"DRAFT".equals(credit.getStatus())) throw error(HttpStatus.CONFLICT,"Only drafts accept new documents");
        }
        if (file.isEmpty() || file.getSize()>5*1024*1024) throw error(HttpStatus.BAD_REQUEST,"Choose a PDF, PNG, or JPEG up to 5 MB");
        byte[] bytes=file.getBytes(); String type=detectType(bytes);
        if (type==null) throw error(HttpStatus.BAD_REQUEST,"File content must be PDF, PNG, or JPEG");
        CreditDocument document=new CreditDocument(); document.setUserId(id); document.setApplicationId(applicationId); document.setKind(kind);
        String original=Optional.ofNullable(file.getOriginalFilename()).orElse("document").replace('\\','/');
        String filename=original.substring(original.lastIndexOf('/')+1).replaceAll("[^A-Za-z0-9._ -]", "_");
        document.setFilename(filename.substring(0,Math.min(filename.length(),150))); document.setContent(bytes); document.setContentType(type);
        try { document.setSha256(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        scanner.scan(document); audit.record(user(id).getName(),"DOCUMENT_UPLOADED: "+kind); return documents.save(document);
    }
    private String detectType(byte[] b) {
        if (b.length>=5 && b[0]=='%' && b[1]=='P' && b[2]=='D' && b[3]=='F' && b[4]=='-') return "application/pdf";
        if (b.length>=8 && Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (b.length>=3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255) return "image/jpeg";
        return null;
    }
    public static BigDecimal cents(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }
    private void debit(AccountModel account,BigDecimal amount) {
        BigDecimal available=cents(BigDecimal.valueOf(account.getAmount()));
        if (amount.signum()<0 || available.compareTo(amount)<0) throw error(HttpStatus.BAD_REQUEST,"Insufficient available balance");
        account.setAmount(available.subtract(amount).doubleValue()); accounts.save(account);
    }
    private void credit(AccountModel account,BigDecimal amount) {
        account.setAmount(cents(BigDecimal.valueOf(account.getAmount()).add(amount)).doubleValue()); accounts.save(account);
    }
    private void ledger(AccountModel account,BigDecimal amount,String type,String reference) {
        TransactionModel transaction=new TransactionModel(); transaction.setCardNumber(account.getCardNumber()); transaction.setAmount(amount.doubleValue());
        transaction.setTimestamp(java.sql.Timestamp.from(Instant.now())); transaction.setReturnCode("00"); transaction.setTransactionType(type); transaction.setCounterpartyIban(reference);
        transactions.save(transaction);
    }
    private static ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
