package com.nspay.finance;

import com.nspay.entities.TransactionModel;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Explainable, history-fitted anomaly baseline; no claim of validated fraud prediction. */
@Component
public class LocalBehavioralModel implements FraudScoringEngine {
    @Override
    public RiskAssessment assess(long userId, String iban, BigDecimal amount, List<TransactionModel> history) {
        List<TransactionModel> outgoing = history.stream()
            .filter(t -> "00".equals(t.getReturnCode()) && !"TRANSFER_IN".equals(t.getTransactionType()))
            .filter(t -> t.getAmount() != null && Double.isFinite(t.getAmount()) && t.getAmount() > 0)
            .sorted(Comparator.comparing(TransactionModel::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(100).collect(Collectors.toList());
        List<String> reasons = new ArrayList<>();
        int score = 5;
        if (outgoing.size() >= 5) {
            double mean = outgoing.stream().mapToDouble(t -> Math.log1p(t.getAmount())).average().orElse(0);
            double variance = outgoing.stream().mapToDouble(t -> Math.pow(Math.log1p(t.getAmount()) - mean, 2)).average().orElse(0);
            double deviation = (Math.log1p(amount.doubleValue()) - mean) / Math.max(0.3, Math.sqrt(variance));
            if (deviation > 2) { score += deviation > 3 ? 45 : 25; reasons.add("Amount is unusually high compared with your recent payments"); }
        } else {
            reasons.add("Limited history: the behavioral model needs at least 5 successful payments");
        }
        if (amount.compareTo(new BigDecimal("2000")) >= 0) { score += 25; reasons.add("Large demo transfer (at least EUR 2,000)"); }
        boolean known = outgoing.stream().anyMatch(t -> iban.equals(t.getCounterpartyIban()));
        if (!known) { score += 15; reasons.add("First transfer to this recipient"); }
        long recent = outgoing.stream().filter(t -> t.getTimestamp() != null &&
            t.getTimestamp().toInstant().isAfter(Instant.now().minusSeconds(600))).count();
        if (recent >= 3) { score += 30; reasons.add("Several payments within the last 10 minutes"); }
        if (reasons.isEmpty()) reasons.add("No elevated signals in the local demo model");
        RiskAssessment result = new RiskAssessment();
        result.setUserId(userId); result.setRecipientIban(iban); result.setAmount(amount);
        result.setScore(Math.min(100, score));
        result.setLevel(score >= 60 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW");
        result.setReasons(String.join("; ", reasons)); result.setHistorySamples(outgoing.size());
        return result;
    }
}
