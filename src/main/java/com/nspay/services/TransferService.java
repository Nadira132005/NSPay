package com.nspay.services;

import com.nspay.dto.TransferRequestDTO;
import com.nspay.entities.AccountModel;
import com.nspay.entities.AccountStatus;
import com.nspay.entities.TransactionModel;
import com.nspay.entities.UserModel;
import com.nspay.repository.AccountRepository;
import com.nspay.repository.TransactionRepository;
import com.nspay.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final com.nspay.finance.FraudService fraudService;

    public TransferService(AccountRepository accountRepository, UserRepository userRepository,
                           TransactionRepository transactionRepository, com.nspay.finance.FraudService fraudService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.fraudService = fraudService;
    }

    @Transactional
    public TransferReceipt transfer(long userId, TransferRequestDTO request) {
        UserModel user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Signed-in user was not found"));
        if (user.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No account is assigned to your user");
        }

        AccountModel recipientLookup = accountRepository.findByIban(
                request.getRecipientIban().replaceAll("\\s+", "").toUpperCase());
        if (recipientLookup == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient IBAN was not found");
        }
        if (user.getAccountId() == recipientLookup.getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an IBAN different from your own");
        }
        // Lock accounts in ID order so simultaneous transfers in opposite directions cannot deadlock.
        AccountModel sender;
        AccountModel recipient;
        if (user.getAccountId() < recipientLookup.getId()) {
            sender = accountRepository.lockById(user.getAccountId());
            recipient = accountRepository.lockById(recipientLookup.getId());
        } else {
            recipient = accountRepository.lockById(recipientLookup.getId());
            sender = accountRepository.lockById(user.getAccountId());
        }
        if (sender.getStatus() != AccountStatus.ACTIVE || recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both accounts must be active");
        }

        double amount = Math.round(request.getAmount() * 100.0) / 100.0;
        if (!Double.isFinite(amount) || amount <= 0 || sender.getAmount() < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
        if (sender.getDailyTx() >= sender.getDailyTxLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily transfer count limit exceeded");
        }
        if (sender.getDailyTxSum() + amount > sender.getDailyTxSumLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily transfer amount limit exceeded");
        }

        // Demo monitoring only: risk assessments are explainable review signals, not a fraud verdict.
        fraudService.assess(userId, recipient.getIban(), java.math.BigDecimal.valueOf(amount));
        sender.setAmount(Math.round((sender.getAmount() - amount) * 100.0) / 100.0);
        sender.setDailyTx(sender.getDailyTx() + 1);
        sender.setDailyTxSum(sender.getDailyTxSum() + amount);
        recipient.setAmount(Math.round((recipient.getAmount() + amount) * 100.0) / 100.0);
        accountRepository.save(sender);
        accountRepository.save(recipient);

        Timestamp now = Timestamp.from(Instant.now());
        transactionRepository.save(makeTransaction(sender, amount, now, "TRANSFER_OUT", recipient.getIban()));
        transactionRepository.save(makeTransaction(recipient, amount, now, "TRANSFER_IN", sender.getIban()));
        return new TransferReceipt(sender.getIban(), recipient.getIban(), amount, sender.getAmount());
    }

    private TransactionModel makeTransaction(AccountModel account, double amount, Timestamp timestamp,
                                             String type, String counterpartyIban) {
        TransactionModel transaction = new TransactionModel();
        transaction.setCardNumber(account.getCardNumber());
        transaction.setAmount(amount);
        transaction.setTimestamp(timestamp);
        transaction.setReturnCode("00");
        transaction.setTransactionType(type);
        transaction.setCounterpartyIban(counterpartyIban);
        return transaction;
    }

    public static class TransferReceipt {
        private final String senderIban;
        private final String recipientIban;
        private final double amount;
        private final double remainingBalance;

        public TransferReceipt(String senderIban, String recipientIban, double amount, double remainingBalance) {
            this.senderIban = senderIban;
            this.recipientIban = recipientIban;
            this.amount = amount;
            this.remainingBalance = remainingBalance;
        }
        public String getSenderIban() { return senderIban; }
        public String getRecipientIban() { return recipientIban; }
        public double getAmount() { return amount; }
        public double getRemainingBalance() { return remainingBalance; }
    }
}
