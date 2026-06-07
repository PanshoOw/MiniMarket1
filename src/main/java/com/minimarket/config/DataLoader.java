package com.minimarket.config;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataLoader {

    private static final String ROLE_GERENTE = "ROLE_GERENTE";
    private static final String ROLE_EMPLEADO = "ROLE_EMPLEADO";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";

    @Bean
    CommandLineRunner initData(RolRepository rolRepository,
                               UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {

            Rol rolGerente = crearRolSiNoExiste(rolRepository, ROLE_GERENTE);
            Rol rolEmpleado = crearRolSiNoExiste(rolRepository, ROLE_EMPLEADO);
            Rol rolCliente = crearRolSiNoExiste(rolRepository, ROLE_CLIENTE);

            crearUsuarioSiNoExiste(
                    usuarioRepository,
                    passwordEncoder,
                    "gerente",
                    "gerente123",
                    rolGerente
            );

            crearUsuarioSiNoExiste(
                    usuarioRepository,
                    passwordEncoder,
                    "empleado",
                    "empleado123",
                    rolEmpleado
            );

            crearUsuarioSiNoExiste(
                    usuarioRepository,
                    passwordEncoder,
                    "cliente",
                    "cliente123",
                    rolCliente
            );
        };
    }

    private Rol crearRolSiNoExiste(RolRepository rolRepository, String nombreRol) {
        return rolRepository.findByNombre(nombreRol)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(nombreRol);
                    return rolRepository.save(rol);
                });
    }

    private void crearUsuarioSiNoExiste(UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        String username,
                                        String password,
                                        Rol rol) {

        if (usuarioRepository.findByUsername(username).isPresent()) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);

        // La contraseña se guarda cifrada con BCrypt.
        usuario.setPassword(passwordEncoder.encode(password));

        // El usuario se crea con un rol controlado desde el backend.
        usuario.setRoles(Set.of(rol));

        usuarioRepository.save(usuario);
    }
}