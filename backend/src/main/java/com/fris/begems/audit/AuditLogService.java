package com.fris.begems.audit;

import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(AppUserPrincipal actor, AuditAction action, AuditEntityType entityType, UUID entityId,
            String summary) {
        String actorName = userRepository.findById(actor.getUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(actor.getUsername());
        AuditLog log = AuditLog.create(actor.getOrganizationId(), actor.getUserId(), actorName, action, entityType,
                entityId, summary);
        auditLogRepository.save(log);
    }
}
