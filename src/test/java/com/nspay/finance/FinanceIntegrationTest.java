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
    @Autowired InvestmentPositionRepository positions;
    @Autowired PortfolioPerformanceRepository performance;
    @Autowired SecurityService security;
    @Autowired SecurityReportRepository reports;
    @Autowired AccountRestrictionRepository restrictions;
    @Autowired StatusChangeRepository statusHistory;
    @Autowired PaymentSignalRepository paymentSignals;
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
    @Autowired OnboardingService onboarding;
    @Autowired RiskAssessmentRepository risks;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @MockBean DocumentScanService scanner;
    UserModel alice,bob,admin;
    AccountModel aliceAccount,bobAccount;
    String aliceToken,bobToken,adminToken;
    @BeforeEach void setup() {
        paymentSignals.deleteAll();performance.deleteAll();positions.deleteAll();commands.deleteAll(); wallets.deleteAll(); reports.deleteAll(); restrictions.deleteAll(); statusHistory.deleteAll();
        documents.deleteAll(); credits.deleteAll(); profiles.deleteAll(); trades.deleteAll();
        pockets.deleteAll(); schedules.deleteAll(); risks.deleteAll(); transactions.deleteAll(); users.deleteAll(); accounts.deleteAll();
        aliceAccount=makeAccount("4111111111111111","RO11NSPY0000000000000002",1000);
        bobAccount=makeAccount("5555555555554444","RO81NSPY0000000000000003",500);
        alice=makeUser("alice",aliceAccount,UserRole.User); bob=makeUser("bob",bobAccount,UserRole.User);
        admin=makeUser("admin",makeAccount("4000000000000002","RO38NSPY0000000000000001",1000),UserRole.Admin);
        org.mockito.Mockito.when(market.source()).thenReturn("twelve-data");
        org.mockito.Mockito.when(market.catalog()).thenReturn(java.util.Arrays.asList(new MarketDataService.Instrument("AAPL","Apple","Technology")));
        org.mockito.Mockito.when(market.configured()).thenReturn(true);
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
        org.mockito.Mockito.when(market.quotes(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(java.util.Collections.singletonMap("AAPL",q));
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
    @Test void portfolioUsesAverageCostAndCurrentPricesAfterPartialSale() {
        investments.fund(alice.getId(),funding("300","deposit"),false);
        mockQuote("100");investments.order(alice.getId(),order("BUY","1","first"));
        mockQuote("120");investments.order(alice.getId(),order("BUY","1","second"));
        mockQuote("140");investments.order(alice.getId(),order("SELL","0.5","partial"));
        java.util.Map<String,Object> view=investments.portfolio(alice.getId());
        assertEquals(new BigDecimal("354.00"),view.get("totalValue"));assertEquals(new BigDecimal("148.50"),view.get("costBasis"));
        assertEquals(new BigDecimal("13.50"),view.get("realizedGain"));assertEquals(new BigDecimal("40.50"),view.get("unrealizedGain"));assertEquals(new BigDecimal("54.00"),view.get("totalGain"));
        InvestmentPosition p=positions.findByUserIdOrderBySymbolAsc(alice.getId()).get(0);assertEquals(0,new BigDecimal("1.5").compareTo(p.getQuantity()));
        assertEquals(new BigDecimal("0.00"),investments.portfolio(bob.getId()).get("totalValue"));
    }
    @Test void missingPricesDoNotTurnHoldingsIntoZeroAndCashRemainsWithdrawable() {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);investments.order(alice.getId(),order("BUY","1","buy"));
        org.mockito.Mockito.when(market.quotes(org.mockito.ArgumentMatchers.anyCollection())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Provider unavailable"));
        java.util.Map<String,Object> view=investments.portfolio(alice.getId());assertEquals("INCOMPLETE",view.get("valuationStatus"));assertNull(view.get("totalValue"));assertNull(view.get("unrealizedGain"));assertEquals(new BigDecimal("90.00"),view.get("costBasis"));
        investments.fund(alice.getId(),funding("110","withdraw"),true);assertEquals(910,aliceBalance());assertEquals(1,positions.count());
    }
    @Test void changingPriceSourceCannotSellOrRevalueLegacyHoldings() {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);investments.order(alice.getId(),order("BUY","1","buy"));
        org.mockito.Mockito.when(market.source()).thenReturn("cuckoo-new-dataset");
        MarketDataService.Quote q=market.accepted("AAPL","server-quote");q.setMarketProvider("cuckoo-new-dataset");
        assertNull(investments.portfolio(alice.getId()).get("totalValue"));
        assertThrows(RuntimeException.class,()->investments.order(alice.getId(),order("SELL","1","wrong-source")));
        assertEquals(110,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());assertEquals(1,trades.count());assertEquals(1,positions.count());
    }
    @Test void concurrentSalesCannotOversellProjectedPositions() throws Exception {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);investments.order(alice.getId(),order("BUY","1","buy"));mockQuote("120");
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{java.util.List<java.util.concurrent.Callable<Boolean>> work=new java.util.ArrayList<>();for(int i=0;i<2;i++){final String key="sell-"+i;work.add(()->{try{investments.order(alice.getId(),order("SELL","1",key));return true;}catch(org.springframework.web.server.ResponseStatusException e){assertEquals(400,e.getRawStatusCode());return false;}});}
            int completed=0;for(java.util.concurrent.Future<Boolean> f:pool.invokeAll(work))if(f.get())completed++;assertEquals(1,completed);
        }finally{pool.shutdownNow();}
        assertEquals(218,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());assertEquals(2,trades.count());assertEquals(0,positions.findByUserIdOrderBySymbolAsc(alice.getId()).get(0).getQuantity().signum());
    }
    @Test void existingOrdersAreProjectedOnceWithoutChangingCash() {
        InvestmentWallet w=new InvestmentWallet();w.setUserId(alice.getId());w.setBalance(new BigDecimal("74"));wallets.save(w);
        for(String[] input:new String[][]{{"BUY","2","180"},{"SELL","0.5","54"}}){TradeOrder t=new TradeOrder();t.setUserId(alice.getId());t.setSymbol("AAPL");t.setSide(input[0]);t.setQuantity(new BigDecimal(input[1]));t.setPrice(new BigDecimal("90"));t.setTotal(new BigDecimal(input[2]));trades.save(t);}
        mockQuote("120");java.util.Map<String,Object> first=investments.portfolio(alice.getId());java.util.Map<String,Object> second=investments.portfolio(alice.getId());
        assertEquals(new BigDecimal("236.00"),first.get("totalValue"));assertEquals(first.get("totalGain"),second.get("totalGain"));assertEquals(new BigDecimal("9.00"),first.get("realizedGain"));assertEquals(1,positions.count());assertEquals(74,wallets.findByUserId(alice.getId()).get().getBalance().doubleValue());assertTrue(wallets.findByUserId(alice.getId()).get().isPositionsInitialized());
    }
    @Test void orderHistoryIsPaginatedAndOwnerScoped() throws Exception {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);investments.order(alice.getId(),order("BUY","1","buy"));
        mvc.perform(get("/api/investments/orders?page=0&size=1").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/investments/orders").header("Authorization",auth(bobToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/investments/orders?size=101").header("Authorization",auth(aliceToken))).andExpect(status().isBadRequest());
    }
    @Test void authenticatedActivityRenewsADayLongSessionButExpiredTokensCannotRenew() throws Exception {
        com.nspay.services.JwtService shortSession=new com.nspay.services.JwtService("local-dev-local-dev-local-dev-local-dev-local-dev",86400);
        String shortToken=shortSession.generateToken(alice);
        org.springframework.mock.web.MockHttpServletResponse response=mvc.perform(get("/api/finance/overview").header("Authorization",auth(shortToken))).andExpect(status().isOk()).andReturn().getResponse();
        String renewed=response.getHeader("X-Session-Token");assertNotNull(renewed);
        io.jsonwebtoken.Claims claims=jwt.parseToken(renewed);assertEquals(alice.getId(),((Number)claims.get("userId")).longValue());assertEquals(86400000L,claims.getExpiration().getTime()-claims.getIssuedAt().getTime());assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=86400"));
        String expired=new com.nspay.services.JwtService("local-dev-local-dev-local-dev-local-dev-local-dev",-1000).generateToken(alice);
        mvc.perform(get("/api/finance/overview").header("Authorization",auth(expired))).andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("X-Session-Token"));
    }
    @Test void profitHistoryIsDailyOwnerScopedAndDoesNotCountDepositsAsProfit() throws Exception {
        mockQuote("100");investments.fund(alice.getId(),funding("200","deposit"),false);investments.order(alice.getId(),order("BUY","1","buy"));
        mockQuote("120");investments.portfolio(alice.getId());investments.portfolio(alice.getId());assertEquals(1,performance.count());
        mvc.perform(get("/api/investments/performance").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].totalGain").value(18));
        mvc.perform(get("/api/investments/performance").header("Authorization",auth(bobToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].totalGain").value(0));
        mvc.perform(get("/api/investments/performance?range=bad").header("Authorization",auth(aliceToken))).andExpect(status().isBadRequest());
    }
    @Test void securityReportsLeaveAccessUnchangedAndConfirmedSecondSuspensionBlocks() {
        InvestmentService.Funding f=funding("10","first");investments.fund(alice.getId(),f,false);
        TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);
        SecurityReport report=security.report(alice.getId(),tx.getId(),"I did not authorize this transaction");assertEquals(AccountStatus.ACTIVE,accounts.findById(aliceAccount.getId()).get().getStatus());assertEquals(0,statusHistory.count());
        assertEquals(report.getId(),security.report(alice.getId(),tx.getId(),"Duplicate report retry").getId());
        assertThrows(RuntimeException.class,()->security.review(admin.getId(),report.getId(),true,29,"Confirmed misconduct"));assertEquals("OPEN",reports.findById(report.getId()).get().getStatus());
        security.review(admin.getId(),report.getId(),true,30,"Confirmed misconduct");assertEquals(1,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());
        assertThrows(RuntimeException.class,()->investments.fund(alice.getId(),funding("10","suspended"),false));
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
    @Test void clearingReportLeavesAccessUnchangedAndEnforcesOwnershipAndRole() throws Exception {
        investments.fund(alice.getId(),funding("10","first"),false);TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);
        assertThrows(RuntimeException.class,()->security.report(bob.getId(),tx.getId(),"Not my transaction"));
        SecurityReport report=security.report(alice.getId(),tx.getId(),"Unexpected transaction");security.setStatus(admin.getId(),alice.getId(),AccountStatus.FREEZE,null,"Unrelated account investigation");security.review(admin.getId(),report.getId(),false,null,"Transaction verified legitimate");assertEquals(AccountStatus.FREEZE,accounts.findById(aliceAccount.getId()).get().getStatus());
        mvc.perform(post("/api/security/users/"+bob.getId()+"/status").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\",\"reason\":\"Unauthorized status change\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/security/users").header("Authorization",auth(adminToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].card").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("4111111111111111"))));
    }
    @Test void concurrentReportReviewsRecordOneSuspension() throws Exception {
        investments.fund(alice.getId(),funding("10","first"),false);TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);SecurityReport report=security.report(alice.getId(),tx.getId(),"Unexpected transaction");
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{java.util.concurrent.Callable<Boolean> decision=()->{try{security.review(admin.getId(),report.getId(),true,30,"Confirmed misconduct");return true;}catch(org.springframework.web.server.ResponseStatusException e){assertEquals(409,e.getRawStatusCode());return false;}};int successful=0;for(java.util.concurrent.Future<Boolean> r:pool.invokeAll(java.util.Arrays.asList(decision,decision)))if(r.get())successful++;assertEquals(1,successful);}finally{pool.shutdownNow();}
        assertEquals(1,restrictions.findByUserId(alice.getId()).get().getSuspensionCount());
    }
    @Test void paymentReviewControlsRestrictionsAndKeepsPrivateNotesOwnerScoped() throws Exception {
        investments.fund(alice.getId(),funding("10","security-case"),false);TransactionModel tx=transactions.findAllByCardNumber(aliceAccount.getCardNumber()).get(0);
        mvc.perform(post("/api/security/transactions/"+tx.getId()+"/report").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"I did not authorize this transaction\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        assertEquals(AccountStatus.ACTIVE,accounts.findById(aliceAccount.getId()).orElseThrow().getStatus());assertEquals(0,statusHistory.count());
        mvc.perform(get("/api/security/payments").header("Authorization",auth(bobToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/security/transactions/"+tx.getId()+"/evidence").header("Authorization",auth(bobToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/finance/risk-assessments").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/finance/risk-preview").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content("{\"recipientIban\":\"RO81NSPY0000000000000003\",\"amount\":1}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/security/transactions/"+tx.getId()+"/report").header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Administrator report attempt\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/security/payments").header("Authorization",auth(adminToken)).param("reported","reported").param("sort","reportingScore").param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].reported").value(true)).andExpect(jsonPath("$.items[0].reportingScore").value(60));
        mvc.perform(get("/api/security/payments").header("Authorization",auth(adminToken)).param("sort","privateNote")).andExpect(status().isBadRequest());
        String decision="{\"status\":\"FREEZE\",\"reason\":\"Account access restricted after review\",\"publicNote\":\"Account temporarily frozen for investigation\",\"privateNote\":\"Private customer instruction 123\"}";
        mvc.perform(post("/api/security/transactions/"+tx.getId()+"/review").header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content(decision))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("FREEZE"));
        assertEquals(AccountStatus.FREEZE,accounts.findById(aliceAccount.getId()).orElseThrow().getStatus());assertEquals("CONFIRMED",reports.findByUserIdOrderByCreatedAtDesc(alice.getId()).get(0).getStatus());
        mvc.perform(get("/api/security").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$.activity[0].privateNote").value("Private customer instruction 123"));
        mvc.perform(get("/api/security").header("Authorization",auth(bobToken))).andExpect(status().isOk()).andExpect(jsonPath("$.activity[0].publicNote").value("Account temporarily frozen for investigation"))
            .andExpect(jsonPath("$.activity[0].privateNote").doesNotExist()).andExpect(jsonPath("$.activity[0].reason").doesNotExist()).andExpect(jsonPath("$.activity[0].transactionId").doesNotExist());
        mvc.perform(post("/api/security/transactions/"+tx.getId()+"/review").header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content(decision)).andExpect(status().isConflict());
        assertEquals(1,statusHistory.count());assertEquals(990,aliceBalance());
    }
    @Autowired com.nspay.services.TransferService transferService;
    @Autowired com.nspay.services.TransactionService cardService;
    @Autowired DailyPaymentUsage dailyUsage;
    @Autowired PaymentSignalService signalService;
    @Test void advisoryLimitsAllowPaymentsAndIncreaseScoresWithoutRestrictions() throws Exception {
        mvc.perform(put("/api/account-limits/"+aliceAccount.getId()).header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"dailyTxLimit\":1,\"dailyTxSumLimit\":10,\"amount\":999999,\"dailyTx\":0,\"status\":\"BLOCKED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usage.dailyTxLimit").value(1));
        assertEquals(1000,aliceBalance());assertEquals(AccountStatus.ACTIVE,accounts.findById(aliceAccount.getId()).orElseThrow().getStatus());
        com.nspay.dto.TransferRequestDTO transfer=new com.nspay.dto.TransferRequestDTO();transfer.setRecipientIban(bobAccount.getIban());transfer.setAmount(10.0);
        transferService.transfer(alice.getId(),transfer);transferService.transfer(alice.getId(),transfer);
        com.nspay.validators.ValidationResults card=cardService.processTransaction(new com.nspay.dto.TransactionDTO(aliceAccount.getCardNumber(),5.0,java.sql.Timestamp.from(Instant.now())));
        assertEquals(com.nspay.validators.ResponseCode.APPROVED,card.getCode());
        assertEquals(975,aliceBalance());assertEquals(520,accounts.findById(bobAccount.getId()).orElseThrow().getAmount());
        mvc.perform(get("/api/finance/overview").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$.dailyUsage.dailyTx").value(3)).andExpect(jsonPath("$.dailyUsage.dailyTxSum").value(25)).andExpect(jsonPath("$.dailyUsage.level").value("EXCEEDED"));
        mvc.perform(get("/api/security/payments").header("Authorization",auth(adminToken)).param("search","alice").param("metric","limitScore").param("minScore","60").param("sort","limitScore")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3));
        assertEquals(0,statusHistory.count());
        PaymentSignal payment=paymentSignals.findAll().stream().filter(t->t.getUserId()==alice.getId()&&t.getDailyTx()==3).findFirst().orElseThrow();int high=payment.getMisalignmentScore();assertEquals(100,payment.getLimitScore());
        mvc.perform(put("/api/account-limits/"+aliceAccount.getId()).header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"dailyTxLimit\":100,\"dailyTxSumLimit\":1000}")) .andExpect(status().isOk());
        payment=paymentSignals.findByTransactionId(payment.getTransactionId()).orElseThrow();assertEquals(0,payment.getLimitScore());assertEquals(25,high-payment.getMisalignmentScore());assertEquals(975,aliceBalance());assertEquals(3,accounts.findById(aliceAccount.getId()).orElseThrow().getDailyTx());
    }
    @Test void usageRollsOverFromLedgerAndExcludesReceiptsFailuresAndInternalMoves() {
        Instant now=Instant.now();Instant yesterday=now.atZone(DailyPaymentUsage.ZONE).toLocalDate().atStartOfDay(DailyPaymentUsage.ZONE).toInstant().minusSeconds(1);
        for(String type:java.util.List.of("TRANSFER_OUT","PAYMENT","TRANSFER_IN","SAVINGS_OUT","INVESTMENT_DEPOSIT")){TransactionModel tx=new TransactionModel();tx.setCardNumber(aliceAccount.getCardNumber());tx.setAmount(5.0);tx.setReturnCode("00");tx.setTransactionType(type);tx.setTimestamp(java.sql.Timestamp.from(now.minusSeconds(1)));transactions.save(tx);}
        TransactionModel old=new TransactionModel();old.setCardNumber(aliceAccount.getCardNumber());old.setAmount(200.0);old.setReturnCode("00");old.setTransactionType("TRANSFER_OUT");old.setTimestamp(java.sql.Timestamp.from(yesterday));transactions.save(old);
        TransactionModel failed=new TransactionModel();failed.setCardNumber(aliceAccount.getCardNumber());failed.setAmount(300.0);failed.setReturnCode("51");failed.setTimestamp(java.sql.Timestamp.from(now.minusSeconds(1)));transactions.save(failed);
        aliceAccount.setDailyTx(99);aliceAccount.setDailyTxSum(9999);accounts.save(aliceAccount);
        DailyPaymentUsage.Usage current=dailyUsage.current(aliceAccount);assertEquals(2,current.getDailyTx());assertEquals(10,current.getDailyTxSum());assertEquals("NORMAL",current.getLevel());assertEquals(now.atZone(DailyPaymentUsage.ZONE).toLocalDate().plusDays(1).atStartOfDay(DailyPaymentUsage.ZONE).toInstant(),current.getResetsAt());
    }
    @Test void limitsAdministrationRequiresStoredAdminRoleAndDeletionIsUnavailable() throws Exception {
        String path="/api/account-limits/"+aliceAccount.getId();String body="{\"dailyTxLimit\":1,\"dailyTxSumLimit\":10}";
        mvc.perform(get("/api/account-limits").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization",auth(bobToken)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"dailyTxLimit\":0,\"dailyTxSumLimit\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/account-limits").header("Authorization",auth(adminToken)).param("search","%")) .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(delete("/api/accounts/"+aliceAccount.getId()).header("Authorization",auth(adminToken))).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/users/"+alice.getId()).header("Authorization",auth(adminToken))).andExpect(status().isMethodNotAllowed());
        assertTrue(users.existsById(alice.getId()));assertTrue(accounts.existsById(aliceAccount.getId()));
    }
    @Test void administratorCannotInspectOrDecideOwnIdentityAndCreditQueueIsProtected() throws Exception {
        mvc.perform(get("/api/finance/onboarding/users/"+admin.getId()).header("Authorization",auth(adminToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/finance/onboarding/users/"+admin.getId()+"/review").header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"VERIFIED\",\"note\":\"Self approval attempted\",\"version\":0}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/finance/credit/reviews").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        CreditApplication draft=new CreditApplication();draft.setUserId(alice.getId());draft.setAmount(new BigDecimal("3000"));draft.setMonthlyIncome(new BigDecimal("1500"));draft.setTermMonths(12);draft.setPurpose("Home office upgrade");credits.save(draft);
        CreditApplication own=new CreditApplication();own.setUserId(admin.getId());own.setAmount(new BigDecimal("3000"));own.setMonthlyIncome(new BigDecimal("1500"));own.setTermMonths(12);own.setPurpose("Own application");credits.save(own);
        mvc.perform(get("/api/finance/credit/reviews").header("Authorization",auth(adminToken)).param("status","DRAFT").param("search","alice").param("sort","amount"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].username").value("alice"));
        mvc.perform(get("/api/finance/credit/reviews").header("Authorization",auth(adminToken)).param("status","All"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
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
        assertThrows(RuntimeException.class,() -> finance.createCredit(alice.getId(),r));
        FinanceRequests.Profile p=new FinanceRequests.Profile();p.setEmail("alice@example.test");p.setPhone("+40000000000");p.setCountry("Romania");p.setOccupation("Designer");p.setDateOfBirth(java.time.LocalDate.of(1995,1,1));finance.saveProfile(alice.getId(),p);
        assertThrows(RuntimeException.class,() -> finance.createCredit(alice.getId(),r));
        MockMultipartFile file=new MockMultipartFile("file","demo.pdf","application/pdf","%PDF-1.4 demo".getBytes());
        finance.upload(alice.getId(),null,"IDENTITY",file);verifyIdentity(alice.getId(),"VERIFIED");
        CreditApplication c=finance.createCredit(alice.getId(),r);
        assertThrows(RuntimeException.class,() -> finance.submitCredit(alice.getId(),c.getId()));
        CreditDocument doc=finance.upload(alice.getId(),c.getId(),"IDENTITY",file);finance.upload(alice.getId(),c.getId(),"INCOME",file);
        mvc.perform(get("/api/finance/documents/"+doc.getId()+"/download").header("Authorization",auth(bobToken))).andExpect(status().isNotFound());
        assertEquals("UNDER_REVIEW",finance.submitCredit(alice.getId(),c.getId()).getStatus());
        FinanceRequests.Review review=new FinanceRequests.Review();review.setDecision("APPROVED");review.setNote("Documents reviewed for the local demo");
        assertThrows(RuntimeException.class,() -> finance.reviewCredit(bob.getId(),c.getId(),review));
        assertEquals("APPROVED",finance.reviewCredit(admin.getId(),c.getId(),review).getStatus());assertEquals(1000,aliceBalance());
    }
    FinanceRequests.Profile onboardingDetails(){
        FinanceRequests.Profile p=new FinanceRequests.Profile();p.setEmail("alice@example.test");p.setPhone("+40000000000");p.setCountry("Romania");p.setOccupation("Designer");p.setDateOfBirth(java.time.LocalDate.of(1995,1,1));return p;
    }
    MockMultipartFile identityFile(){return new MockMultipartFile("file","identity.pdf","application/pdf","%PDF-1.4 identity".getBytes());}
    CustomerProfile verifyIdentity(long subject,String status){
        FinanceRequests.OnboardingDecision decision=new FinanceRequests.OnboardingDecision();decision.setDecision(status);decision.setNote("Identity reviewed by administrator");decision.setVersion(profiles.findByUserId(subject).get().getVersion());return onboarding.review(admin.getId(),subject,decision);
    }
    @Test void creditDraftUpdatesKeepOwnershipDocumentsAndApplicationId() throws Exception {
        for(long subject:new long[]{alice.getId(),bob.getId()}){
            finance.saveProfile(subject,onboardingDetails());finance.upload(subject,null,"IDENTITY",identityFile());verifyIdentity(subject,"VERIFIED");
        }
        FinanceRequests.Credit request=new FinanceRequests.Credit();request.setPurpose("Home office upgrade");request.setAmount(new BigDecimal("3000"));request.setMonthlyIncome(new BigDecimal("1500"));request.setTermMonths(12);
        CreditApplication draft=finance.createCredit(alice.getId(),request);
        CreditDocument identity=finance.upload(alice.getId(),draft.getId(),"IDENTITY",identityFile());finance.upload(alice.getId(),draft.getId(),"INCOME",identityFile());
        String payload="{\"purpose\":\"Home office equipment\",\"amount\":3500,\"monthlyIncome\":1800,\"termMonths\":18,\"userId\":"+bob.getId()+",\"status\":\"APPROVED\"}";
        mvc.perform(put("/api/finance/credit/"+draft.getId()).header("Authorization",auth(bobToken)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isNotFound());
        mvc.perform(put("/api/finance/credit/"+draft.getId()).header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload.replace("3500","0"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/finance/credit/"+draft.getId()).header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(draft.getId())).andExpect(jsonPath("$.userId").value(alice.getId())).andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.purpose").value("Home office equipment")).andExpect(jsonPath("$.amount").value(3500)).andExpect(jsonPath("$.monthlyIncome").value(1800)).andExpect(jsonPath("$.termMonths").value(18));
        assertEquals(1,credits.count());assertEquals(2,documents.findByApplicationIdAndUserId(draft.getId(),alice.getId()).size());assertEquals(draft.getId(),documents.findById(identity.getId()).get().getApplicationId());
        finance.submitCredit(alice.getId(),draft.getId());
        mvc.perform(put("/api/finance/credit/"+draft.getId()).header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
        assertEquals(1000,aliceBalance());
    }
    @Test void onboardingIsOneTimeOwnerScopedAndCannotSelfVerify() throws Exception {
        mvc.perform(get("/api/finance/onboarding").header("Authorization",auth(aliceToken)).param("userId",String.valueOf(bob.getId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.step").value("DETAILS"));
        String body="{\"email\":\"alice@example.test\",\"phone\":\"+40000000000\",\"country\":\"Romania\",\"occupation\":\"Designer\",\"dateOfBirth\":\"1995-01-01\",\"userId\":"+bob.getId()+",\"onboardingStatus\":\"VERIFIED\"}";
        mvc.perform(put("/api/finance/profile").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(alice.getId())).andExpect(jsonPath("$.onboardingStatus").value("PROFILE_COMPLETE"));
        assertFalse(profiles.findByUserId(bob.getId()).isPresent());
        mvc.perform(put("/api/finance/profile").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(body.replace("Designer","Changed"))).andExpect(status().isConflict());
        assertEquals("Designer",profiles.findByUserId(alice.getId()).get().getOccupation());
        mvc.perform(get("/api/finance/onboarding").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$.step").value("IDENTITY"));
        mvc.perform(multipart("/api/finance/documents").file(identityFile()).param("kind","INCOME").header("Authorization",auth(aliceToken))).andExpect(status().isBadRequest());
        CreditDocument doc=finance.upload(alice.getId(),null,"IDENTITY",identityFile());
        assertEquals("UNDER_REVIEW",profiles.findByUserId(alice.getId()).get().getOnboardingStatus());
        mvc.perform(get("/api/finance/onboarding").header("Authorization",auth(aliceToken))).andExpect(status().isConflict());
        mvc.perform(multipart("/api/finance/documents").file(identityFile()).param("kind","IDENTITY").header("Authorization",auth(aliceToken))).andExpect(status().isConflict());
        mvc.perform(get("/api/finance/overview").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$.onboardingAvailable").value(false)).andExpect(jsonPath("$.creditEligible").value(false));
        mvc.perform(get("/api/finance/documents/"+doc.getId()+"/download").header("Authorization",auth(bobToken))).andExpect(status().isNotFound());
        mvc.perform(get("/api/finance/documents/"+doc.getId()+"/download").header("Authorization",auth(adminToken))).andExpect(status().isOk());
        mvc.perform(get("/api/finance/onboarding/users/"+alice.getId()).header("Authorization",auth(bobToken)).header("userRole","Admin")).andExpect(status().isForbidden());
        mvc.perform(get("/api/finance/onboarding/reviews").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/finance/onboarding/users/"+alice.getId()+"/review").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"VERIFIED\",\"note\":\"Approve myself\",\"version\":1}")).andExpect(status().isForbidden());
        assertEquals(1,documents.count());
    }
    @Test void administratorCanApproveAndRevokeVerificationWithoutReopeningDetails() throws Exception {
        finance.saveProfile(alice.getId(),onboardingDetails());finance.upload(alice.getId(),null,"IDENTITY",identityFile());
        long version=profiles.findByUserId(alice.getId()).get().getVersion();
        mvc.perform(post("/api/finance/onboarding/users/"+alice.getId()+"/review").header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"VERIFIED\",\"note\":\"Identity confirmed\",\"version\":"+version+"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.onboardingStatus").value("VERIFIED")).andExpect(jsonPath("$.reviewedBy").value(admin.getId()));
        mvc.perform(get("/api/finance/overview").header("Authorization",auth(aliceToken))).andExpect(status().isOk()).andExpect(jsonPath("$.creditEligible").value(true)).andExpect(jsonPath("$.onboardingAvailable").value(false));
        mvc.perform(post("/api/finance/onboarding/users/"+alice.getId()+"/review").header("Authorization",auth(adminToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"DECLINED\",\"note\":\"Stale review rejected\",\"version\":"+version+"}" )).andExpect(status().isConflict());
        verifyIdentity(alice.getId(),"DECLINED");
        assertFalse((Boolean)finance.overview(alice.getId()).get("creditEligible"));
        assertThrows(RuntimeException.class,()->finance.saveProfile(alice.getId(),onboardingDetails()));
        assertThrows(RuntimeException.class,()->finance.upload(alice.getId(),null,"IDENTITY",identityFile()));
        verifyIdentity(alice.getId(),"VERIFIED");assertEquals("Designer",profiles.findByUserId(alice.getId()).get().getOccupation());
        // A stale JWT role or client-supplied role cannot keep administrator privileges after revocation.
        admin.setRole(UserRole.Analyst);users.save(admin);
        mvc.perform(get("/api/finance/onboarding/users/"+alice.getId()).header("Authorization",auth(adminToken))).andExpect(status().isForbidden());
    }
    @Test void creditMutationsRequireVerificationAndActiveStatusEveryTime() throws Exception {
        String payload="{\"amount\":3000,\"monthlyIncome\":1500,\"termMonths\":12,\"purpose\":\"Renovation\"}";
        mvc.perform(post("/api/finance/credit").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        finance.saveProfile(alice.getId(),onboardingDetails());finance.upload(alice.getId(),null,"IDENTITY",identityFile());verifyIdentity(alice.getId(),"VERIFIED");
        CreditApplication draft=new CreditApplication();draft.setUserId(alice.getId());draft.setAmount(new BigDecimal("3000"));draft.setMonthlyIncome(new BigDecimal("1500"));draft.setTermMonths(12);draft.setPurpose("Renovation");draft=credits.save(draft);
        finance.upload(alice.getId(),draft.getId(),"IDENTITY",identityFile());finance.upload(alice.getId(),draft.getId(),"INCOME",identityFile());
        for(AccountStatus blocked:new AccountStatus[]{AccountStatus.FREEZE,AccountStatus.SUSPENDED,AccountStatus.BLOCKED}){
            aliceAccount.setStatus(blocked);accounts.save(aliceAccount);
            mvc.perform(post("/api/finance/credit").header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
            mvc.perform(put("/api/finance/credit/"+draft.getId()).header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
            mvc.perform(post("/api/finance/credit/"+draft.getId()+"/submit").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
            mvc.perform(multipart("/api/finance/documents").file(identityFile()).param("kind","INCOME").param("applicationId",draft.getId().toString()).header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
            assertFalse((Boolean)finance.overview(alice.getId()).get("creditEligible"));
        }
        aliceAccount.setStatus(AccountStatus.ACTIVE);accounts.save(aliceAccount);verifyIdentity(alice.getId(),"DECLINED");
        mvc.perform(put("/api/finance/credit/"+draft.getId()).header("Authorization",auth(aliceToken)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(post("/api/finance/credit/"+draft.getId()+"/submit").header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/finance/documents").file(identityFile()).param("kind","INCOME").param("applicationId",draft.getId().toString()).header("Authorization",auth(aliceToken))).andExpect(status().isForbidden());
        verifyIdentity(alice.getId(),"VERIFIED");
        mvc.perform(get("/api/finance/credit/"+draft.getId()+"/documents").header("Authorization",auth(bobToken))).andExpect(status().isNotFound());
        mvc.perform(post("/api/finance/credit/"+draft.getId()+"/submit").header("Authorization",auth(aliceToken))).andExpect(status().isOk());
        assertEquals(1000,aliceBalance());assertEquals(3,documents.count());
    }
    @Test void concurrentIdentityUploadsOnlySubmitOneOwnedDocument() throws Exception {
        finance.saveProfile(alice.getId(),onboardingDetails());
        java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> upload=()->{start.await();try{finance.upload(alice.getId(),null,"IDENTITY",identityFile());return true;}catch(org.springframework.web.server.ResponseStatusException e){assertEquals(org.springframework.http.HttpStatus.CONFLICT,e.getStatus());return false;}};
        try{
            java.util.concurrent.Future<Boolean> first=workers.submit(upload),second=workers.submit(upload);start.countDown();
            assertNotEquals(first.get(10,java.util.concurrent.TimeUnit.SECONDS),second.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,documents.count());assertEquals("UNDER_REVIEW",profiles.findByUserId(alice.getId()).get().getOnboardingStatus());
        }finally{workers.shutdownNow();}
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
