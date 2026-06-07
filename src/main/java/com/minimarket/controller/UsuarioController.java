package com.minimarket.controller;

import com.minimarket.dto.UsuarioDTO;
import com.minimarket.dto.UsuarioRequest;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasAuthority('ROLE_GERENTE')")
public class UsuarioController {

    private static final String ERROR_KEY = "error";

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public UsuarioController(UsuarioService usuarioService,
                             PasswordEncoder passwordEncoder,
                             RolRepository rolRepository) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
    }

    @GetMapping
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioService.findAll()
                .stream()
                .map(this::convertirADto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorId(@PathVariable Long id) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);

        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertirADto(usuarioOptional.get()));
    }

    @PostMapping
    public ResponseEntity<Object> guardarUsuario(@RequestBody(required = false) UsuarioRequest usuarioRequest) {

        if (usuarioRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del usuario son obligatorios"));
        }

        String username = obtenerTextoValidado(usuarioRequest.getUsername());
        String password = obtenerTextoValidado(usuarioRequest.getPassword());
        String nombreRol = obtenerTextoValidado(usuarioRequest.getRol());

        if (username == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre de usuario es obligatorio"));
        }

        if (password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La contraseña es obligatoria"));
        }

        if (nombreRol == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El rol es obligatorio"));
        }

        if (usuarioService.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El usuario ya existe"));
        }

        Optional<Rol> rolOptional = rolRepository.findByNombre(nombreRol);

        if (rolOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El rol indicado no existe"));
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);

        // La contraseña se almacena cifrada con BCrypt.
        usuario.setPassword(passwordEncoder.encode(password));

        // Solo un GERENTE puede acceder a este controlador y asignar roles.
        usuario.setRoles(Set.of(rolOptional.get()));

        Usuario guardado = usuarioService.save(usuario);

        return ResponseEntity.ok(convertirADto(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarUsuario(@PathVariable Long id,
                                                    @RequestBody(required = false) UsuarioRequest usuarioRequest) {

        Optional<Usuario> usuarioExistenteOptional = usuarioService.findById(id);

        if (usuarioExistenteOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (usuarioRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos del usuario son obligatorios"));
        }

        String username = obtenerTextoValidado(usuarioRequest.getUsername());
        String password = obtenerTextoValidado(usuarioRequest.getPassword());
        String nombreRol = obtenerTextoValidado(usuarioRequest.getRol());

        if (username == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre de usuario es obligatorio"));
        }

        if (password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La contraseña es obligatoria"));
        }

        if (nombreRol == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El rol es obligatorio"));
        }

        Optional<Usuario> usuarioConMismoUsername = usuarioService.findByUsername(username);

        if (usuarioConMismoUsername.isPresent()
                && !usuarioConMismoUsername.get().getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre de usuario ya está en uso"));
        }

        Optional<Rol> rolOptional = rolRepository.findByNombre(nombreRol);

        if (rolOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El rol indicado no existe"));
        }

        Usuario usuarioExistente = usuarioExistenteOptional.get();
        usuarioExistente.setUsername(username);

        // La contraseña actualizada también se almacena cifrada.
        usuarioExistente.setPassword(passwordEncoder.encode(password));

        usuarioExistente.setRoles(Set.of(rolOptional.get()));

        Usuario guardado = usuarioService.save(usuarioExistente);

        return ResponseEntity.ok(convertirADto(guardado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioService.findById(id);

        if (usuario.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        usuarioService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private String obtenerTextoValidado(String texto) {

        if (texto == null) {
            return null;
        }

        String textoLimpio = texto.trim();

        if (textoLimpio.isEmpty()) {
            return null;
        }

        return textoLimpio;
    }

    private UsuarioDTO convertirADto(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getUsername()
        );
    }
}