package com.minimarket.security.model;

public class RegisterRequest {

    private String username;
    private String password;
    private String rol;
    private String nombre;
    private String apellido;
    private String email;
    private String direccion;

    public RegisterRequest() {
    }

    public RegisterRequest(String username,
                           String password,
                           String rol,
                           String nombre,
                           String apellido,
                           String email,
                           String direccion) {
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.direccion = direccion;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRol() {
        return rol;
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