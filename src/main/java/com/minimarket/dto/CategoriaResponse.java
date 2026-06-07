package com.minimarket.dto;

public class CategoriaResponse {

    private Long id;
    private String nombre;

    public CategoriaResponse() {
    }

    public CategoriaResponse(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}