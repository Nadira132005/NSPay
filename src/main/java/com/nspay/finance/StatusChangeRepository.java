package com.nspay.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface StatusChangeRepository extends JpaRepository<StatusChange,Long> {
    List<StatusChange> findByUserIdOrderByCreatedAtDesc(long id);
}
