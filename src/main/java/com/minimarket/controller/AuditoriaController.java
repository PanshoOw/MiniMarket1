package com.minimarket.controller;

import com.minimarket.entity.LogAuditoria;
import com.minimarket.service.AuditoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAuthority('ROLE_GERENTE')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<LogAuditoria> listarLogs() {
        return auditoriaService.listarTodos();
    }

    @GetMapping("/usuario/{username}")
    public ResponseEntity<List<LogAuditoria>> buscarPorUsuario(@PathVariable String username) {
        List<LogAuditoria> logs = auditoriaService.buscarPorUsername(username);
        if (logs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/accion/{accion}")
    public ResponseEntity<List<LogAuditoria>> buscarPorAccion(@PathVariable String accion) {
        List<LogAuditoria> logs = auditoriaService.buscarPorAccion(accion);
        if (logs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(logs);
    }
}
