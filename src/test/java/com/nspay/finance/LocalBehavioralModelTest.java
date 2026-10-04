package com.nspay.finance;
import org.junit.jupiter.api.Test;
import com.nspay.entities.TransactionModel;
import java.util.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
class LocalBehavioralModelTest {
 @Test void modelExplainsColdStartAndHighDeviation() {
  LocalBehavioralModel model=new LocalBehavioralModel();
  RiskAssessment cold=model.assess(1,"DEMO",new BigDecimal("10"),Collections.emptyList());
  assertEquals(0,cold.getHistorySamples());assertTrue(cold.getReasons().contains("Limited history"));
  List<TransactionModel> history=new ArrayList<>();
  for(int i=0;i<8;i++) history.add(new TransactionModel(i,"4111111111111111",10.0,Timestamp.from(Instant.now().minusSeconds(1000)),"00"));
  RiskAssessment unusual=model.assess(1,"NEW",new BigDecimal("5000"),history);
  assertEquals("HIGH",unusual.getLevel());assertTrue(unusual.getReasons().contains("unusually high"));assertEquals(8,unusual.getHistorySamples());
 }
}
