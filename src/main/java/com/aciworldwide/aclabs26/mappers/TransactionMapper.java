package com.aciworldwide.aclabs26.mappers;

import com.aciworldwide.aclabs26.dto.TransactionDTO;
import com.aciworldwide.aclabs26.entities.TransactionModel;
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
