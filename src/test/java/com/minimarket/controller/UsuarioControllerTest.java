package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.dto.UsuarioRequest;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = UsuarioController.class,
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
class UsuarioControllerTest {

    private static final String API_USUARIOS = "/api/usuarios";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String ROLE_EMPLEADO = "ROLE_EMPLEADO";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RolRepository rolRepository;

    @Test
    void listarUsuariosDebeRetornarOk() throws Exception {
        Usuario usuario = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        when(usuarioService.findAll()).thenReturn(List.of(usuario));

        mockMvc.perform(get(API_USUARIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("empleado"))
                .andExpect(jsonPath("$[0].nombre").value("Empleado"))
                .andExpect(jsonPath("$[0].apellido").value("Operativo"))
                .andExpect(jsonPath("$[0].email").value("empleado@minimarket.cl"))
                .andExpect(jsonPath("$[0].direccion").value("Sucursal central 200"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void listarUsuariosVacioDebeRetornarOk() throws Exception {
        when(usuarioService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(API_USUARIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void obtenerUsuarioPorIdExistenteDebeRetornarOk() throws Exception {
        Usuario usuario = crearUsuario(
                1L,
                "cliente",
                "Cliente",
                "Demo",
                "cliente@minimarket.cl",
                "Dirección cliente 300",
                ROLE_CLIENTE
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuario));

        mockMvc.perform(get(API_USUARIOS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("cliente"))
                .andExpect(jsonPath("$.nombre").value("Cliente"))
                .andExpect(jsonPath("$.apellido").value("Demo"))
                .andExpect(jsonPath("$.email").value("cliente@minimarket.cl"))
                .andExpect(jsonPath("$.direccion").value("Dirección cliente 300"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void obtenerUsuarioPorIdInexistenteDebeRetornarNotFound() throws Exception {
        when(usuarioService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(API_USUARIOS + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void guardarUsuarioValidoDebeRetornarOk() throws Exception {
        UsuarioRequest request = crearRequestValido();
        Rol rolEmpleado = crearRol(1L, ROLE_EMPLEADO);
        Usuario usuarioGuardado = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        when(usuarioService.findByUsername("empleado")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado@minimarket.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_EMPLEADO)).thenReturn(Optional.of(rolEmpleado));
        when(passwordEncoder.encode("clave123")).thenReturn("clave-encriptada");
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        postUsuario(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("empleado"))
                .andExpect(jsonPath("$.nombre").value("Empleado"))
                .andExpect(jsonPath("$.apellido").value("Operativo"))
                .andExpect(jsonPath("$.email").value("empleado@minimarket.cl"))
                .andExpect(jsonPath("$.direccion").value("Sucursal central 200"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(passwordEncoder).encode("clave123");
        verify(usuarioService).save(any(Usuario.class));
    }

    @Test
    void guardarUsuarioConBodyNullDebeRetornarBadRequest() throws Exception {
        mockMvc.perform(post(API_USUARIOS)
                        .contentType(JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del usuario son obligatorios"));
    }

    @ParameterizedTest
    @MethodSource("requestsInvalidos")
    void guardarUsuarioConDatosInvalidosDebeRetornarBadRequest(UsuarioRequest request,
                                                               String mensajeEsperado) throws Exception {
        postUsuario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(mensajeEsperado));
    }

    @Test
    void guardarUsuarioConUsernameExistenteDebeRetornarBadRequest() throws Exception {
        UsuarioRequest request = crearRequestValido();
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        when(usuarioService.findByUsername("empleado")).thenReturn(Optional.of(usuarioExistente));

        postUsuario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El usuario ya existe"));
    }

    @Test
    void guardarUsuarioConEmailExistenteDebeRetornarBadRequest() throws Exception {
        UsuarioRequest request = crearRequestValido();
        Usuario usuarioExistente = crearUsuario(
                2L,
                "otro",
                "Otro",
                "Usuario",
                "empleado@minimarket.cl",
                "Otra dirección",
                ROLE_CLIENTE
        );

        when(usuarioService.findByUsername("empleado")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado@minimarket.cl")).thenReturn(Optional.of(usuarioExistente));

        postUsuario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está en uso"));
    }

    @Test
    void guardarUsuarioConRolInexistenteDebeRetornarBadRequest() throws Exception {
        UsuarioRequest request = crearRequestValido();

        when(usuarioService.findByUsername("empleado")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado@minimarket.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_EMPLEADO)).thenReturn(Optional.empty());

        postUsuario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El rol indicado no existe"));
    }

    @Test
    void actualizarUsuarioValidoDebeRetornarOk() throws Exception {
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        UsuarioRequest request = new UsuarioRequest(
                "empleadoActualizado",
                "clave123",
                ROLE_EMPLEADO,
                "Empleado",
                "Actualizado",
                "empleado.actualizado@minimarket.cl",
                "Sucursal actualizada 500"
        );

        Usuario usuarioActualizado = crearUsuario(
                1L,
                "empleadoActualizado",
                "Empleado",
                "Actualizado",
                "empleado.actualizado@minimarket.cl",
                "Sucursal actualizada 500",
                ROLE_EMPLEADO
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleadoActualizado")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado.actualizado@minimarket.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_EMPLEADO)).thenReturn(Optional.of(crearRol(1L, ROLE_EMPLEADO)));
        when(passwordEncoder.encode("clave123")).thenReturn("clave-encriptada");
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioActualizado);

        putUsuario(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("empleadoActualizado"))
                .andExpect(jsonPath("$.nombre").value("Empleado"))
                .andExpect(jsonPath("$.apellido").value("Actualizado"))
                .andExpect(jsonPath("$.email").value("empleado.actualizado@minimarket.cl"))
                .andExpect(jsonPath("$.direccion").value("Sucursal actualizada 500"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(passwordEncoder).encode("clave123");
        verify(usuarioService).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioInexistenteDebeRetornarNotFound() throws Exception {
        when(usuarioService.findById(99L)).thenReturn(Optional.empty());

        putUsuario(99L, crearRequestValido())
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarUsuarioConBodyNullDebeRetornarBadRequest() throws Exception {
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));

        mockMvc.perform(put(API_USUARIOS + "/1")
                        .contentType(JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del usuario son obligatorios"));
    }

    @ParameterizedTest
    @MethodSource("requestsInvalidos")
    void actualizarUsuarioConDatosInvalidosDebeRetornarBadRequest(UsuarioRequest request,
                                                                  String mensajeEsperado) throws Exception {
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));

        putUsuario(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(mensajeEsperado));
    }

    @Test
    void actualizarUsuarioConUsernameDeOtroUsuarioDebeRetornarBadRequest() throws Exception {
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        Usuario otroUsuario = crearUsuario(
                2L,
                "empleadoActualizado",
                "Otro",
                "Usuario",
                "otro@minimarket.cl",
                "Otra dirección",
                ROLE_CLIENTE
        );

        UsuarioRequest request = new UsuarioRequest(
                "empleadoActualizado",
                "clave123",
                ROLE_EMPLEADO,
                "Empleado",
                "Operativo",
                "empleado.nuevo@minimarket.cl",
                "Sucursal central 200"
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleadoActualizado")).thenReturn(Optional.of(otroUsuario));

        putUsuario(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de usuario ya está en uso"));
    }

    @Test
    void actualizarUsuarioConEmailDeOtroUsuarioDebeRetornarBadRequest() throws Exception {
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        Usuario otroUsuario = crearUsuario(
                2L,
                "otro",
                "Otro",
                "Usuario",
                "empleado.actualizado@minimarket.cl",
                "Otra dirección",
                ROLE_CLIENTE
        );

        UsuarioRequest request = new UsuarioRequest(
                "empleadoActualizado",
                "clave123",
                ROLE_EMPLEADO,
                "Empleado",
                "Actualizado",
                "empleado.actualizado@minimarket.cl",
                "Sucursal actualizada 500"
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleadoActualizado")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado.actualizado@minimarket.cl")).thenReturn(Optional.of(otroUsuario));

        putUsuario(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está en uso"));
    }

    @Test
    void actualizarUsuarioConRolInexistenteDebeRetornarBadRequest() throws Exception {
        Usuario usuarioExistente = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        UsuarioRequest request = crearRequestValido();

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleado")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado@minimarket.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_EMPLEADO)).thenReturn(Optional.empty());

        putUsuario(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El rol indicado no existe"));
    }

    @Test
    void eliminarUsuarioExistenteDebeRetornarNoContent() throws Exception {
        Usuario usuario = crearUsuario(
                1L,
                "empleado",
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200",
                ROLE_EMPLEADO
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuario));

        mockMvc.perform(delete(API_USUARIOS + "/1"))
                .andExpect(status().isNoContent());

        verify(usuarioService).deleteById(1L);
    }

    @Test
    void eliminarUsuarioInexistenteDebeRetornarNotFound() throws Exception {
        when(usuarioService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete(API_USUARIOS + "/99"))
                .andExpect(status().isNotFound());
    }

    private ResultActions postUsuario(UsuarioRequest request) throws Exception {
        return mockMvc.perform(post(API_USUARIOS)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions putUsuario(Long id, UsuarioRequest request) throws Exception {
        return mockMvc.perform(put(API_USUARIOS + "/" + id)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    private static Stream<Arguments> requestsInvalidos() {
        return Stream.of(
                Arguments.of(
                        request(null, "clave123", ROLE_EMPLEADO, "Empleado", "Operativo",
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "El nombre de usuario es obligatorio"
                ),
                Arguments.of(
                        request("   ", "clave123", ROLE_EMPLEADO, "Empleado", "Operativo",
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "El nombre de usuario es obligatorio"
                ),
                Arguments.of(
                        request("empleado", null, ROLE_EMPLEADO, "Empleado", "Operativo",
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "La contraseña es obligatoria"
                ),
                Arguments.of(
                        request("empleado", "   ", ROLE_EMPLEADO, "Empleado", "Operativo",
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "La contraseña es obligatoria"
                ),
                Arguments.of(
                        request("empleado", "clave123", null, "Empleado", "Operativo",
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "El rol es obligatorio"
                ),
                Arguments.of(
                        request("empleado", "clave123", ROLE_EMPLEADO, null, "Operativo",
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "El nombre es obligatorio"
                ),
                Arguments.of(
                        request("empleado", "clave123", ROLE_EMPLEADO, "Empleado", null,
                                "empleado@minimarket.cl", "Sucursal central 200"),
                        "El apellido es obligatorio"
                ),
                Arguments.of(
                        request("empleado", "clave123", ROLE_EMPLEADO, "Empleado", "Operativo",
                                null, "Sucursal central 200"),
                        "El email es obligatorio"
                ),
                Arguments.of(
                        request("empleado", "clave123", ROLE_EMPLEADO, "Empleado", "Operativo",
                                "correo-invalido", "Sucursal central 200"),
                        "El email no tiene un formato válido"
                ),
                Arguments.of(
                        request("empleado", "clave123", ROLE_EMPLEADO, "Empleado", "Operativo",
                                "empleado@minimarket.cl", null),
                        "La dirección es obligatoria"
                )
        );
    }

    private static UsuarioRequest request(String username,
                                          String password,
                                          String rol,
                                          String nombre,
                                          String apellido,
                                          String email,
                                          String direccion) {
        return new UsuarioRequest(
                username,
                password,
                rol,
                nombre,
                apellido,
                email,
                direccion
        );
    }

    private UsuarioRequest crearRequestValido() {
        return new UsuarioRequest(
                "empleado",
                "clave123",
                ROLE_EMPLEADO,
                "Empleado",
                "Operativo",
                "empleado@minimarket.cl",
                "Sucursal central 200"
        );
    }

    private Usuario crearUsuario(Long id,
                                 String username,
                                 String nombre,
                                 String apellido,
                                 String email,
                                 String direccion,
                                 String nombreRol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPassword("clave-encriptada");
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setDireccion(direccion);
        usuario.setRoles(Set.of(crearRol(1L, nombreRol)));
        return usuario;
    }

    private static Rol crearRol(Long id, String nombre) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombre(nombre);
        return rol;
    }
}