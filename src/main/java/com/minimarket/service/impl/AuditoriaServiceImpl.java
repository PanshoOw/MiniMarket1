package com.minimarket.service.impl;

import com.minimarket.entity.LogAuditoria;
import com.minimarket.repository.LogAuditoriaRepository;
import com.minimarket.service.AuditoriaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public AuditoriaServiceImpl(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    @Override
    public void registrarEvento(String username, String accion, String ipAddress, String detalle) {
        LogAuditoria log = new LogAuditoria(username, accion, ipAddress, detalle);
        logAuditoriaRepository.save(log);
    }

    @Override
    public List<LogAuditoria> listarTodos() {
        return logAuditoriaRepository.findByOrderByFechaHoraDesc();
    }

    @Override
    public List<LogAuditoria> buscarPorUsername(String username) {
        return logAuditoriaRepository.findByUsername(username);
    }

    @Override
    public List<LogAuditoria> buscarPorAccion(String accion) {
        return logAuditoriaRepository.findByAccion(accion);
    }
}
