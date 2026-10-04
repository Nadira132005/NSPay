package com.nspay.mappers;

import com.nspay.dto.TransactionDTO;
import com.nspay.entities.TransactionModel;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionModel toModel(TransactionDTO transactionDTO) {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setCardNumber(transactionDTO.getCardNumber());
        transactionModel.setAmount(transactionDTO.getAmount());
        transactionModel.setTimestamp(transactionDTO.getTimestamp());
        return transactionModel;
    }

    public TransactionDTO toDto(TransactionModel transactionModel) {
        return new TransactionDTO(
                transactionModel.getCardNumber(),
                transactionModel.getAmount(),
                transactionModel.getTimestamp()
        );
    }
}
