package com.nspay.repository;

import com.nspay.entities.AuditModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditModel, Long> {
    List<AuditModel> findAllByOrderByTimestampDesc();
}
