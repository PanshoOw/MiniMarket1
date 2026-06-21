package com.minimarket.controller;

import com.minimarket.dto.CategoriaRequest;
import com.minimarket.dto.CategoriaResponse;
import com.minimarket.entity.Categoria;
import com.minimarket.service.CategoriaService;
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
@RequestMapping("/api/categorias")
public class CategoriaController {

    private static final String ERROR_KEY = "error";
    private static final String ROLE_GERENTE = "ROLE_GERENTE";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";
    private static final String ROLE_EMPLEADO = "ROLE_EMPLEADO";

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_CLIENTE + "', '" + ROLE_EMPLEADO + "')")
    public List<CategoriaResponse> listarCategorias() {
        return categoriaService.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_CLIENTE + "', '" + ROLE_EMPLEADO + "')")
    public ResponseEntity<CategoriaResponse> obtenerCategoriaPorId(@PathVariable Long id) {
        Categoria categoria = categoriaService.findById(id);

        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertirAResponse(categoria));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_EMPLEADO + "')")
    public ResponseEntity<Object> guardarCategoria(
            @RequestBody(required = false) CategoriaRequest categoriaRequest) {

        String nombre = obtenerNombreValidado(categoriaRequest);

        if (nombre == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre de la categoría es obligatorio"));
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);

        Categoria categoriaGuardada = categoriaService.save(categoria);

        return ResponseEntity.ok(convertirAResponse(categoriaGuardada));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + ROLE_GERENTE + "', '" + ROLE_EMPLEADO + "')")
    public ResponseEntity<Object> actualizarCategoria(
            @PathVariable Long id,
            @RequestBody(required = false) CategoriaRequest categoriaRequest) {

        Categoria categoriaExistente = categoriaService.findById(id);

        if (categoriaExistente == null) {
            return ResponseEntity.notFound().build();
        }

        String nombre = obtenerNombreValidado(categoriaRequest);

        if (nombre == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre de la categoría es obligatorio"));
        }

        categoriaExistente.setNombre(nombre);

        Categoria categoriaActualizada = categoriaService.save(categoriaExistente);

        return ResponseEntity.ok(convertirAResponse(categoriaActualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + ROLE_GERENTE + "')")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        Categoria categoria = categoriaService.findById(id);

        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }

        categoriaService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private String obtenerNombreValidado(CategoriaRequest categoriaRequest) {

        if (categoriaRequest == null || categoriaRequest.getNombre() == null) {
            return null;
        }

        String nombre = categoriaRequest.getNombre().trim();

        if (nombre.isEmpty()) {
            return null;
        }

        return nombre;
    }

    private CategoriaResponse convertirAResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre()
        );
    }
}