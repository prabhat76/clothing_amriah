package com.clothing.ai.audit.service;

import com.clothing.ai.audit.entity.AuditLog;
import com.clothing.ai.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Async("taskExecutor")
    @Transactional
    public void record(String actorId, String action, String entityType, String entityId, String description) {
        repository.save(AuditLog.builder()
                .actorId(actorId).action(action)
                .entityType(entityType).entityId(entityId)
                .description(description).build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public String exportCsv(String action, String entityType) {
        var logs = repository.findAll(PageRequest.of(0, 10_000)).getContent().stream()
                .filter(l -> action == null || action.equalsIgnoreCase(l.getAction()))
                .filter(l -> entityType == null || entityType.equalsIgnoreCase(l.getEntityType()))
                .toList();
        StringBuilder sb = new StringBuilder("actorId,action,entityType,entityId,description,createdAt\n");
        for (AuditLog l : logs) {
            sb.append(csv(l.getActorId())).append(',')
              .append(csv(l.getAction())).append(',')
              .append(csv(l.getEntityType())).append(',')
              .append(csv(l.getEntityId())).append(',')
              .append(csv(l.getDescription())).append(',')
              .append(l.getCreatedAt()).append('\n');
        }
        return sb.toString();
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
