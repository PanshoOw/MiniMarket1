package com.minimarket.dto;

public class UsuarioDTO {

    private Long id;
    private String username;
    private String nombre;
    private String apellido;
    private String email;
    private String direccion;

    public UsuarioDTO() {
    }

    public UsuarioDTO(Long id,
                      String username,
                      String nombre,
                      String apellido,
                      String email,
                      String direccion) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.direccion = direccion;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getDireccion() {
        return direccion;
    }
}