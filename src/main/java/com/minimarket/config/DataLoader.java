package com.minimarket.config;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    private static final String ROLE_GERENTE = "ROLE_GERENTE";
    private static final String ROLE_EMPLEADO = "ROLE_EMPLEADO";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(RolRepository rolRepository,
                      UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Rol rolGerente = crearRolSiNoExiste(ROLE_GERENTE);
        Rol rolEmpleado = crearRolSiNoExiste(ROLE_EMPLEADO);
        Rol rolCliente = crearRolSiNoExiste(ROLE_CLIENTE);

        crearUsuarioSiNoExiste(
                new UsuarioInicial(
                        "gerente",
                        "gerente123",
                        "Gerente",
                        "Principal",
                        "gerente@minimarket.cl",
                        "Casa matriz 100",
                        rolGerente
                )
        );

        crearUsuarioSiNoExiste(
                new UsuarioInicial(
                        "empleado",
                        "empleado123",
                        "Empleado",
                        "Operativo",
                        "empleado@minimarket.cl",
                        "Sucursal central 200",
                        rolEmpleado
                )
        );

        crearUsuarioSiNoExiste(
                new UsuarioInicial(
                        "cliente",
                        "cliente123",
                        "Cliente",
                        "Demo",
                        "cliente@minimarket.cl",
                        "Dirección cliente 300",
                        rolCliente
                )
        );
    }

    private Rol crearRolSiNoExiste(String nombreRol) {
        return rolRepository.findByNombre(nombreRol)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(nombreRol);
                    return rolRepository.save(rol);
                });
    }

    private void crearUsuarioSiNoExiste(UsuarioInicial usuarioInicial) {
        if (usuarioRepository.findByUsername(usuarioInicial.username()).isPresent()) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(usuarioInicial.username());
        usuario.setPassword(passwordEncoder.encode(usuarioInicial.password()));
        usuario.setNombre(usuarioInicial.nombre());
        usuario.setApellido(usuarioInicial.apellido());
        usuario.setEmail(usuarioInicial.email());
        usuario.setDireccion(usuarioInicial.direccion());
        usuario.setRoles(Set.of(usuarioInicial.rol()));

        usuarioRepository.save(usuario);
    }

    private record UsuarioInicial(String username,
                                  String password,
                                  String nombre,
                                  String apellido,
                                  String email,
                                  String direccion,
                                  Rol rol) {
    }
}