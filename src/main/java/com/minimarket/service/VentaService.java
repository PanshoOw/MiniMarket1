package com.minimarket.service;

import com.minimarket.entity.Venta;

import java.util.List;

public interface VentaService {

    List<Venta> findAll();

    Venta findById(Long id);

    Venta save(Venta venta);

    List<Venta> findByUsuarioId(Long usuarioId);

    boolean ventaTieneUsuarioValido(Venta venta);

    boolean ventaTieneStockSuficiente(Venta venta);

    double calcularTotalVenta(Venta venta);

    Venta registrarVenta(Venta venta);
}