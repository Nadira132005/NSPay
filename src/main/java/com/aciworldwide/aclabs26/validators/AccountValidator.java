package com.aciworldwide.aclabs26.validators;

import com.aciworldwide.aclabs26.dto.AccountDTO;
import com.aciworldwide.aclabs26.repository.AccountRepository;
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
