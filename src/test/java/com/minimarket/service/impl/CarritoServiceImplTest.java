package com.minimarket.service.impl;

import com.minimarket.entity.Carrito;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.CarritoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class CarritoServiceImplTest {

    @Mock
    private CarritoRepository carritoRepository;

    @InjectMocks
    private CarritoServiceImpl carritoService;

    @Test
    void findAllDebeRetornarListaDeCarritos() {
        Carrito carrito = crearCarritoCompleto();

        when(carritoRepository.findAll()).thenReturn(List.of(carrito));

        List<Carrito> resultado = carritoService.findAll();

        assertEquals(1, resultado.size());
        assertEquals(carrito.getId(), resultado.get(0).getId());
        assertEquals(2, resultado.get(0).getCantidad());
        verify(carritoRepository).findAll();
    }

    @Test
    void findAllSinDatosDebeRetornarListaVacia() {
        when(carritoRepository.findAll()).thenReturn(List.of());

        List<Carrito> resultado = carritoService.findAll();

        assertTrue(resultado.isEmpty());
        verify(carritoRepository).findAll();
    }

    @Test
    void findByIdConIdExistenteDebeRetornarCarrito() {
        Carrito carrito = crearCarritoCompleto();

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        Carrito resultado = carritoService.findById(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("cliente", resultado.getUsuario().getUsername());
        assertEquals("Leche", resultado.getProducto().getNombre());
        verify(carritoRepository).findById(1L);
    }

    @Test
    void findByIdConIdInexistenteDebeRetornarNull() {
        when(carritoRepository.findById(99L)).thenReturn(Optional.empty());

        Carrito resultado = carritoService.findById(99L);

        assertNull(resultado);
        verify(carritoRepository).findById(99L);
    }

    @Test
    void findByIdConIdNuloDebeRetornarNull() {
        Carrito resultado = carritoService.findById(null);

        assertNull(resultado);
        verifyNoInteractions(carritoRepository);
    }

    @Test
    void saveDebePersistirCarrito() {
        Carrito carrito = crearCarritoCompleto();
        Carrito carritoNoNulo = Objects.requireNonNull(carrito);

        when(carritoRepository.save(carritoNoNulo)).thenReturn(carritoNoNulo);

        Carrito resultado = carritoService.save(carritoNoNulo);

        assertSame(carritoNoNulo, resultado);
        assertEquals(2, resultado.getCantidad());
        verify(carritoRepository).save(carritoNoNulo);
    }

    @Test
    void saveConCarritoNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.save(null)
        );

        assertEquals("El carrito no puede ser nulo", exception.getMessage());
        verifyNoInteractions(carritoRepository);
    }

    @Test
    void deleteByIdDebeEliminarCarrito() {
        carritoService.deleteById(1L);

        verify(carritoRepository).deleteById(1L);
    }

    @Test
    void deleteByIdConIdNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.deleteById(null)
        );

        assertEquals("El id del carrito no puede ser nulo", exception.getMessage());
        verifyNoInteractions(carritoRepository);
    }

    @Test
    void findByUsuarioIdDebeRetornarCarritosDelUsuario() {
        Carrito carrito = crearCarritoCompleto();

        when(carritoRepository.findByUsuarioId(1L)).thenReturn(List.of(carrito));

        List<Carrito> resultado = carritoService.findByUsuarioId(1L);

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getUsuario().getId());
        assertEquals("cliente", resultado.get(0).getUsuario().getUsername());
        verify(carritoRepository).findByUsuarioId(1L);
    }

    @Test
    void findByUsuarioIdConIdNuloDebeRetornarListaVacia() {
        List<Carrito> resultado = carritoService.findByUsuarioId(null);

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(carritoRepository);
    }

    private Carrito crearCarritoCompleto() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("cliente");

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Leche");
        producto.setStock(10);

        Carrito carrito = new Carrito();
        carrito.setId(1L);
        carrito.setUsuario(usuario);
        carrito.setProducto(producto);
        carrito.setCantidad(2);

        return carrito;
    }
}