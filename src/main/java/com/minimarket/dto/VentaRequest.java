package com.minimarket.dto;

import java.util.List;

public class VentaRequest {

    private Long usuarioId;
    private List<DetalleVentaRequest> detalles;

    public VentaRequest() {
    }

    public VentaRequest(Long usuarioId, List<DetalleVentaRequest> detalles) {
        this.usuarioId = usuarioId;
        this.detalles = detalles;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<DetalleVentaRequest> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVentaRequest> detalles) {
        this.detalles = detalles;
    }
}