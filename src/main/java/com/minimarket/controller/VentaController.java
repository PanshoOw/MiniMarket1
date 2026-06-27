package com.minimarket.controller;

import com.minimarket.dto.DetalleVentaRequest;
import com.minimarket.dto.VentaRequest;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.service.VentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
@PreAuthorize("hasAnyAuthority('ROLE_GERENTE', 'ROLE_EMPLEADO')")
public class VentaController {

    private static final String ERROR_KEY = "error";

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public List<Venta> listarVentas() {
        return ventaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerVentaPorId(@PathVariable Long id) {
        Venta venta = ventaService.findById(id);

        if (venta == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(venta);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_EMPLEADO')")
    public ResponseEntity<Object> registrarVenta(@RequestBody(required = false) VentaRequest ventaRequest) {
        if (ventaRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos de la venta son obligatorios"));
        }

        try {
            Venta venta = convertirAEntidad(ventaRequest);
            Venta ventaRegistrada = ventaService.registrarVenta(venta);

            return ResponseEntity.ok(ventaRegistrada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    private Venta convertirAEntidad(VentaRequest request) {
        Venta venta = new Venta();

        if (request.getUsuarioId() != null) {
            Usuario usuario = new Usuario();
            usuario.setId(request.getUsuarioId());
            venta.setUsuario(usuario);
        }

        if (request.getDetalles() != null) {
            List<DetalleVenta> detalles = request.getDetalles()
                    .stream()
                    .map(this::convertirDetalleAEntidad)
                    .toList();

            venta.setDetalles(detalles);
        }

        return venta;
    }

    private DetalleVenta convertirDetalleAEntidad(DetalleVentaRequest request) {
        DetalleVenta detalleVenta = new DetalleVenta();

        if (request.getProductoId() != null) {
            Producto producto = new Producto();
            producto.setId(request.getProductoId());
            detalleVenta.setProducto(producto);
        }

        detalleVenta.setCantidad(request.getCantidad());
        detalleVenta.setPrecio(request.getPrecio());

        return detalleVenta;
    }
}