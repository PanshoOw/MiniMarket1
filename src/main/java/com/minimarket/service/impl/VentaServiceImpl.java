package com.minimarket.service.impl;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import com.minimarket.service.VentaService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            UsuarioRepository usuarioRepository,
                            ProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<Venta> findAll() {
        return ventaRepository.findAll();
    }

    @Override
    public Venta findById(Long id) {
        if (id == null) {
            return null;
        }

        return ventaRepository.findById(id).orElse(null);
    }

    @Override
    public Venta save(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException("La venta no puede ser nula");
        }

        return ventaRepository.save(venta);
    }

    @Override
    public List<Venta> findByUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }

        return ventaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public boolean ventaTieneUsuarioValido(Venta venta) {
        if (venta == null) {
            return false;
        }

        Usuario usuario = venta.getUsuario();

        if (usuario == null) {
            return false;
        }

        Long usuarioId = usuario.getId();

        if (usuarioId == null) {
            return false;
        }

        return usuarioRepository.findById(usuarioId).isPresent();
    }

    @Override
    public boolean ventaTieneStockSuficiente(Venta venta) {
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return false;
        }

        return venta.getDetalles()
                .stream()
                .allMatch(this::detalleTieneStockSuficiente);
    }

    @Override
    public double calcularTotalVenta(Venta venta) {
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return 0.0;
        }

        return venta.getDetalles()
                .stream()
                .mapToDouble(this::calcularSubtotalDetalle)
                .sum();
    }

    @Override
    public Venta registrarVenta(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException("La venta no puede ser nula");
        }

        if (!ventaTieneUsuarioValido(venta)) {
            throw new IllegalArgumentException("La venta debe estar asociada a un usuario válido");
        }

        if (!ventaTieneStockSuficiente(venta)) {
            throw new IllegalArgumentException("No existe stock suficiente para registrar la venta");
        }

        if (venta.getFecha() == null) {
            venta.setFecha(new Date());
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            prepararDetalleVenta(venta, detalle);
            descontarStock(detalle);
        }

        return ventaRepository.save(venta);
    }

    private boolean detalleTieneStockSuficiente(DetalleVenta detalle) {
        if (detalle == null || detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            return false;
        }

        Long productoId = obtenerProductoId(detalle);

        if (productoId == null) {
            return false;
        }

        Optional<Producto> productoOptional = productoRepository.findById(productoId);

        if (productoOptional.isEmpty()) {
            return false;
        }

        Producto producto = productoOptional.get();
        Integer stockDisponible = producto.getStock();

        return stockDisponible != null && stockDisponible >= detalle.getCantidad();
    }

    private double calcularSubtotalDetalle(DetalleVenta detalle) {
        if (detalle == null || detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            return 0.0;
        }

        Double precio = obtenerPrecioDetalle(detalle);

        if (precio == null || precio <= 0) {
            return 0.0;
        }

        return precio * detalle.getCantidad();
    }

    private Double obtenerPrecioDetalle(DetalleVenta detalle) {
        if (detalle.getPrecio() != null) {
            return detalle.getPrecio();
        }

        Producto producto = detalle.getProducto();

        if (producto != null) {
            return producto.getPrecio();
        }

        return null;
    }

    private void prepararDetalleVenta(Venta venta, DetalleVenta detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle de venta no puede ser nulo");
        }

        detalle.setVenta(venta);

        if (detalle.getPrecio() == null && detalle.getProducto() != null) {
            detalle.setPrecio(detalle.getProducto().getPrecio());
        }
    }

    private void descontarStock(DetalleVenta detalle) {
        Long productoId = obtenerProductoId(detalle);

        if (productoId == null) {
            throw new IllegalArgumentException("El detalle debe contener un producto válido");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto indicado no existe"));

        Integer stockActual = producto.getStock();
        Integer cantidadVendida = detalle.getCantidad();

        if (stockActual == null || cantidadVendida == null || cantidadVendida <= 0) {
            throw new IllegalArgumentException("Los datos de stock o cantidad no son válidos");
        }

        producto.setStock(stockActual - cantidadVendida);

        productoRepository.save(producto);
    }

    private Long obtenerProductoId(DetalleVenta detalle) {
        if (detalle == null) {
            return null;
        }

        Producto producto = detalle.getProducto();

        if (producto == null) {
            return null;
        }

        return producto.getId();
    }
}