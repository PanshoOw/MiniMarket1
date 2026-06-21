package com.minimarket.service.impl;

import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.service.InventarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    @Test
    void findAllDebeRetornarListaDeMovimientos() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        when(inventarioRepository.findAll()).thenReturn(List.of(inventario));

        List<Inventario> resultado = inventarioService.findAll();

        assertEquals(1, resultado.size());
        assertEquals(5, resultado.get(0).getCantidad());
        verify(inventarioRepository).findAll();
    }

    @Test
    void findByIdExistenteDebeRetornarInventario() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));

        Inventario resultado = inventarioService.findById(1L);

        assertSame(inventario, resultado);
        assertEquals(5, resultado.getCantidad());
        verify(inventarioRepository).findById(1L);
    }

    @Test
    void findByIdInexistenteDebeRetornarNull() {
        when(inventarioRepository.findById(99L)).thenReturn(Optional.empty());

        Inventario resultado = inventarioService.findById(99L);

        assertNull(resultado);
        verify(inventarioRepository).findById(99L);
    }

    @Test
    void findByIdConIdNuloDebeRetornarNull() {
        Inventario resultado = inventarioService.findById(null);

        assertNull(resultado);
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void saveDebePersistirInventario() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        Inventario inventarioNoNulo = Objects.requireNonNull(inventario);

        when(inventarioRepository.save(inventarioNoNulo)).thenReturn(inventarioNoNulo);

        Inventario resultado = inventarioService.save(inventarioNoNulo);

        assertSame(inventarioNoNulo, resultado);
        assertEquals(5, resultado.getCantidad());
        verify(inventarioRepository).save(inventarioNoNulo);
    }

    @Test
    void saveConInventarioNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.save(null)
        );

        assertEquals("El movimiento de inventario no puede ser nulo", exception.getMessage());
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void deleteByIdDebeEliminarInventario() {
        inventarioService.deleteById(1L);

        verify(inventarioRepository).deleteById(1L);
    }

    @Test
    void deleteByIdConIdNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.deleteById(null)
        );

        assertEquals("El id del movimiento de inventario no puede ser nulo", exception.getMessage());
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void findByProductoIdValidoDebeConsultarRepositorio() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                producto
        );

        when(inventarioRepository.findByProductoId(1L)).thenReturn(List.of(inventario));

        List<Inventario> resultado = inventarioService.findByProductoId(1L);

        assertEquals(1, resultado.size());
        assertEquals(5, resultado.get(0).getCantidad());
        verify(inventarioRepository).findByProductoId(1L);
    }

    @Test
    void findByProductoIdConIdNuloDebeRetornarListaVacia() {
        List<Inventario> resultado = inventarioService.findByProductoId(null);

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void movimientoConTipoYCantidadValidosDebeSerValido() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        boolean resultado = inventarioService.movimientoTieneDatosValidos(inventario);

        assertTrue(resultado);
    }

    @Test
    void movimientoNuloDebeSerInvalido() {
        boolean resultado = inventarioService.movimientoTieneDatosValidos(null);

        assertFalse(resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "AJUSTE"})
    void movimientoConTipoInvalidoDebeSerInvalido(String tipoMovimientoInvalido) {
        Inventario inventario = crearInventario(
                tipoMovimientoInvalido,
                5,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        boolean resultado = inventarioService.movimientoTieneDatosValidos(inventario);

        assertFalse(resultado);
    }

    @Test
    void movimientoConCantidadNulaDebeSerInvalido() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                null,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        boolean resultado = inventarioService.movimientoTieneDatosValidos(inventario);

        assertFalse(resultado);
    }

    @Test
    void movimientoConCantidadCeroDebeSerInvalido() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                0,
                crearProducto(1L, "Arroz", 1500.0, 10)
        );

        boolean resultado = inventarioService.movimientoTieneDatosValidos(inventario);

        assertFalse(resultado);
    }

    @Test
    void inventarioConProductoExistenteDebeSerValido() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                producto
        );

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        boolean resultado = inventarioService.inventarioTieneProductoValido(inventario);

        assertTrue(resultado);
        verify(productoRepository).findById(1L);
    }

    @Test
    void inventarioNuloDebeSerInvalido() {
        boolean resultado = inventarioService.inventarioTieneProductoValido(null);

        assertFalse(resultado);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void inventarioSinProductoDebeSerInvalido() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                null
        );

        boolean resultado = inventarioService.inventarioTieneProductoValido(inventario);

        assertFalse(resultado);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void inventarioConProductoSinIdDebeSerInvalido() {
        Producto producto = crearProducto(null, "Arroz", 1500.0, 10);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                producto
        );

        boolean resultado = inventarioService.inventarioTieneProductoValido(inventario);

        assertFalse(resultado);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void registrarEntradaDebeAumentarStockYGuardarMovimiento() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                producto
        );

        Inventario inventarioNoNulo = Objects.requireNonNull(inventario);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.save(inventarioNoNulo)).thenReturn(inventarioNoNulo);

        Inventario resultado = inventarioService.registrarMovimiento(inventarioNoNulo);

        assertSame(inventarioNoNulo, resultado);
        assertEquals(15, producto.getStock());
        assertSame(producto, inventarioNoNulo.getProducto());
        assertNotNull(inventarioNoNulo.getFechaMovimiento());

        verify(productoRepository).findById(1L);
        verify(productoRepository).save(Objects.requireNonNull(producto));
        verify(inventarioRepository).save(inventarioNoNulo);
    }

    @Test
    void registrarSalidaDebeDisminuirStockYGuardarMovimiento() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_SALIDA,
                4,
                producto
        );

        Inventario inventarioNoNulo = Objects.requireNonNull(inventario);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.save(inventarioNoNulo)).thenReturn(inventarioNoNulo);

        Inventario resultado = inventarioService.registrarMovimiento(inventarioNoNulo);

        assertSame(inventarioNoNulo, resultado);
        assertEquals(6, producto.getStock());
        assertSame(producto, inventarioNoNulo.getProducto());
        assertNotNull(inventarioNoNulo.getFechaMovimiento());

        verify(productoRepository).findById(1L);
        verify(productoRepository).save(Objects.requireNonNull(producto));
        verify(inventarioRepository).save(inventarioNoNulo);
    }

    @Test
    void registrarSalidaSinStockDebeLanzarExcepcion() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 3);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_SALIDA,
                5,
                producto
        );

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.registrarMovimiento(inventario)
        );

        assertEquals("No existe stock suficiente para registrar la salida", exception.getMessage());
        assertEquals(3, producto.getStock());

        verify(productoRepository).findById(1L);
        verifyNoMoreInteractions(productoRepository);
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void registrarMovimientoConProductoInexistenteDebeLanzarExcepcion() {
        Producto producto = crearProducto(99L, "Producto inexistente", 1000.0, 10);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                producto
        );

        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.registrarMovimiento(inventario)
        );

        assertEquals("El producto asociado no existe", exception.getMessage());

        verify(productoRepository).findById(99L);
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void registrarMovimientoSinProductoDebeLanzarExcepcion() {
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.registrarMovimiento(inventario)
        );

        assertEquals("El movimiento debe estar asociado a un producto válido", exception.getMessage());
        verifyNoInteractions(productoRepository);
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void registrarMovimientoConProductoSinStockDebeLanzarExcepcion() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, null);
        Inventario inventario = crearInventario(
                InventarioService.TIPO_ENTRADA,
                5,
                producto
        );

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.registrarMovimiento(inventario)
        );

        assertEquals("El producto no tiene stock definido", exception.getMessage());

        verify(productoRepository).findById(1L);
        verifyNoMoreInteractions(productoRepository);
        verifyNoInteractions(inventarioRepository);
    }

    @Test
    void registrarMovimientoNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.registrarMovimiento(null)
        );

        assertEquals("El movimiento de inventario no puede ser nulo", exception.getMessage());
        verifyNoInteractions(productoRepository);
        verifyNoInteractions(inventarioRepository);
    }

    private Producto crearProducto(Long id, String nombre, Double precio, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        return producto;
    }

    private Inventario crearInventario(String tipoMovimiento, Integer cantidad, Producto producto) {
        Inventario inventario = new Inventario();
        inventario.setTipoMovimiento(tipoMovimiento);
        inventario.setCantidad(cantidad);
        inventario.setProducto(producto);
        return inventario;
    }
}