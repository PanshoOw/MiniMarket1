package com.minimarket.service;

import com.minimarket.entity.Inventario;

import java.util.List;

public interface InventarioService {

    String TIPO_ENTRADA = "ENTRADA";
    String TIPO_SALIDA = "SALIDA";

    List<Inventario> findAll();

    Inventario findById(Long id);

    Inventario save(Inventario inventario);

    void deleteById(Long id);

    List<Inventario> findByProductoId(Long productoId);

    boolean movimientoTieneDatosValidos(Inventario inventario);

    boolean inventarioTieneProductoValido(Inventario inventario);

    Inventario registrarMovimiento(Inventario inventario);
}