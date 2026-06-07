package com.minimarket.controller;

import com.minimarket.dto.CarritoRequest;
import com.minimarket.dto.CarritoResponse;
import com.minimarket.entity.Carrito;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.CarritoService;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasAuthority('ROLE_CLIENTE')")
public class CarritoController {

    private static final String ERROR_KEY = "error";

    private final CarritoService carritoService;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    public CarritoController(CarritoService carritoService,
                             UsuarioRepository usuarioRepository,
                             ProductoRepository productoRepository) {
        this.carritoService = carritoService;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public List<CarritoResponse> listarCarrito() {
        return carritoService.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarritoResponse> obtenerCarritoPorId(@PathVariable Long id) {
        Carrito carrito = carritoService.findById(id);

        if (carrito == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertirAResponse(carrito));
    }

    @PostMapping
    public ResponseEntity<Object> agregarProductoAlCarrito(@RequestBody(required = false) CarritoRequest carritoRequest) {

        if (carritoRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del carrito son obligatorios"));
        }

        Long usuarioId = carritoRequest.getUsuarioId();
        Long productoId = carritoRequest.getProductoId();
        Integer cantidad = carritoRequest.getCantidad();

        if (usuarioId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El id del usuario es obligatorio"));
        }

        if (productoId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El id del producto es obligatorio"));
        }

        if (cantidad == null || cantidad <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La cantidad debe ser mayor a cero"));
        }

        Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuarioId);
        Optional<Producto> productoOptional = productoRepository.findById(productoId);

        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El usuario indicado no existe"));
        }

        if (productoOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El producto indicado no existe"));
        }

        Carrito carrito = new Carrito();

        // Se asignan entidades existentes a partir de los ID recibidos en el DTO.
        carrito.setUsuario(usuarioOptional.get());
        carrito.setProducto(productoOptional.get());
        carrito.setCantidad(cantidad);

        Carrito carritoGuardado = carritoService.save(carrito);

        return ResponseEntity.ok(convertirAResponse(carritoGuardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarCarrito(@PathVariable Long id,
                                                    @RequestBody(required = false) CarritoRequest carritoRequest) {

        Carrito carritoExistente = carritoService.findById(id);

        if (carritoExistente == null) {
            return ResponseEntity.notFound().build();
        }

        if (carritoRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del carrito son obligatorios"));
        }

        Long usuarioId = carritoRequest.getUsuarioId();
        Long productoId = carritoRequest.getProductoId();
        Integer cantidad = carritoRequest.getCantidad();

        if (usuarioId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El id del usuario es obligatorio"));
        }

        if (productoId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El id del producto es obligatorio"));
        }

        if (cantidad == null || cantidad <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La cantidad debe ser mayor a cero"));
        }

        Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuarioId);
        Optional<Producto> productoOptional = productoRepository.findById(productoId);

        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El usuario indicado no existe"));
        }

        if (productoOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El producto indicado no existe"));
        }

        carritoExistente.setUsuario(usuarioOptional.get());
        carritoExistente.setProducto(productoOptional.get());
        carritoExistente.setCantidad(cantidad);

        Carrito carritoActualizado = carritoService.save(carritoExistente);

        return ResponseEntity.ok(convertirAResponse(carritoActualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProductoDelCarrito(@PathVariable Long id) {
        Carrito carrito = carritoService.findById(id);

        if (carrito == null) {
            return ResponseEntity.notFound().build();
        }

        carritoService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private CarritoResponse convertirAResponse(Carrito carrito) {
        CarritoResponse response = new CarritoResponse();

        response.setId(carrito.getId());
        response.setCantidad(carrito.getCantidad());

        if (carrito.getUsuario() != null) {
            response.setUsuarioId(carrito.getUsuario().getId());
            response.setUsername(carrito.getUsuario().getUsername());
        }

        if (carrito.getProducto() != null) {
            response.setProductoId(carrito.getProducto().getId());
            response.setNombreProducto(carrito.getProducto().getNombre());
        }

        return response;
    }
}