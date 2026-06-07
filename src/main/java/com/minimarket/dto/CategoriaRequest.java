package com.minimarket.dto;

public class CategoriaRequest {

    private String nombre;

    public CategoriaRequest() {
    }

    public CategoriaRequest(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}