package com.nspay.finance;

import java.math.BigDecimal;
import java.util.List;
import com.nspay.entities.TransactionModel;

/** Replace this interface's implementation with a trained model/provider when available. */
public interface FraudScoringEngine {
    RiskAssessment assess(long userId, String recipientIban, BigDecimal amount, List<TransactionModel> history);
}
