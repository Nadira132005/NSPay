package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.TransactionModel;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<TransactionModel,Long> {

    List<TransactionModel> findAllByCardNumber(String cardNumber);

}
