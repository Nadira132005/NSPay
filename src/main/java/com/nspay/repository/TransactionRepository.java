package com.nspay.repository;

import com.nspay.entities.TransactionModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@Repository
public interface TransactionRepository extends CrudRepository<TransactionModel, Long> {

    List<TransactionModel> findAllByCardNumber(String cardNumber);
}
