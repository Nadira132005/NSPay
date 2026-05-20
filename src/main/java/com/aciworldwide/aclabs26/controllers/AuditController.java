package com.aciworldwide.aclabs26.controllers;

import com.aciworldwide.aclabs26.dto.AuditRequestDTO;
import com.aciworldwide.aclabs26.entities.AuditModel;
import com.aciworldwide.aclabs26.services.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<AuditModel> createAuditEntry(
            @Valid @RequestBody AuditRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(auditService.recordFromRequest(request, requestDTO.getAction()));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<AuditModel>> getAuditEntries(HttpServletRequest request) {
        return ResponseEntity.ok(auditService.getAll(request));
    }
}
