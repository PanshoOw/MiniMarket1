package com.minimarket.controller;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.security.util.JwtUtil;
import com.minimarket.service.AuditoriaService;
import com.minimarket.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String API_AUTH = "/api/auth";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RolRepository rolRepository;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @Test
    void loginValidoDebeRetornarTokenUsernameYRoles() throws Exception {
        // REQ-AUTH-01:
        // Valida autenticación correcta: credenciales válidas generan token JWT,
        // username y roles asociados al usuario.

        UserDetails userDetails = new User(
                "gerente1",
                "password-encriptada",
                List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                ));

        when(userDetailsService.loadUserByUsername("gerente1")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("token-jwt-prueba");

        postLogin(loginJsonValido())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-prueba"))
                .andExpect(jsonPath("$.username").value("gerente1"))
                .andExpect(jsonPath("$.roles").value("ROLE_GERENTE"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("gerente1");
        verify(jwtUtil).generateToken(userDetails);
        verify(auditoriaService).registrarEvento(
                "gerente1",
                "LOGIN_EXITOSO",
                "0.0.0.0",
                "Roles: ROLE_GERENTE"
        );
    }

    @Test
    void loginInvalidoDebeRetornarUnauthorized() throws Exception {
        // REQ-AUTH-02:
        // Valida autenticación inválida: credenciales incorrectas retornan 401
        // y registran evento de auditoría fallido.

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        postLogin(loginJsonInvalido())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales incorrectas"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername("gerente1");
        verify(jwtUtil, never()).generateToken(any(UserDetails.class));
        verify(auditoriaService).registrarEvento(
                "gerente1",
                "LOGIN_FALLIDO",
                "0.0.0.0",
                "Credenciales incorrectas"
        );
    }

    @Test
    void registrarUsuarioValidoDebeAsignarRolCliente() throws Exception {
        // REQ-AUTH-03:
        // Valida que el registro público cree usuarios únicamente con ROLE_CLIENTE,
        // evitando que un usuario se registre con privilegios administrativos.

        Rol rolCliente = crearRol(1L, ROLE_CLIENTE);

        Usuario usuarioGuardado = crearUsuario(
                1L,
                "cliente1",
                "Cliente",
                "Uno",
                "cliente1@test.cl",
                "Calle 123"
        );

        usuarioGuardado.setRoles(java.util.Set.of(rolCliente));

        when(usuarioService.findByUsername("cliente1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("cliente1@test.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_CLIENTE)).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode("123456")).thenReturn("password-encriptada");
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        postRegister(registerJsonValido())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente con rol CLIENTE"))
                .andExpect(jsonPath("$.username").value("cliente1"))
                .andExpect(jsonPath("$.email").value("cliente1@test.cl"))
                .andExpect(jsonPath("$.rol").value(ROLE_CLIENTE));

        verify(usuarioService).save(any(Usuario.class));
        verify(passwordEncoder).encode("123456");
        verify(rolRepository).findByNombre(ROLE_CLIENTE);
    }

    @Test
    void registrarSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-04:
        // Valida que no se permita registrar un usuario sin cuerpo JSON.

        mockMvc.perform(post(API_AUTH + "/register")
                        .contentType(JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos de registro son obligatorios"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void registrarSinUsernameDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-05:
        // Valida que el username sea obligatorio para registrar usuarios.

        postRegister(registerJsonSinUsername())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de usuario es obligatorio"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void registrarSinPasswordDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-06:
        // Valida que la contraseña sea obligatoria para registrar usuarios.

        postRegister(registerJsonSinPassword())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña es obligatoria"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void registrarConEmailInvalidoDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-07:
        // Valida que el email tenga un formato mínimo correcto.

        postRegister(registerJsonEmailInvalido())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email no tiene un formato válido"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void registrarUsuarioExistenteDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-08:
        // Valida que no se permita registrar un username ya existente.

        Usuario existente = crearUsuario(
                1L,
                "cliente1",
                "Cliente",
                "Uno",
                "cliente1@test.cl",
                "Calle 123"
        );

        when(usuarioService.findByUsername("cliente1")).thenReturn(Optional.of(existente));

        postRegister(registerJsonValido())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El usuario ya existe"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void registrarEmailExistenteDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-09:
        // Valida que no se permita registrar un email ya utilizado por otro usuario.

        Usuario existente = crearUsuario(
                2L,
                "otrocliente",
                "Otro",
                "Cliente",
                "cliente1@test.cl",
                "Pasaje 456"
        );

        when(usuarioService.findByUsername("cliente1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("cliente1@test.cl")).thenReturn(Optional.of(existente));

        postRegister(registerJsonValido())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está en uso"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void registrarSinRolClienteEnBaseDebeRetornarBadRequest() throws Exception {
        // REQ-AUTH-10:
        // Valida que el registro no continúe si no existe ROLE_CLIENTE en la base de datos.

        when(usuarioService.findByUsername("cliente1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("cliente1@test.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_CLIENTE)).thenReturn(Optional.empty());

        postRegister(registerJsonValido())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No existe el rol ROLE_CLIENTE en la base de datos"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    private ResultActions postLogin(String json) throws Exception {
        return mockMvc.perform(post(API_AUTH + "/login")
                .contentType(JSON)
                .content(json));
    }

    private ResultActions postRegister(String json) throws Exception {
        return mockMvc.perform(post(API_AUTH + "/register")
                .contentType(JSON)
                .content(json));
    }

    private String loginJsonValido() {
        return """
                {
                    "username": "gerente1",
                    "password": "123456"
                }
                """;
    }

    private String loginJsonInvalido() {
        return """
                {
                    "username": "gerente1",
                    "password": "incorrecta"
                }
                """;
    }

    private String registerJsonValido() {
        return """
                {
                    "username": "cliente1",
                    "password": "123456",
                    "nombre": "Cliente",
                    "apellido": "Uno",
                    "email": "cliente1@test.cl",
                    "direccion": "Calle 123"
                }
                """;
    }

    private String registerJsonSinUsername() {
        return """
                {
                    "username": " ",
                    "password": "123456",
                    "nombre": "Cliente",
                    "apellido": "Uno",
                    "email": "cliente1@test.cl",
                    "direccion": "Calle 123"
                }
                """;
    }

    private String registerJsonSinPassword() {
        return """
                {
                    "username": "cliente1",
                    "password": " ",
                    "nombre": "Cliente",
                    "apellido": "Uno",
                    "email": "cliente1@test.cl",
                    "direccion": "Calle 123"
                }
                """;
    }

    private String registerJsonEmailInvalido() {
        return """
                {
                    "username": "cliente1",
                    "password": "123456",
                    "nombre": "Cliente",
                    "apellido": "Uno",
                    "email": "cliente1test.cl",
                    "direccion": "Calle 123"
                }
                """;
    }

    private Rol crearRol(Long id, String nombre) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombre(nombre);
        return rol;
    }

    private Usuario crearUsuario(Long id,
                                 String username,
                                 String nombre,
                                 String apellido,
                                 String email,
                                 String direccion) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setDireccion(direccion);
        return usuario;
    }
}