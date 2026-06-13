package com.minimarket.service;

import com.minimarket.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    List<Usuario> findAll();

    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    Usuario save(Usuario usuario);

    void deleteById(Long id);

    boolean usuarioTieneDatosCompletos(Usuario usuario);

    boolean usuarioTieneRol(Usuario usuario, String nombreRol);
}