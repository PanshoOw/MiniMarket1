package com.minimarket.service.impl;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        if (!tieneTexto(username)) {
            return Optional.empty();
        }

        return usuarioRepository.findByUsername(username);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        if (!tieneTexto(email)) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmail(email);
    }

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        return usuarioRepository.save(usuario);
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del usuario no puede ser nulo");
        }

        usuarioRepository.deleteById(id);
    }

    @Override
    public boolean usuarioTieneDatosCompletos(Usuario usuario) {
        if (usuario == null) {
            return false;
        }

        return tieneTexto(usuario.getUsername())
                && tieneTexto(usuario.getPassword())
                && tieneTexto(usuario.getNombre())
                && tieneTexto(usuario.getApellido())
                && tieneTexto(usuario.getEmail())
                && tieneTexto(usuario.getDireccion());
    }

    @Override
    public boolean usuarioTieneRol(Usuario usuario, String nombreRol) {
        if (usuario == null || !tieneTexto(nombreRol) || usuario.getRoles() == null) {
            return false;
        }

        return usuario.getRoles()
                .stream()
                .map(Rol::getNombre)
                .anyMatch(nombreRol::equals);
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }
}