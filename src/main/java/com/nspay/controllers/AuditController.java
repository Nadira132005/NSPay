package com.nspay.controllers;

import com.nspay.dto.AuditRequestDTO;
import com.nspay.entities.AuditModel;
import com.nspay.services.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name="Audit")
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping({"", "/"})
    @io.swagger.v3.oas.annotations.Operation(summary="Record an audit event", description="Records an action using the authenticated session identity.")
    public ResponseEntity<AuditModel> createAuditEntry(
            @Valid @RequestBody AuditRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(auditService.recordFromRequest(request, requestDTO.getAction()));
    }

    @GetMapping({"", "/"})
    @io.swagger.v3.oas.annotations.Operation(summary="Get audit history", description="Administrator or analyst only.")
    public ResponseEntity<List<AuditModel>> getAuditEntries(HttpServletRequest request) {
        return ResponseEntity.ok(auditService.getAll(request));
    }
}
