package com.aciworldwide.aclabs26.services;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.entities.AccountModel;
import com.aciworldwide.aclabs26.repository.AccountRepository;
import com.aciworldwide.aclabs26.validators.AccountValidator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountValidator accountValidator;

    public ResponseEntity<?> getAccount(String cardNumber) {
        AccountModel account = accountRepository.findAccountByCardNumber(cardNumber);
        if (account == null) {
            AccountDTO response = new AccountDTO();
            response.setStatus("Account not found.");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new AccountDTO(account), new HttpHeaders(), HttpStatus.OK);
    }


    public ResponseEntity<?> addAccount(AccountDTO accountDTO) {
        AccountDTO response = new AccountDTO();

        if (!accountValidator.validateAccount(accountDTO)) {
            response.setStatus("Account already exists.");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        response = new AccountDTO(accountRepository.save(new AccountModel(accountDTO)));
        return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> updateAccount(String cardNumber, AccountDTO updatedAccountDTO) {
        AccountDTO response = new AccountDTO();

        AccountModel account = accountRepository.findAccountByCardNumber(cardNumber);
        if (account == null) {
            response.setStatus("account not exists");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        boolean changes = false;

        if (updatedAccountDTO.getAmount() != null && !updatedAccountDTO.getAmount().equals(account.getAmount())) {
            account.setAmount(updatedAccountDTO.getAmount());
            changes = true;
        }

        if (!changes) {
            response.setStatus("No data changes found");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        AccountModel updatedAccount = new AccountModel(updatedAccountDTO);
        updatedAccount.setId(account.getId());

        response = new AccountDTO(accountRepository.save(updatedAccount));
        response.setStatus("Account Updated");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> deleteAccount(AccountDTO accountDTO) {
        AccountDTO response = new AccountDTO();
        AccountModel account = accountRepository.findAccountByCardNumber(accountDTO.getCardNumber());
        if (account == null) {
            response.setStatus("account not exists");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }
        accountRepository.delete(account);
        response.setStatus("account deleted");
        return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);

    }

    public List<AccountModel> getAllAccounts() {
        List<AccountModel> accounts = (List<AccountModel>) accountRepository.findAll();
        return accounts;
    }

}
