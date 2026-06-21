package com.minimarket.controller;

import com.minimarket.dto.InventarioRequest;
import com.minimarket.dto.InventarioResponse;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.service.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasAnyAuthority('ROLE_GERENTE', 'ROLE_EMPLEADO')")
public class InventarioController {

    private static final String ERROR_KEY = "error";

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public List<InventarioResponse> listarMovimientosDeInventario() {
        return inventarioService.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventarioResponse> obtenerMovimientoPorId(@PathVariable Long id) {
        Inventario inventario = inventarioService.findById(id);

        if (inventario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertirAResponse(inventario));
    }

    @PostMapping
    public ResponseEntity<Object> registrarMovimiento(
            @RequestBody(required = false) InventarioRequest inventarioRequest) {

        if (inventarioRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del movimiento de inventario son obligatorios"));
        }

        try {
            Inventario inventario = convertirAEntidad(inventarioRequest);
            Inventario inventarioRegistrado = inventarioService.registrarMovimiento(inventario);

            return ResponseEntity.ok(convertirAResponse(inventarioRegistrado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarMovimiento(
            @PathVariable Long id,
            @RequestBody(required = false) InventarioRequest inventarioRequest) {

        Inventario existente = inventarioService.findById(id);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        if (inventarioRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del movimiento de inventario son obligatorios"));
        }

        try {
            Inventario inventario = convertirAEntidad(inventarioRequest);
            inventario.setId(id);

            Inventario inventarioActualizado = inventarioService.registrarMovimiento(inventario);

            return ResponseEntity.ok(convertirAResponse(inventarioActualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMovimiento(@PathVariable Long id) {
        Inventario inventario = inventarioService.findById(id);

        if (inventario == null) {
            return ResponseEntity.notFound().build();
        }

        inventarioService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private Inventario convertirAEntidad(InventarioRequest request) {
        Inventario inventario = new Inventario();
        inventario.setTipoMovimiento(request.getTipoMovimiento());
        inventario.setCantidad(request.getCantidad());

        if (request.getProductoId() != null) {
            Producto producto = new Producto();
            producto.setId(request.getProductoId());
            inventario.setProducto(producto);
        }

        return inventario;
    }

    private InventarioResponse convertirAResponse(Inventario inventario) {
        Long productoId = null;
        String productoNombre = null;

        if (inventario.getProducto() != null) {
            productoId = inventario.getProducto().getId();
            productoNombre = inventario.getProducto().getNombre();
        }

        return new InventarioResponse(
                inventario.getId(),
                productoId,
                productoNombre,
                inventario.getTipoMovimiento(),
                inventario.getCantidad(),
                inventario.getFechaMovimiento()
        );
    }
}