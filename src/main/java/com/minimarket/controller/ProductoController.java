package com.minimarket.controller;

import com.minimarket.dto.ProductoRequest;
import com.minimarket.dto.ProductoResponse;
import com.minimarket.entity.Categoria;
import com.minimarket.entity.Producto;
import com.minimarket.service.CategoriaService;
import com.minimarket.service.ProductoService;
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
@RequestMapping("/api/productos")
public class ProductoController {

    private static final String ERROR_KEY = "error";
    private static final String ROLE_GERENTE = "ROLE_GERENTE";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";
    private static final String ROLE_EMPLEADO = "ROLE_EMPLEADO";

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoController(ProductoService productoService,
                              CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_CLIENTE + "', '" + ROLE_EMPLEADO + "')")
    public List<ProductoResponse> listarProductos() {
        return productoService.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_CLIENTE + "', '" + ROLE_EMPLEADO + "')")
    public ResponseEntity<ProductoResponse> obtenerProductoPorId(@PathVariable Long id) {
        Producto producto = productoService.findById(id);

        if (producto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertirAResponse(producto));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_EMPLEADO + "')")
    public ResponseEntity<Object> guardarProducto(@RequestBody ProductoRequest productoRequest) {

        if (productoRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del producto son obligatorios"));
        }

        String nombre = productoRequest.getNombre();
        Double precio = productoRequest.getPrecio();
        Integer stock = productoRequest.getStock();
        Long categoriaId = productoRequest.getCategoriaId();

        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre del producto es obligatorio"));
        }

        if (contieneContenidoPeligroso(nombre)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre del producto contiene caracteres no permitidos"));
        }

        if (precio == null || precio.isNaN() || precio <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El precio debe ser mayor a cero"));
        }

        if (stock == null || stock < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El stock no puede ser negativo"));
        }

        if (categoriaId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El id de la categoría es obligatorio"));
        }

        Categoria categoria = categoriaService.findById(categoriaId);

        if (categoria == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La categoría indicada no existe"));
        }

        Producto producto = new Producto();
        producto.setNombre(nombre.trim());
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(categoria);

        Producto productoGuardado = productoService.save(producto);

        return ResponseEntity.ok(convertirAResponse(productoGuardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_EMPLEADO + "')")
    public ResponseEntity<Object> actualizarProducto(@PathVariable Long id,
                                                     @RequestBody ProductoRequest productoRequest) {

        Producto productoExistente = productoService.findById(id);

        if (productoExistente == null) {
            return ResponseEntity.notFound().build();
        }

        if (productoRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del producto son obligatorios"));
        }

        String nombre = productoRequest.getNombre();
        Double precio = productoRequest.getPrecio();
        Integer stock = productoRequest.getStock();
        Long categoriaId = productoRequest.getCategoriaId();

        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre del producto es obligatorio"));
        }

        if (contieneContenidoPeligroso(nombre)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre del producto contiene caracteres no permitidos"));
        }

        if (precio == null || precio.isNaN() || precio <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El precio debe ser mayor a cero"));
        }

        if (stock == null || stock < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El stock no puede ser negativo"));
        }

        if (categoriaId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El id de la categoría es obligatorio"));
        }

        Categoria categoria = categoriaService.findById(categoriaId);

        if (categoria == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La categoría indicada no existe"));
        }

        productoExistente.setNombre(nombre.trim());
        productoExistente.setPrecio(precio);
        productoExistente.setStock(stock);
        productoExistente.setCategoria(categoria);

        Producto productoActualizado = productoService.save(productoExistente);

        return ResponseEntity.ok(convertirAResponse(productoActualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + ROLE_GERENTE + "')")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        Producto producto = productoService.findById(id);

        if (producto == null) {
            return ResponseEntity.notFound().build();
        }

        productoService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private boolean contieneContenidoPeligroso(String texto) {
        String textoNormalizado = texto.toLowerCase();

        return textoNormalizado.contains("<script")
                || textoNormalizado.contains("</script>")
                || textoNormalizado.contains("javascript:")
                || textoNormalizado.contains("onerror")
                || textoNormalizado.contains("onload")
                || textoNormalizado.contains("<")
                || textoNormalizado.contains(">");
    }

    private ProductoResponse convertirAResponse(Producto producto) {
        ProductoResponse response = new ProductoResponse();

        response.setId(producto.getId());
        response.setNombre(producto.getNombre());
        response.setPrecio(producto.getPrecio());
        response.setStock(producto.getStock());

        if (producto.getCategoria() != null) {
            response.setCategoriaId(producto.getCategoria().getId());
            response.setCategoriaNombre(producto.getCategoria().getNombre());
        }

        return response;
    }
}