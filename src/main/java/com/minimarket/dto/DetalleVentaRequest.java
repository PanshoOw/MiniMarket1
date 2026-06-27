package com.minimarket.dto;

public class DetalleVentaRequest {

    private Long productoId;
    private Integer cantidad;
    private Double precio;

    public DetalleVentaRequest() {
    }

    public DetalleVentaRequest(Long productoId, Integer cantidad, Double precio) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
}