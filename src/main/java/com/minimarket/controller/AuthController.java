package com.minimarket.controller;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.security.model.LoginRequest;
import com.minimarket.security.model.RegisterRequest;
import com.minimarket.security.util.JwtUtil;
import com.minimarket.service.AuditoriaService;
import com.minimarket.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ERROR_KEY = "error";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UsuarioService usuarioService,
                          PasswordEncoder passwordEncoder,
                          RolRepository rolRepository,
                          AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            auditoriaService.registrarEvento(
                    loginRequest.getUsername(),
                    "LOGIN_FALLIDO",
                    "0.0.0.0",
                    "Credenciales incorrectas"
            );

            return ResponseEntity.status(401)
                    .body(Map.of(ERROR_KEY, "Credenciales incorrectas"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        final String roles = obtenerRoles(userDetails);

        auditoriaService.registrarEvento(
                userDetails.getUsername(),
                "LOGIN_EXITOSO",
                "0.0.0.0",
                "Roles: " + roles
        );

        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", userDetails.getUsername());
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody(required = false) RegisterRequest registerRequest) {

        if (registerRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Los datos de registro son obligatorios"));
        }

        String username = obtenerTextoValidado(registerRequest.getUsername());
        String password = obtenerTextoValidado(registerRequest.getPassword());
        String nombre = obtenerTextoValidado(registerRequest.getNombre());
        String apellido = obtenerTextoValidado(registerRequest.getApellido());
        String email = obtenerTextoValidado(registerRequest.getEmail());
        String direccion = obtenerTextoValidado(registerRequest.getDireccion());

        ResponseEntity<Map<String, String>> validacion = validarDatosRegistro(
                username,
                password,
                nombre,
                apellido,
                email,
                direccion
        );

        if (validacion != null) {
            return validacion;
        }

        if (usuarioService.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El usuario ya existe"));
        }

        if (usuarioService.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El email ya está en uso"));
        }

        Optional<Rol> rolClienteOptional = rolRepository.findByNombre(ROLE_CLIENTE);

        if (rolClienteOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "No existe el rol ROLE_CLIENTE en la base de datos"));
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setDireccion(direccion);

        // El registro público asigna siempre el rol CLIENTE.
        usuario.setRoles(Set.of(rolClienteOptional.get()));

        usuarioService.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Usuario registrado exitosamente con rol CLIENTE");
        response.put("username", usuario.getUsername());
        response.put("email", usuario.getEmail());
        response.put("rol", ROLE_CLIENTE);

        return ResponseEntity.ok(response);
    }

    private String obtenerRoles(UserDetails userDetails) {
        StringJoiner rolesJoiner = new StringJoiner(",");

        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            rolesJoiner.add(authority.getAuthority());
        }

        return rolesJoiner.toString();
    }

    private ResponseEntity<Map<String, String>> validarDatosRegistro(String username,
                                                                     String password,
                                                                     String nombre,
                                                                     String apellido,
                                                                     String email,
                                                                     String direccion) {

        if (username == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre de usuario es obligatorio"));
        }

        if (password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La contraseña es obligatoria"));
        }

        if (nombre == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El nombre es obligatorio"));
        }

        if (apellido == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El apellido es obligatorio"));
        }

        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El email es obligatorio"));
        }

        if (!email.contains("@") || !email.contains(".")) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El email no tiene un formato válido"));
        }

        if (direccion == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "La dirección es obligatoria"));
        }

        return null;
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
}