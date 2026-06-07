package com.minimarket.service;

import com.minimarket.entity.LogAuditoria;

import java.util.List;

public interface AuditoriaService {

    void registrarEvento(String username, String accion, String ipAddress, String detalle);

    List<LogAuditoria> listarTodos();

    List<LogAuditoria> buscarPorUsername(String username);

    List<LogAuditoria> buscarPorAccion(String accion);
}
