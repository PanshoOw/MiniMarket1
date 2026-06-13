package com.minimarket.service.impl;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void usuarioConDatosCompletosDebeSerValido() {
        Usuario usuario = crearUsuarioCompleto();

        boolean resultado = usuarioService.usuarioTieneDatosCompletos(usuario);

        assertTrue(resultado);
    }

    @Test
    void usuarioSinNombreDebeSerInvalido() {
        Usuario usuario = crearUsuarioCompleto();
        usuario.setNombre(null);

        boolean resultado = usuarioService.usuarioTieneDatosCompletos(usuario);

        assertFalse(resultado);
    }

    @Test
    void usuarioSinApellidoDebeSerInvalido() {
        Usuario usuario = crearUsuarioCompleto();
        usuario.setApellido("");

        boolean resultado = usuarioService.usuarioTieneDatosCompletos(usuario);

        assertFalse(resultado);
    }

    @Test
    void usuarioSinEmailDebeSerInvalido() {
        Usuario usuario = crearUsuarioCompleto();
        usuario.setEmail("   ");

        boolean resultado = usuarioService.usuarioTieneDatosCompletos(usuario);

        assertFalse(resultado);
    }

    @Test
    void usuarioSinDireccionDebeSerInvalido() {
        Usuario usuario = crearUsuarioCompleto();
        usuario.setDireccion(null);

        boolean resultado = usuarioService.usuarioTieneDatosCompletos(usuario);

        assertFalse(resultado);
    }

    @Test
    void usuarioConRolEmpleadoDebeTenerPermisoParaRegistrarVenta() {
        Usuario usuario = crearUsuarioCompleto();
        Rol rolEmpleado = new Rol();
        rolEmpleado.setNombre("ROLE_EMPLEADO");
        usuario.setRoles(Set.of(rolEmpleado));

        boolean resultado = usuarioService.usuarioTieneRol(usuario, "ROLE_EMPLEADO");

        assertTrue(resultado);
    }

    @Test
    void usuarioConRolClienteNoDebeTenerPermisoDeEmpleado() {
        Usuario usuario = crearUsuarioCompleto();
        Rol rolCliente = new Rol();
        rolCliente.setNombre("ROLE_CLIENTE");
        usuario.setRoles(Set.of(rolCliente));

        boolean resultado = usuarioService.usuarioTieneRol(usuario, "ROLE_EMPLEADO");

        assertFalse(resultado);
    }

    @Test
    void findByUsernameDebeUsarRepositorioMock() {
        Usuario usuario = crearUsuarioCompleto();

        when(usuarioRepository.findByUsername("empleado"))
                .thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.findByUsername("empleado");

        assertTrue(resultado.isPresent());
        assertEquals("empleado", resultado.get().getUsername());
        verify(usuarioRepository).findByUsername("empleado");
    }

    @Test
    void saveConUsuarioNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.save(null)
        );

        assertEquals("El usuario no puede ser nulo", exception.getMessage());
    }

    private Usuario crearUsuarioCompleto() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("empleado");
        usuario.setPassword("empleado123");
        usuario.setNombre("Empleado");
        usuario.setApellido("Operativo");
        usuario.setEmail("empleado@minimarket.cl");
        usuario.setDireccion("Sucursal central 200");
        return usuario;
    }

    @Test
    void findByIdConIdNuloDebeRetornarOptionalVacio() {
        Optional<Usuario> resultado = usuarioService.findById(null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByUsernameVacioDebeRetornarOptionalVacio() {
        Optional<Usuario> resultado = usuarioService.findByUsername("   ");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByEmailVacioDebeRetornarOptionalVacio() {
        Optional<Usuario> resultado = usuarioService.findByEmail("");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deleteByIdConIdNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.deleteById(null)
        );

        assertEquals("El id del usuario no puede ser nulo", exception.getMessage());
    }

    @Test
    void usuarioNuloNoDebeTenerDatosCompletos() {
        boolean resultado = usuarioService.usuarioTieneDatosCompletos(null);

        assertFalse(resultado);
    }

    @Test
    void usuarioNuloNoDebeTenerRol() {
        boolean resultado = usuarioService.usuarioTieneRol(null, "ROLE_EMPLEADO");

        assertFalse(resultado);
    }

    @Test
    void usuarioConRolNuloNoDebeTenerRol() {
        Usuario usuario = crearUsuarioCompleto();

        boolean resultado = usuarioService.usuarioTieneRol(usuario, null);

        assertFalse(resultado);
    }
}