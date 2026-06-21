package com.minimarket.service.impl;

import com.minimarket.entity.Categoria;
import com.minimarket.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    @Test
    void findAllDebeRetornarListaDeCategorias() {
        Categoria categoria = crearCategoria(1L, "Bebidas");

        when(categoriaRepository.findAll()).thenReturn(List.of(categoria));

        List<Categoria> resultado = categoriaService.findAll();

        assertEquals(1, resultado.size());
        assertEquals("Bebidas", resultado.get(0).getNombre());
        verify(categoriaRepository).findAll();
    }

    @Test
    void findByIdExistenteDebeRetornarCategoria() {
        Categoria categoria = crearCategoria(1L, "Bebidas");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        Categoria resultado = categoriaService.findById(1L);

        assertSame(categoria, resultado);
        assertEquals("Bebidas", resultado.getNombre());
        verify(categoriaRepository).findById(1L);
    }

    @Test
    void findByIdInexistenteDebeRetornarNull() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        Categoria resultado = categoriaService.findById(99L);

        assertNull(resultado);
        verify(categoriaRepository).findById(99L);
    }

    @Test
    void findByIdConIdNuloDebeRetornarNull() {
        Categoria resultado = categoriaService.findById(null);

        assertNull(resultado);
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    void saveDebePersistirCategoria() {
        Categoria categoria = crearCategoria(1L, "Bebidas");

        when(categoriaRepository.save(categoria)).thenReturn(categoria);

        Categoria resultado = categoriaService.save(categoria);

        assertSame(categoria, resultado);
        assertEquals("Bebidas", resultado.getNombre());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void saveConCategoriaNulaDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.save(null)
        );

        assertEquals("La categoría no puede ser nula", exception.getMessage());
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    void deleteByIdDebeEliminarCategoria() {
        categoriaService.deleteById(1L);

        verify(categoriaRepository).deleteById(1L);
    }

    @Test
    void deleteByIdConIdNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.deleteById(null)
        );

        assertEquals("El id de la categoría no puede ser nulo", exception.getMessage());
        verifyNoInteractions(categoriaRepository);
    }

    private Categoria crearCategoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }
}