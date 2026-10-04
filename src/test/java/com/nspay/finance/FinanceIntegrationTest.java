package com.nspay.finance;

import com.nspay.entities.*;
import com.nspay.enums.UserRole;
import com.nspay.repository.*;
import com.nspay.services.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.jwt.filter-enabled=true","app.schedules.enabled=false"})
@AutoConfigureMockMvc
class FinanceIntegrationTest {
    @Autowired FinanceService finance;
    @Autowired InvestmentService investments;
    @Autowired InvestmentWalletRepository wallets;
    @Autowired InvestmentCommandRepository commands;
    @Autowired SecurityService security;
    @Autowired SecurityReportRepository reports;
    @Autowired AccountRestrictionRepository restrictions;
    @Autowired StatusChangeRepository statusHistory;
    @MockBean MarketDataService market;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired SavingsPocketRepository pockets;
    @Autowired ScheduledTransferRepository schedules;
    @Autowired TradeOrderRepository trades;
    @Autowired CreditApplicationRepository credits;
    @Autowired CreditDocumentRepository documents;
    @Autowired CustomerProfileRepository profiles;
    @Autowired RiskAssessmentRepository risks;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @MockBean DocumentScanService scanner;
    UserModel alice,bob,admin;
    AccountModel aliceAccount,bobAccount;
    String aliceToken,bobToken,adminToken;
    @BeforeEach void setup() {
        commands.deleteAll(); wallets.deleteAll(); reports.deleteAll(); restrictions.deleteAll(); statusHistory.deleteAll();
        documents.deleteAll(); credits.deleteAll(); profiles.deleteAll(); trades.deleteAll();
        pockets.deleteAll(); schedules.deleteAll(); risks.deleteAll(); transactions.deleteAll(); users.deleteAll(); accounts.deleteAll();
        aliceAccount=makeAccount("4111111111111111","RO11NSPY0000000000000002",1000);
        bobAccount=makeAccount("5555555555554444","RO81NSPY0000000000000003",500);
        alice=makeUser("alice",aliceAccount,UserRole.User); bob=makeUser("bob",bobAccount,UserRole.User);
        admin=makeUser("admin",makeAccount("4000000000000002","RO38NSPY0000000000000001",1000),UserRole.Admin);
        aliceToken=jwt.generateToken(alice,aliceAccount.getCardNumber()); bobToken=jwt.generateToken(bob,bobAccount.getCardNumber()); adminToken=jwt.generateToken(admin);
    }
    AccountModel makeAccount(String card,String iban,double amount) {
        AccountModel a=new AccountModel(); a.setCardNumber(card); a.setIban(iban); a.setAmount(amount);
        a.setStatus(AccountStatus.ACTIVE); a.setDailyTxLimit(100); a.setDailyTxSumLimit(10000); return accounts.save(a);
    }
    UserModel makeUser(String name,AccountModel account,UserRole role) {
        UserModel u=new UserModel(); u.setName(name); u.setFirstName(name); u.setLastName("Demo");
        u.setAccountId(account.getId()); u.setRole(role); u.setPassword(new BCryptPasswordEncoder().encode("DemoPay123!")); return users.save(u);
    }
    String auth(String token) { return "Bearer "+token; }
    double aliceBalance() { return accounts.findById(aliceAccount.getId()).get().getAmount(); }
    @Test void loginAndOverviewAreScopedToAuthenticatedUser() throws Exception {
        mvc.perform(post("/api/login/").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"alice\",\"password\":\"DemoPay123!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("User"));
        mvc.perform(get("/api/finance/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/finance/overview").header("Authorization",auth(aliceToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.account.id").value(aliceAccount.getId()))
            .andExpect(jsonPath("$.user.password").doesNotExist());
        mvc.perform(get("/api/accounts/"+bobAccount.getId()).header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        mvc.perform(put("/api/accounts/"+aliceAccount.getId()+"/suspend").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/login/").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"missing\",\"password\":\"bad\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void savingsConservesMoneyAndRejectsCrossUserAndOverdraft() throws Exception {
        FinanceRequests.Pocket r=new FinanceRequests.Pocket();r.setName("Holiday");r.setInitialAmount(new BigDecimal("200.00"));r.setTargetAmount(new BigDecimal("1000.00"));
        SavingsPocket p=finance.createPocket(alice.getId(),r); assertEquals(800,aliceBalance());
        mvc.perform(post("/api/finance/pockets/"+p.getId()+"/withdraw").header("Authorization",auth(bobToken)).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
            .andExpect(status().isNotFound());
        assertThrows(RuntimeException.class,() -> finance.movePocket(alice.getId(),p.getId(),new BigDecimal("900"),false));
        assertEquals(800,aliceBalance()); assertEquals(200,pockets.findById(p.getId()).get().getBalance().doubleValue());
        finance.movePocket(alice.getId(),p.getId(),new BigDecimal("50"),true); assertEquals(850,aliceBalance());
        finance.closePocket(alice.getId(),p.getId()); assertEquals(1000,aliceBalance()); assertFalse(pockets.findById(p.getId()).isPresent());
    }
    InvestmentService.Funding funding(String amount,String key) {
        InvestmentService.Funding f=new InvestmentService.Funding();f.setAmount(new BigDecimal(amount));f.setRequestKey(key);return f;
    }
    InvestmentService.Order order(String side,String units,String key) {
        InvestmentService.Order o=new InvestmentService.Order();o.setSymbol("AAPL");o.setSide(side);o.setQuantity(new BigDecimal(units));o.setQuoteId("server-quote");o.setRequestKey(key);return o;
    }
    void mockQuote(String price) {
        MarketDataService.Quote q=new MarketDataService.Quote();q.setId("server-quote");q.setSymbol("AAPL");q.setPrice(new BigDecimal(price));q.setExchangeRate(new BigDecimal("0.9"));q.setEurPrice(q.getPrice().multiply(q.getExchangeRate()));q.setPriceAt(Instant.now());q.setExchangeRateAt(Instant.now());q.setExpiresAt(Instant.now().plusSeconds(60));
        org.mockito.Mockito.when(market.accepted("AAPL","server-quote")).thenReturn(q);
    }
    @Test void investmentsConserveCashAndRealizedProceedsAndRejectOverselling() {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);
        assertEquals(800,aliceBalance());assertEquals(200,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());
        investments.order(alice.getId(),order("BUY","1","buy"));assertEquals(110,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());assertEquals(800,aliceBalance());
        assertThrows(RuntimeException.class,()->investments.order(alice.getId(),order("SELL","2","oversell")));
        assertThrows(RuntimeException.class,()->investments.fund(alice.getId(),funding("200","withdraw-too-much"),true));
        mockQuote("120");investments.order(alice.getId(),order("SELL","1","sell"));
        assertEquals(218,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());
        investments.fund(alice.getId(),funding("218","withdraw"),true);assertEquals(1018,aliceBalance());assertEquals(0,wallets.findByUserId(alice.getId()).get().getBalance().signum());
    }
    @Test void investmentRetriesAreIdempotentAndConflictingKeysAreRejected() {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);investments.fund(alice.getId(),funding("200.00","deposit"),false);assertEquals(800,aliceBalance());
        assertThrows(RuntimeException.class,()->investments.fund(alice.getId(),funding("201","deposit"),false));
        TradeOrder first=investments.order(alice.getId(),order("BUY","1","buy"));TradeOrder retry=investments.order(alice.getId(),order("BUY","1","buy"));assertEquals(first.getId(),retry.getId());assertEquals(1,trades.count());assertEquals(110,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());
    }
    @Test void concurrentInvestmentRetriesMoveCashOnlyOnce() throws Exception {
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {java.util.concurrent.Callable<InvestmentWallet> task=()->investments.fund(alice.getId(),funding("200","same-key"),false);for(java.util.concurrent.Future<InvestmentWallet> result:pool.invokeAll(java.util.Arrays.asList(task,task)))result.get();}
        finally{pool.shutdownNow();}
        assertEquals(800,aliceBalance());assertEquals(200,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());assertEquals(1,commands.count());
    }
    @Test void securityReportsFreezeAndConfirmedSecondSuspensionBlocks() {
        InvestmentService.Funding f=funding("10","first");investments.fund(alice.getId(),f,false);
        TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);
        SecurityReport report=security.report(alice.getId(),tx.getId(),"I did not authorize this transaction");assertEquals(AccountStatus.FREEZE,accounts.findById(aliceAccount.getId()).get().getStatus());
        assertEquals(report.getId(),security.report(alice.getId(),tx.getId(),"Duplicate report retry").getId());
        assertThrows(RuntimeException.class,()->investments.fund(alice.getId(),funding("10","frozen"),false));
        assertThrows(RuntimeException.class,()->security.review(admin.getId(),report.getId(),true,29,"Confirmed misconduct"));assertEquals("OPEN",reports.findById(report.getId()).get().getStatus());
        security.review(admin.getId(),report.getId(),true,30,"Confirmed misconduct");assertEquals(1,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());
        assertThrows(RuntimeException.class,()->security.review(admin.getId(),report.getId(),true,30,"Repeat decision"));
        security.setStatus(admin.getId(),alice.getId(),AccountStatus.ACTIVE,null,"Administrator restored access");
        security.setStatus(admin.getId(),alice.getId(),AccountStatus.SUSPENDED,90,"Second confirmed violation");assertEquals(AccountStatus.BLOCKED,accounts.findById(aliceAccount.getId()).get().getStatus());assertEquals(2,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());assertNull(restrictions.findByUserId(alice.getId()).get().getSuspendedUntil());
    }
    @Test void separateConfirmedCasesBlockEvenDuringFirstSuspension() {
        investments.fund(alice.getId(),funding("10","first"),false);investments.fund(alice.getId(),funding("10","second"),false);
        java.util.List<TransactionModel> activity=transactions.findAllByCardNumber(aliceAccount.getCardNumber());
        SecurityReport first=security.report(alice.getId(),activity.get(0).getId(),"Unexpected first transaction");SecurityReport second=security.report(alice.getId(),activity.get(1).getId(),"Unexpected second transaction");
        security.review(admin.getId(),first.getId(),true,30,"First confirmed misconduct");security.review(admin.getId(),second.getId(),true,30,"Second confirmed misconduct");assertEquals(AccountStatus.BLOCKED,accounts.findById(aliceAccount.getId()).get().getStatus());assertEquals(2,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());
    }
    @Test void expiredSuspensionRestoresAccessWithoutResettingCount() {
        security.setStatus(admin.getId(),alice.getId(),AccountStatus.SUSPENDED,30,"Confirmed misconduct");
        AccountRestriction state=restrictions.findByUserId(alice.getId()).get();state.setSuspendedUntil(Instant.now().minusSeconds(1));restrictions.save(state);security.expire(alice.getId());
        assertEquals(AccountStatus.ACTIVE,accounts.findById(aliceAccount.getId()).get().getStatus());assertEquals(1,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());
        security.setStatus(admin.getId(),alice.getId(),AccountStatus.SUSPENDED,30,"Repeated misconduct");assertEquals(AccountStatus.BLOCKED,accounts.findById(aliceAccount.getId()).get().getStatus());
    }
    @Test void clearingReportRestoresFrozenAccountAndEnforcesOwnershipAndRole() throws Exception {
        investments.fund(alice.getId(),funding("10","first"),false);TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);
        assertThrows(RuntimeException.class,()->security.report(bob.getId(),tx.getId(),"Not my transaction"));
        SecurityReport report=security.report(alice.getId(),tx.getId(),"Unexpected transaction");security.review(admin.getId(),report.getId(),false,null,"Transaction verified legitimate");assertEquals(AccountStatus.ACTIVE,accounts.findById(aliceAccount.getId()).get().getStatus());
        mvc.perform(post("/api/security/users/"+bob.getId()+"/status").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\",\"reason\":\"Unauthorized status change\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/security/users").header("Authorization",auth(adminToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].card").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("4111111111111111"))));
    }
    @Test void concurrentReportReviewsRecordOneSuspension() throws Exception {
        investments.fund(alice.getId(),funding("10","first"),false);TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);SecurityReport report=security.report(alice.getId(),tx.getId(),"Unexpected transaction");
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{java.util.concurrent.Callable<Boolean> decision=()->{try{security.review(admin.getId(),report.getId(),true,30,"Confirmed misconduct");return true;}catch(org.springframework.web.server.ResponseStatusException e){assertEquals(409,e.getRawStatusCode());return false;}};int successful=0;for(java.util.concurrent.Future<Boolean> r:pool.invokeAll(java.util.Arrays.asList(decision,decision)))if(r.get())successful++;assertEquals(1,successful);}finally{pool.shutdownNow();}
        assertEquals(1,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());
    }
    @Test void dueScheduleExecutesOnlyOnceWithoutBrowser() {
        FinanceRequests.Payment r=new FinanceRequests.Payment();r.setRecipientIban(bobAccount.getIban());r.setAmount(new BigDecimal("25"));r.setScheduledAt(Instant.now().plusSeconds(60));
        ScheduledTransfer s=finance.schedule(alice.getId(),r);s.setScheduledAt(Instant.now().minusSeconds(1)); schedules.save(s);
        finance.executeSchedule(s.getId());finance.executeSchedule(s.getId());
        assertEquals(975,aliceBalance());assertEquals(525,accounts.findById(bobAccount.getId()).get().getAmount());
        assertEquals("COMPLETED",schedules.findById(s.getId()).get().getStatus());assertEquals(2,transactions.count());assertEquals(1,risks.count());
    }
    @Test void failedScheduleKeepsFundsAndRecordsFailure() {
        FinanceRequests.Payment r=new FinanceRequests.Payment();r.setRecipientIban(bobAccount.getIban());r.setAmount(new BigDecimal("2000"));r.setScheduledAt(Instant.now().plusSeconds(60));
        ScheduledTransfer s=finance.schedule(alice.getId(),r);s.setScheduledAt(Instant.now().minusSeconds(1));schedules.save(s);
        new FinanceScheduler(schedules,finance).executeDue();
        assertEquals("FAILED",schedules.findById(s.getId()).get().getStatus());assertEquals(1000,aliceBalance());assertEquals(0,transactions.count());
    }
    @Test void creditRequiresProfileDocumentsAndAdminReviewWithoutDisbursement() throws Exception {
        FinanceRequests.Credit r=new FinanceRequests.Credit();r.setAmount(new BigDecimal("3000"));r.setMonthlyIncome(new BigDecimal("1500"));r.setTermMonths(12);r.setPurpose("Home improvements");
        CreditApplication c=finance.createCredit(alice.getId(),r);
        assertThrows(RuntimeException.class,() -> finance.submitCredit(alice.getId(),c.getId()));
        FinanceRequests.Profile p=new FinanceRequests.Profile();p.setEmail("alice@example.test");p.setPhone("+40000000000");p.setCountry("Romania");p.setOccupation("Designer");p.setDateOfBirth(java.time.LocalDate.of(1995,1,1));finance.saveProfile(alice.getId(),p);
        assertThrows(RuntimeException.class,() -> finance.submitCredit(alice.getId(),c.getId()));
        MockMultipartFile file=new MockMultipartFile("file","demo.pdf","application/pdf","%PDF-1.4 demo".getBytes());
        CreditDocument doc=finance.upload(alice.getId(),c.getId(),"IDENTITY",file);finance.upload(alice.getId(),c.getId(),"INCOME",file);
        mvc.perform(get("/api/finance/documents/"+doc.getId()+"/download").header("Authorization",auth(bobToken))).andExpect(status().isForbidden());
        assertEquals("UNDER_REVIEW",finance.submitCredit(alice.getId(),c.getId()).getStatus());
        FinanceRequests.Review review=new FinanceRequests.Review();review.setDecision("APPROVED");review.setNote("Documents reviewed for the local demo");
        assertThrows(RuntimeException.class,() -> finance.reviewCredit(bob.getId(),c.getId(),review));
        assertEquals("APPROVED",finance.reviewCredit(admin.getId(),c.getId(),review).getStatus());assertEquals(1000,aliceBalance());
    }
    @Test void invalidAmountsAndFakeDocumentAreRejected() throws Exception {
        mvc.perform(post("/api/finance/pockets").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Demo\",\"initialAmount\":-1,\"targetAmount\":100}" )).andExpect(status().isBadRequest());
        mvc.perform(post("/api/finance/trades").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"symbol\":\"NSPX\",\"side\":\"BUY\",\"quantity\":0}" )).andExpect(status().isBadRequest());
        mvc.perform(get("/api/finance/reviews").header("Authorization",auth(aliceToken)).header("userRole","Admin")).andExpect(status().isForbidden());
        assertThrows(RuntimeException.class,() -> finance.upload(alice.getId(),null,"IDENTITY",new MockMultipartFile("file","fake.pdf","application/pdf","plain text".getBytes())));
    }
}
