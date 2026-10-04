package com.nspay.services;
import com.nspay.entities.*;
import com.nspay.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.schedules.enabled=false"})
class DailyResetServiceTest {
 @Autowired AccountRepository accounts;
 @Autowired DailyResetService reset;
 @Test void resetPreservesFrozenStatusAndBalance() {
  AccountModel account=new AccountModel();account.setCardNumber("reset-card");account.setStatus(AccountStatus.FREEZE);account.setAmount(123.45);account.setDailyTx(5);account.setDailyTxSum(100);account=accounts.save(account);
  reset.resetDailyCounters();AccountModel result=accounts.findById(account.getId()).get();assertEquals(0,result.getDailyTx());assertEquals(0,result.getDailyTxSum());assertEquals(123.45,result.getAmount());assertEquals(AccountStatus.FREEZE,result.getStatus());accounts.deleteById(account.getId());
 }
}
