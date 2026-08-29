package com.clothing.ai.audit.repository;

import com.clothing.ai.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByActorId(String actorId, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
}
