package com.minimarket.repository;

import com.minimarket.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findByUsername(String username);

    List<LogAuditoria> findByAccion(String accion);

    List<LogAuditoria> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    List<LogAuditoria> findByOrderByFechaHoraDesc();
}
