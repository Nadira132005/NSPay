package com.nspay.validators;

import com.nspay.dto.AccountDTO;
import com.nspay.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountValidator {

    private final AccountRepository accountRepository;

    public boolean validateAccount(AccountDTO accountDTO) {
        return accountDTO != null && accountDTO.getCardNumber() != null && !accountRepository.existsByCardNumber(accountDTO.getCardNumber());
    }
}
