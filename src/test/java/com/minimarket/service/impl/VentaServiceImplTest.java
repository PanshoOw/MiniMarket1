package com.minimarket.service.impl;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    @Test
    void ventaConUsuarioExistenteDebeSerValida() {
        Usuario usuario = crearUsuario(1L);
        Venta venta = crearVentaConUsuario(usuario);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        boolean resultado = ventaService.ventaTieneUsuarioValido(venta);

        assertTrue(resultado);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void ventaSinUsuarioDebeSerInvalida() {
        Venta venta = new Venta();
        venta.setUsuario(null);

        boolean resultado = ventaService.ventaTieneUsuarioValido(venta);

        assertFalse(resultado);
    }

    @Test
    void ventaConStockSuficienteDebeSerValida() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        DetalleVenta detalle = crearDetalle(producto, 3, 1500.0);
        Venta venta = crearVentaConDetalles(List.of(detalle));

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        boolean resultado = ventaService.ventaTieneStockSuficiente(venta);

        assertTrue(resultado);
        verify(productoRepository).findById(1L);
    }

    @Test
    void ventaSinStockSuficienteDebeSerRechazada() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 2);
        DetalleVenta detalle = crearDetalle(producto, 5, 1500.0);
        Venta venta = crearVentaConDetalles(List.of(detalle));

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        boolean resultado = ventaService.ventaTieneStockSuficiente(venta);

        assertFalse(resultado);
        verify(productoRepository).findById(1L);
    }

    @Test
    void calcularTotalVentaDebeSumarPrecioPorCantidad() {
        Producto arroz = crearProducto(1L, "Arroz", 1500.0, 10);
        Producto aceite = crearProducto(2L, "Aceite", 2000.0, 8);

        DetalleVenta detalleArroz = crearDetalle(arroz, 2, 1500.0);
        DetalleVenta detalleAceite = crearDetalle(aceite, 3, 2000.0);

        Venta venta = crearVentaConDetalles(List.of(detalleArroz, detalleAceite));

        double total = ventaService.calcularTotalVenta(venta);

        assertEquals(9000.0, total, 0.001);
    }

    @Test
    void registrarVentaDebeDescontarStockYGuardarVenta() {
        Usuario usuario = crearUsuario(1L);
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        DetalleVenta detalle = crearDetalle(producto, 3, 1500.0);

        Venta venta = crearVentaConUsuario(usuario);
        venta.setDetalles(List.of(detalle));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(ventaRepository.save(venta)).thenReturn(venta);

        Venta resultado = ventaService.registrarVenta(venta);

        assertSame(venta, resultado);
        assertEquals(7, producto.getStock());
        assertSame(venta, detalle.getVenta());
        assertNotNull(venta.getFecha());

        verify(usuarioRepository).findById(1L);
        verify(productoRepository, times(2)).findById(1L);
        verify(productoRepository).save(producto);
        verify(ventaRepository).save(venta);
    }

    @Test
    void registrarVentaSinStockDebeLanzarExcepcion() {
        Usuario usuario = crearUsuario(1L);
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 2);
        DetalleVenta detalle = crearDetalle(producto, 5, 1500.0);

        Venta venta = crearVentaConUsuario(usuario);
        venta.setDetalles(List.of(detalle));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrarVenta(venta)
        );

        assertEquals("No existe stock suficiente para registrar la venta", exception.getMessage());

        verify(usuarioRepository).findById(1L);
        verify(productoRepository).findById(1L);
        verifyNoInteractions(ventaRepository);
        verifyNoMoreInteractions(productoRepository);
    }

    @Test
    void findByIdConIdNuloDebeRetornarNull() {
        Venta resultado = ventaService.findById(null);

        assertNull(resultado);
    }

    @Test
    void findByUsuarioIdConIdNuloDebeRetornarListaVacia() {
        List<Venta> resultado = ventaService.findByUsuarioId(null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void saveConVentaNulaDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.save(null)
        );

        assertEquals("La venta no puede ser nula", exception.getMessage());
    }

    @Test
    void ventaNulaNoDebeTenerUsuarioValido() {
        boolean resultado = ventaService.ventaTieneUsuarioValido(null);

        assertFalse(resultado);
    }

    @Test
    void ventaConUsuarioSinIdDebeSerInvalida() {
        Usuario usuario = crearUsuario(null);
        Venta venta = crearVentaConUsuario(usuario);

        boolean resultado = ventaService.ventaTieneUsuarioValido(venta);

        assertFalse(resultado);
    }

    @Test
    void ventaNulaNoDebeTenerStockSuficiente() {
        boolean resultado = ventaService.ventaTieneStockSuficiente(null);

        assertFalse(resultado);
    }

    @Test
    void ventaSinDetallesNoDebeTenerStockSuficiente() {
        Venta venta = new Venta();
        venta.setDetalles(List.of());

        boolean resultado = ventaService.ventaTieneStockSuficiente(venta);

        assertFalse(resultado);
    }

    @Test
    void calcularTotalVentaNulaDebeRetornarCero() {
        double total = ventaService.calcularTotalVenta(null);

        assertEquals(0.0, total, 0.001);
    }

    @Test
    void calcularTotalVentaSinDetallesDebeRetornarCero() {
        Venta venta = new Venta();
        venta.setDetalles(List.of());

        double total = ventaService.calcularTotalVenta(venta);

        assertEquals(0.0, total, 0.001);
    }

    @Test
    void registrarVentaNulaDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrarVenta(null)
        );

        assertEquals("La venta no puede ser nula", exception.getMessage());
    }

    @Test
    void registrarVentaConUsuarioInvalidoDebeLanzarExcepcion() {
        Usuario usuario = crearUsuario(99L);
        Venta venta = crearVentaConUsuario(usuario);

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrarVenta(venta)
        );

        assertEquals("La venta debe estar asociada a un usuario válido", exception.getMessage());
        verify(usuarioRepository).findById(99L);
        verifyNoInteractions(ventaRepository);
    }

    @Test
    void calcularTotalDebeUsarPrecioDelProductoSiDetalleNoTienePrecio() {
        Producto producto = crearProducto(1L, "Arroz", 1500.0, 10);
        DetalleVenta detalle = crearDetalle(producto, 2, null);
        Venta venta = crearVentaConDetalles(List.of(detalle));

        double total = ventaService.calcularTotalVenta(venta);

        assertEquals(3000.0, total, 0.001);
    }

    private Usuario crearUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername("empleado");
        usuario.setPassword("empleado123");
        usuario.setNombre("Empleado");
        usuario.setApellido("Operativo");
        usuario.setEmail("empleado@minimarket.cl");
        usuario.setDireccion("Sucursal central 200");
        return usuario;
    }

    private Producto crearProducto(Long id, String nombre, Double precio, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        return producto;
    }

    private DetalleVenta crearDetalle(Producto producto, Integer cantidad, Double precio) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecio(precio);
        return detalle;
    }

    private Venta crearVentaConUsuario(Usuario usuario) {
        Venta venta = new Venta();
        venta.setUsuario(usuario);
        return venta;
    }

    private Venta crearVentaConDetalles(List<DetalleVenta> detalles) {
        Venta venta = new Venta();
        venta.setDetalles(detalles);
        return venta;
    }
}