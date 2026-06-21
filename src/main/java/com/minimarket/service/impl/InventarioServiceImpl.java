package com.minimarket.service.impl;

import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.service.InventarioService;
import org.springframework.stereotype.Service;
import java.util.Objects;

import java.util.Date;
import java.util.List;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;

    public InventarioServiceImpl(InventarioRepository inventarioRepository,
                                 ProductoRepository productoRepository) {
        this.inventarioRepository = inventarioRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<Inventario> findAll() {
        return inventarioRepository.findAll();
    }

    @Override
    public Inventario findById(Long id) {
        if (id == null) {
            return null;
        }

        return inventarioRepository.findById(id).orElse(null);
    }

    @Override
    public Inventario save(Inventario inventario) {
        if (inventario == null) {
            throw new IllegalArgumentException("El movimiento de inventario no puede ser nulo");
        }

        return inventarioRepository.save(inventario);
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del movimiento de inventario no puede ser nulo");
        }

        inventarioRepository.deleteById(id);
    }

    @Override
    public List<Inventario> findByProductoId(Long productoId) {
        if (productoId == null) {
            return List.of();
        }

        return inventarioRepository.findByProductoId(productoId);
    }

    @Override
    public boolean movimientoTieneDatosValidos(Inventario inventario) {
        if (inventario == null) {
            return false;
        }

        return tipoMovimientoValido(inventario.getTipoMovimiento())
                && inventario.getCantidad() != null
                && inventario.getCantidad() > 0;
    }

    @Override
    public boolean inventarioTieneProductoValido(Inventario inventario) {
        Long productoId = obtenerProductoId(inventario);

        if (productoId == null) {
            return false;
        }

        return productoRepository.findById(productoId).isPresent();
    }

    @Override
    public Inventario registrarMovimiento(Inventario inventario) {
        if (inventario == null) {
            throw new IllegalArgumentException("El movimiento de inventario no puede ser nulo");
        }

        if (!movimientoTieneDatosValidos(inventario)) {
            throw new IllegalArgumentException("El tipo de movimiento y la cantidad son obligatorios y válidos");
        }

        Long productoId = obtenerProductoId(inventario);

        if (productoId == null) {
            throw new IllegalArgumentException("El movimiento debe estar asociado a un producto válido");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto asociado no existe"));

        aplicarMovimientoStock(producto, inventario);

        if (inventario.getFechaMovimiento() == null) {
            inventario.setFechaMovimiento(new Date());
        }

        inventario.setProducto(producto);
        productoRepository.save(Objects.requireNonNull(producto, "El producto asociado no puede ser nulo"));

        return inventarioRepository.save(inventario);
    }

    private void aplicarMovimientoStock(Producto producto, Inventario inventario) {
        Integer stockActual = producto.getStock();
        Integer cantidadMovimiento = inventario.getCantidad();

        if (stockActual == null) {
            throw new IllegalArgumentException("El producto no tiene stock definido");
        }

        if (TIPO_ENTRADA.equalsIgnoreCase(inventario.getTipoMovimiento())) {
            producto.setStock(stockActual + cantidadMovimiento);
            return;
        }

        if (TIPO_SALIDA.equalsIgnoreCase(inventario.getTipoMovimiento())) {
            validarStockSuficiente(stockActual, cantidadMovimiento);
            producto.setStock(stockActual - cantidadMovimiento);
            return;
        }

        throw new IllegalArgumentException("Tipo de movimiento no válido");
    }

    private void validarStockSuficiente(Integer stockActual, Integer cantidadMovimiento) {
        if (stockActual < cantidadMovimiento) {
            throw new IllegalArgumentException("No existe stock suficiente para registrar la salida");
        }
    }

    private boolean tipoMovimientoValido(String tipoMovimiento) {
        if (tipoMovimiento == null || tipoMovimiento.trim().isEmpty()) {
            return false;
        }

        return TIPO_ENTRADA.equalsIgnoreCase(tipoMovimiento.trim())
                || TIPO_SALIDA.equalsIgnoreCase(tipoMovimiento.trim());
    }

    private Long obtenerProductoId(Inventario inventario) {
        if (inventario == null || inventario.getProducto() == null) {
            return null;
        }

        return inventario.getProducto().getId();
    }
}