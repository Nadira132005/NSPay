package com.aciworldwide.aclabs26.repository;

import com.aciworldwide.aclabs26.entities.AuditModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditModel, Long> {
    List<AuditModel> findAllByOrderByTimestampDesc();
}
