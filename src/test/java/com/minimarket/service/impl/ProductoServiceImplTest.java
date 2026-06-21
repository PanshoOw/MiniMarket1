package com.minimarket.service.impl;

import com.minimarket.entity.Categoria;
import com.minimarket.entity.Producto;
import com.minimarket.repository.ProductoRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @Test
    void findAllDebeRetornarListaDeProductos() {
        Producto producto = crearProducto(1L, "Leche", 1500.0, 10);

        when(productoRepository.findAll()).thenReturn(List.of(producto));

        List<Producto> resultado = productoService.findAll();

        assertEquals(1, resultado.size());
        assertEquals("Leche", resultado.get(0).getNombre());
        verify(productoRepository).findAll();
    }

    @Test
    void findByIdExistenteDebeRetornarProducto() {
        Producto producto = crearProducto(1L, "Leche", 1500.0, 10);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        Producto resultado = productoService.findById(1L);

        assertSame(producto, resultado);
        assertEquals("Leche", resultado.getNombre());
        verify(productoRepository).findById(1L);
    }

    @Test
    void findByIdInexistenteDebeRetornarNull() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        Producto resultado = productoService.findById(99L);

        assertNull(resultado);
        verify(productoRepository).findById(99L);
    }

    @Test
    void findByIdConIdNuloDebeRetornarNull() {
        Producto resultado = productoService.findById(null);

        assertNull(resultado);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void saveDebePersistirProducto() {
        Producto producto = crearProducto(1L, "Leche", 1500.0, 10);

        when(productoRepository.save(producto)).thenReturn(producto);

        Producto resultado = productoService.save(producto);

        assertSame(producto, resultado);
        assertEquals("Leche", resultado.getNombre());
        verify(productoRepository).save(producto);
    }

    @Test
    void saveConProductoNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productoService.save(null)
        );

        assertEquals("El producto no puede ser nulo", exception.getMessage());
        verifyNoInteractions(productoRepository);
    }

    @Test
    void deleteByIdDebeEliminarProducto() {
        productoService.deleteById(1L);

        verify(productoRepository).deleteById(1L);
    }

    @Test
    void deleteByIdConIdNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productoService.deleteById(null)
        );

        assertEquals("El id del producto no puede ser nulo", exception.getMessage());
        verifyNoInteractions(productoRepository);
    }

    @Test
    void findByCategoriaIdDebeRetornarProductosDeCategoria() {
        Producto producto = crearProducto(1L, "Leche", 1500.0, 10);

        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of(producto));

        List<Producto> resultado = productoService.findByCategoriaId(1L);

        assertEquals(1, resultado.size());
        assertEquals("Leche", resultado.get(0).getNombre());
        verify(productoRepository).findByCategoriaId(1L);
    }

    @Test
    void findByCategoriaIdConIdNuloDebeRetornarListaVacia() {
        List<Producto> resultado = productoService.findByCategoriaId(null);

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(productoRepository);
    }

    private Producto crearProducto(Long id, String nombre, Double precio, Integer stock) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Bebidas");

        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(categoria);

        return producto;
    }
}