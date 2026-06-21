package com.minimarket.service.impl;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Venta;
import com.minimarket.repository.DetalleVentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
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
class DetalleVentaServiceImplTest {

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @InjectMocks
    private DetalleVentaServiceImpl detalleVentaService;

    @Test
    void findAllDebeRetornarListaDeDetalles() {
        DetalleVenta detalleVenta = crearDetalleVenta(1L, 2, 1500.0);

        when(detalleVentaRepository.findAll()).thenReturn(List.of(detalleVenta));

        List<DetalleVenta> resultado = detalleVentaService.findAll();

        assertEquals(1, resultado.size());
        assertEquals(2, resultado.get(0).getCantidad());
        verify(detalleVentaRepository).findAll();
    }

    @Test
    void findByIdExistenteDebeRetornarDetalleVenta() {
        DetalleVenta detalleVenta = crearDetalleVenta(1L, 2, 1500.0);

        when(detalleVentaRepository.findById(1L)).thenReturn(Optional.of(detalleVenta));

        DetalleVenta resultado = detalleVentaService.findById(1L);

        assertSame(detalleVenta, resultado);
        assertEquals(2, resultado.getCantidad());
        verify(detalleVentaRepository).findById(1L);
    }

    @Test
    void findByIdInexistenteDebeRetornarNull() {
        when(detalleVentaRepository.findById(99L)).thenReturn(Optional.empty());

        DetalleVenta resultado = detalleVentaService.findById(99L);

        assertNull(resultado);
        verify(detalleVentaRepository).findById(99L);
    }

    @Test
    void findByIdConIdNuloDebeRetornarNull() {
        DetalleVenta resultado = detalleVentaService.findById(null);

        assertNull(resultado);
        verifyNoInteractions(detalleVentaRepository);
    }

    @Test
    void saveDebePersistirDetalleVenta() {
        DetalleVenta detalleVenta = crearDetalleVenta(1L, 2, 1500.0);
        DetalleVenta detalleVentaNoNulo = Objects.requireNonNull(detalleVenta);

        when(detalleVentaRepository.save(detalleVentaNoNulo)).thenReturn(detalleVentaNoNulo);

        DetalleVenta resultado = detalleVentaService.save(detalleVentaNoNulo);

        assertSame(detalleVentaNoNulo, resultado);
        assertEquals(2, resultado.getCantidad());
        verify(detalleVentaRepository).save(detalleVentaNoNulo);
    }

    @Test
    void saveConDetalleVentaNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> detalleVentaService.save(null)
        );

        assertEquals("El detalle de venta no puede ser nulo", exception.getMessage());
        verifyNoInteractions(detalleVentaRepository);
    }

    @Test
    void deleteByIdDebeEliminarDetalleVenta() {
        detalleVentaService.deleteById(1L);

        verify(detalleVentaRepository).deleteById(1L);
    }

    @Test
    void deleteByIdConIdNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> detalleVentaService.deleteById(null)
        );

        assertEquals("El id del detalle de venta no puede ser nulo", exception.getMessage());
        verifyNoInteractions(detalleVentaRepository);
    }

    @Test
    void findByVentaIdDebeRetornarDetallesDeVenta() {
        DetalleVenta detalleVenta = crearDetalleVenta(1L, 2, 1500.0);

        when(detalleVentaRepository.findByVentaId(1L)).thenReturn(List.of(detalleVenta));

        List<DetalleVenta> resultado = detalleVentaService.findByVentaId(1L);

        assertEquals(1, resultado.size());
        assertEquals(2, resultado.get(0).getCantidad());
        verify(detalleVentaRepository).findByVentaId(1L);
    }

    @Test
    void findByVentaIdConIdNuloDebeRetornarListaVacia() {
        List<DetalleVenta> resultado = detalleVentaService.findByVentaId(null);

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(detalleVentaRepository);
    }

    private DetalleVenta crearDetalleVenta(Long id, Integer cantidad, Double precio) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Leche");
        producto.setPrecio(precio);
        producto.setStock(10);

        Venta venta = new Venta();
        venta.setId(1L);

        DetalleVenta detalleVenta = new DetalleVenta();
        detalleVenta.setId(id);
        detalleVenta.setProducto(producto);
        detalleVenta.setVenta(venta);
        detalleVenta.setCantidad(cantidad);
        detalleVenta.setPrecio(precio);

        return detalleVenta;
    }
}