package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.TransactionModel;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<TransactionModel,Long> {
}
