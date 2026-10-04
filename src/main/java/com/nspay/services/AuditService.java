package com.nspay.services;

import com.nspay.entities.AuditModel;
import com.nspay.repository.AuditRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;

@Service
public class AuditService {

    private static final Logger logger = LogManager.getLogger(AuditService.class);

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public AuditModel record(String user, String action) {
        if (user == null || user.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user is not available");
        }
        if (action == null || action.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action is required");
        }

        AuditModel auditModel = new AuditModel();
        auditModel.setUser(user.trim());
        auditModel.setActiune(action.trim());
        auditModel.setTimestamp(new Timestamp(System.currentTimeMillis()));

        AuditModel saved = auditRepository.save(auditModel);
        logger.info("AUDIT user={} action={} timestamp={}", saved.getUser(), saved.getActiune(), saved.getTimestamp());
        return saved;
    }

    public AuditModel recordFromRequest(HttpServletRequest request, String action) {
        String currentUser = (String) request.getAttribute("authenticatedName");
        return record(currentUser, action);
    }

    public List<AuditModel> getAll(HttpServletRequest request) {
        assertAnalystOrAdmin(request);
        return auditRepository.findAllByOrderByTimestampDesc();
    }

    private void assertAnalystOrAdmin(HttpServletRequest request) {
        String currentRole = (String) request.getAttribute("authenticatedRole");
        if (!"Admin".equals(currentRole) && !"Analyst".equals(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin or Analyst can view audit logs");
        }
    }
}
