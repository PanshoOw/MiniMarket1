package com.minimarket.controller;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.security.filter.JwtAuthenticationFilter;
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
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private static final String ROLE_GERENTE = "ROLE_GERENTE";
    private static final String ROLE_EMPLEADO = "ROLE_EMPLEADO";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RolRepository rolRepository;

    @Test
    void listarUsuariosDebeRetornarOk() throws Exception {
        // REQ-USR-01:
        // Valida que el sistema permita listar usuarios transformados a UsuarioDTO,
        // evitando exponer datos sensibles como la contraseña.

        Usuario usuario = crearUsuario(
                1L,
                "gerente1",
                "Gerente",
                "Principal",
                "gerente@test.cl",
                "Oficina Central",
                crearRol(1L, ROLE_GERENTE)
        );

        when(usuarioService.findAll()).thenReturn(List.of(usuario));

        mockMvc.perform(get(API_USUARIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("gerente1"))
                .andExpect(jsonPath("$[0].nombre").value("Gerente"))
                .andExpect(jsonPath("$[0].apellido").value("Principal"))
                .andExpect(jsonPath("$[0].email").value("gerente@test.cl"))
                .andExpect(jsonPath("$[0].direccion").value("Oficina Central"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void obtenerUsuarioPorIdExistenteDebeRetornarOk() throws Exception {
        // REQ-USR-02:
        // Valida la consulta exitosa de un usuario existente por ID.

        Usuario usuario = crearUsuario(
                1L,
                "empleado1",
                "Empleado",
                "Uno",
                "empleado@test.cl",
                "Sucursal 1",
                crearRol(2L, ROLE_EMPLEADO)
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuario));

        mockMvc.perform(get(API_USUARIOS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("empleado1"))
                .andExpect(jsonPath("$.nombre").value("Empleado"))
                .andExpect(jsonPath("$.apellido").value("Uno"))
                .andExpect(jsonPath("$.email").value("empleado@test.cl"))
                .andExpect(jsonPath("$.direccion").value("Sucursal 1"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void obtenerUsuarioPorIdInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-USR-03:
        // Valida que el sistema responda 404 cuando se consulta un usuario inexistente.

        when(usuarioService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(API_USUARIOS + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void guardarUsuarioValidoDebeRetornarOk() throws Exception {
        // REQ-USR-04:
        // Valida que se pueda crear un usuario con datos válidos,
        // contraseña encriptada y rol existente.

        Rol rolGerente = crearRol(1L, ROLE_GERENTE);

        Usuario usuarioGuardado = crearUsuario(
                1L,
                "gerente1",
                "Gerente",
                "Principal",
                "gerente@test.cl",
                "Oficina Central",
                rolGerente
        );

        when(usuarioService.findByUsername("gerente1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("gerente@test.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_GERENTE)).thenReturn(Optional.of(rolGerente));
        when(passwordEncoder.encode("123456")).thenReturn("password-encriptada");
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        postUsuario(usuarioJsonValidoGerente())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("gerente1"))
                .andExpect(jsonPath("$.email").value("gerente@test.cl"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(passwordEncoder).encode("123456");
        verify(usuarioService).save(any(Usuario.class));
    }

    @Test
    void guardarUsuarioSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-USR-05:
        // Valida que no se permita crear un usuario sin cuerpo JSON.

        mockMvc.perform(post(API_USUARIOS)
                        .contentType(JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del usuario son obligatorios"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void guardarUsuarioSinUsernameDebeRetornarBadRequest() throws Exception {
        // REQ-USR-06:
        // Valida que el username sea obligatorio al crear usuarios.

        postUsuario(usuarioJsonSinUsername())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de usuario es obligatorio"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void guardarUsuarioExistenteDebeRetornarBadRequest() throws Exception {
        // REQ-USR-07:
        // Valida que no se permita crear un usuario con username duplicado.

        Usuario existente = crearUsuario(
                2L,
                "gerente1",
                "Otro",
                "Usuario",
                "otro@test.cl",
                "Otra dirección",
                crearRol(1L, ROLE_GERENTE)
        );

        when(usuarioService.findByUsername("gerente1")).thenReturn(Optional.of(existente));

        postUsuario(usuarioJsonValidoGerente())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El usuario ya existe"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void guardarUsuarioConEmailExistenteDebeRetornarBadRequest() throws Exception {
        // REQ-USR-08:
        // Valida que no se permita crear un usuario con email duplicado.

        Usuario existente = crearUsuario(
                3L,
                "otroUsuario",
                "Otro",
                "Usuario",
                "gerente@test.cl",
                "Otra dirección",
                crearRol(2L, ROLE_EMPLEADO)
        );

        when(usuarioService.findByUsername("gerente1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("gerente@test.cl")).thenReturn(Optional.of(existente));

        postUsuario(usuarioJsonValidoGerente())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está en uso"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void guardarUsuarioConRolInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-USR-09:
        // Valida que no se permita crear un usuario con un rol inexistente.

        when(usuarioService.findByUsername("gerente1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("gerente@test.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_GERENTE)).thenReturn(Optional.empty());

        postUsuario(usuarioJsonValidoGerente())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El rol indicado no existe"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioExistenteDebeRetornarOk() throws Exception {
        // REQ-USR-10:
        // Valida que se pueda actualizar un usuario existente con datos válidos.

        Rol rolEmpleado = crearRol(2L, ROLE_EMPLEADO);

        Usuario usuarioExistente = crearUsuario(
                1L,
                "gerente1",
                "Gerente",
                "Principal",
                "gerente@test.cl",
                "Oficina Central",
                crearRol(1L, ROLE_GERENTE)
        );

        Usuario usuarioActualizado = crearUsuario(
                1L,
                "empleado1",
                "Empleado",
                "Actualizado",
                "empleado@test.cl",
                "Sucursal 2",
                rolEmpleado
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleado1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado@test.cl")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre(ROLE_EMPLEADO)).thenReturn(Optional.of(rolEmpleado));
        when(passwordEncoder.encode("654321")).thenReturn("password-actualizada");
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioActualizado);

        putUsuario(1L, usuarioJsonActualizadoEmpleado())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("empleado1"))
                .andExpect(jsonPath("$.nombre").value("Empleado"))
                .andExpect(jsonPath("$.apellido").value("Actualizado"))
                .andExpect(jsonPath("$.email").value("empleado@test.cl"))
                .andExpect(jsonPath("$.direccion").value("Sucursal 2"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(passwordEncoder).encode("654321");
        verify(usuarioService).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-USR-11:
        // Valida que no se pueda actualizar un usuario inexistente.

        when(usuarioService.findById(99L)).thenReturn(Optional.empty());

        putUsuario(99L, usuarioJsonActualizadoEmpleado())
                .andExpect(status().isNotFound());

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-USR-12:
        // Valida que no se permita actualizar un usuario sin cuerpo JSON.

        Usuario usuarioExistente = crearUsuario(
                1L,
                "gerente1",
                "Gerente",
                "Principal",
                "gerente@test.cl",
                "Oficina Central",
                crearRol(1L, ROLE_GERENTE)
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));

        mockMvc.perform(put(API_USUARIOS + "/1")
                        .contentType(JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del usuario son obligatorios"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioConUsernameDeOtroUsuarioDebeRetornarBadRequest() throws Exception {
        // REQ-USR-13:
        // Valida que no se permita actualizar un usuario usando el username de otro usuario.

        Usuario usuarioExistente = crearUsuario(
                1L,
                "gerente1",
                "Gerente",
                "Principal",
                "gerente@test.cl",
                "Oficina Central",
                crearRol(1L, ROLE_GERENTE)
        );

        Usuario usuarioConMismoUsername = crearUsuario(
                2L,
                "empleado1",
                "Empleado",
                "Uno",
                "empleado@test.cl",
                "Sucursal 1",
                crearRol(2L, ROLE_EMPLEADO)
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleado1")).thenReturn(Optional.of(usuarioConMismoUsername));

        putUsuario(1L, usuarioJsonActualizadoEmpleado())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de usuario ya está en uso"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioConEmailDeOtroUsuarioDebeRetornarBadRequest() throws Exception {
        // REQ-USR-14:
        // Valida que no se permita actualizar un usuario usando el email de otro usuario.

        Usuario usuarioExistente = crearUsuario(
                1L,
                "gerente1",
                "Gerente",
                "Principal",
                "gerente@test.cl",
                "Oficina Central",
                crearRol(1L, ROLE_GERENTE)
        );

        Usuario usuarioConMismoEmail = crearUsuario(
                2L,
                "empleado2",
                "Empleado",
                "Dos",
                "empleado@test.cl",
                "Sucursal 3",
                crearRol(2L, ROLE_EMPLEADO)
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioService.findByUsername("empleado1")).thenReturn(Optional.empty());
        when(usuarioService.findByEmail("empleado@test.cl")).thenReturn(Optional.of(usuarioConMismoEmail));

        putUsuario(1L, usuarioJsonActualizadoEmpleado())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está en uso"));

        verify(usuarioService, never()).save(any(Usuario.class));
    }

    @Test
    void eliminarUsuarioExistenteDebeRetornarNoContent() throws Exception {
        // REQ-USR-15:
        // Valida que se pueda eliminar un usuario existente.

        Usuario usuario = crearUsuario(
                1L,
                "empleado1",
                "Empleado",
                "Uno",
                "empleado@test.cl",
                "Sucursal 1",
                crearRol(2L, ROLE_EMPLEADO)
        );

        when(usuarioService.findById(1L)).thenReturn(Optional.of(usuario));

        mockMvc.perform(delete(API_USUARIOS + "/1"))
                .andExpect(status().isNoContent());

        verify(usuarioService).deleteById(1L);
    }

    @Test
    void eliminarUsuarioInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-USR-16:
        // Valida que no se pueda eliminar un usuario inexistente.

        when(usuarioService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete(API_USUARIOS + "/99"))
                .andExpect(status().isNotFound());

        verify(usuarioService, never()).deleteById(99L);
    }

    private ResultActions postUsuario(String json) throws Exception {
        return mockMvc.perform(post(API_USUARIOS)
                .contentType(JSON)
                .content(json));
    }

    private ResultActions putUsuario(Long id, String json) throws Exception {
        return mockMvc.perform(put(API_USUARIOS + "/" + id)
                .contentType(JSON)
                .content(json));
    }

    @NonNull
    private String usuarioJsonValidoGerente() {
        return """
                {
                    "username": "gerente1",
                    "password": "123456",
                    "rol": "ROLE_GERENTE",
                    "nombre": "Gerente",
                    "apellido": "Principal",
                    "email": "gerente@test.cl",
                    "direccion": "Oficina Central"
                }
                """;
    }

    @NonNull
    private String usuarioJsonSinUsername() {
        return """
                {
                    "username": " ",
                    "password": "123456",
                    "rol": "ROLE_GERENTE",
                    "nombre": "Gerente",
                    "apellido": "Principal",
                    "email": "gerente@test.cl",
                    "direccion": "Oficina Central"
                }
                """;
    }

    @NonNull
    private String usuarioJsonActualizadoEmpleado() {
        return """
                {
                    "username": "empleado1",
                    "password": "654321",
                    "rol": "ROLE_EMPLEADO",
                    "nombre": "Empleado",
                    "apellido": "Actualizado",
                    "email": "empleado@test.cl",
                    "direccion": "Sucursal 2"
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
                                 String direccion,
                                 Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setDireccion(direccion);
        usuario.setRoles(Set.of(rol));
        return usuario;
    }
}