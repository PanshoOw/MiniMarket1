package com.minimarket.dto;

import java.util.Date;

public class InventarioResponse {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private String tipoMovimiento;
    private Integer cantidad;
    private Date fechaMovimiento;

    public InventarioResponse() {
    }

    public InventarioResponse(Long id,
                              Long productoId,
                              String productoNombre,
                              String tipoMovimiento,
                              Integer cantidad,
                              Date fechaMovimiento) {
        this.id = id;
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public Date getFechaMovimiento() {
        return fechaMovimiento;
    }
}