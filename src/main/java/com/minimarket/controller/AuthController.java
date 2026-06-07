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
import java.util.stream.Collectors;

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

        auditoriaService.registrarEvento(
                userDetails.getUsername(),
                "LOGIN_EXITOSO",
                "0.0.0.0",
                "Roles: " + userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","))
        );

        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest registerRequest) {

        if (usuarioService.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "El usuario ya existe"));
        }

        Optional<Rol> rolClienteOptional = rolRepository.findByNombre(ROLE_CLIENTE);

        if (rolClienteOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "No existe el rol ROLE_CLIENTE en la base de datos"));
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(registerRequest.getUsername());

        // Se almacena la contraseña cifrada, no en texto plano.
        usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Todo registro público queda limitado al rol CLIENTE.
        // Esto evita que una persona externa se registre como GERENTE o EMPLEADO.
        usuario.setRoles(Set.of(rolClienteOptional.get()));

        usuarioService.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Usuario registrado exitosamente con rol CLIENTE");
        response.put("username", usuario.getUsername());
        response.put("rol", ROLE_CLIENTE);

        return ResponseEntity.ok(response);
    }
}