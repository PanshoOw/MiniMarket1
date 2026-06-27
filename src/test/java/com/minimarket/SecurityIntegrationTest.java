package com.minimarket;

import com.minimarket.entity.Categoria;
import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.service.CategoriaService;
import com.minimarket.service.InventarioService;
import com.minimarket.service.ProductoService;
import com.minimarket.service.VentaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String API_USUARIOS = "/api/usuarios";
    private static final String API_INVENTARIO = "/api/inventario";
    private static final String API_PRODUCTOS = "/api/productos";
    private static final String API_VENTAS = "/api/ventas";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private InventarioService inventarioService;

    @MockitoBean
    private VentaService ventaService;

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeVerUsuarios() throws Exception {
        // REQ-SEG-USR-01:
        // Valida que un usuario con rol cliente no pueda acceder a la gestión de usuarios.

        mockMvc.perform(get(API_USUARIOS))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeVerUsuarios() throws Exception {
        // REQ-SEG-USR-02:
        // Valida que un usuario con rol gerente pueda acceder a la gestión de usuarios.

        mockMvc.perform(get(API_USUARIOS))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoPuedeVerInventario() throws Exception {
        // REQ-SEG-INV-01:
        // Valida que un usuario con rol empleado pueda consultar inventario.

        when(inventarioService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(API_INVENTARIO))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeVerInventario() throws Exception {
        // REQ-SEG-INV-02:
        // Valida que un usuario con rol cliente no pueda acceder a la gestión de inventario.

        mockMvc.perform(get(API_INVENTARIO))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).findAll();
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeRegistrarInventario() throws Exception {
        // REQ-SEG-INV-03:
        // Valida que el rol gerente pueda registrar movimientos de inventario.

        Inventario inventarioRegistrado = crearInventario(
                1L,
                crearProductoInventario(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(inventarioRegistrado);

        mockMvc.perform(post(API_INVENTARIO)
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonEntradaGerente()))
                .andExpect(status().isOk());

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoPuedeRegistrarInventario() throws Exception {
        // REQ-SEG-INV-04:
        // Valida que el rol empleado pueda registrar movimientos de inventario.

        Inventario inventarioRegistrado = crearInventario(
                2L,
                crearProductoInventario(2L, "Fideos", 20),
                "ENTRADA",
                10
        );

        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(inventarioRegistrado);

        mockMvc.perform(post(API_INVENTARIO)
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonEntradaEmpleado()))
                .andExpect(status().isOk());

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeRegistrarInventario() throws Exception {
        // REQ-SEG-INV-05:
        // Valida que el rol cliente no pueda registrar movimientos de inventario.

        mockMvc.perform(post(API_INVENTARIO)
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonEntradaClienteNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void usuarioNoAutenticadoNoPuedeRegistrarInventario() throws Exception {
        // REQ-SEG-INV-06:
        // Valida que un usuario sin autenticación no pueda registrar movimientos de inventario.
        // En esta configuración, Spring Security responde 403 al tratar la solicitud como usuario anónimo sin permisos.

        mockMvc.perform(post(API_INVENTARIO)
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonEntradaAnonimo()))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeActualizarInventario() throws Exception {
        // REQ-SEG-INV-07:
        // Valida que el rol gerente pueda actualizar movimientos de inventario.

        Inventario existente = crearInventario(
                1L,
                crearProductoInventario(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        Inventario actualizado = crearInventario(
                1L,
                crearProductoInventario(1L, "Arroz", 8),
                "SALIDA",
                2
        );

        when(inventarioService.findById(1L)).thenReturn(existente);
        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(actualizado);

        mockMvc.perform(put(API_INVENTARIO + "/1")
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonSalidaGerente()))
                .andExpect(status().isOk());

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoPuedeActualizarInventario() throws Exception {
        // REQ-SEG-INV-08:
        // Valida que el rol empleado pueda actualizar movimientos de inventario.

        Inventario existente = crearInventario(
                2L,
                crearProductoInventario(2L, "Fideos", 20),
                "ENTRADA",
                10
        );

        Inventario actualizado = crearInventario(
                2L,
                crearProductoInventario(2L, "Fideos", 15),
                "SALIDA",
                5
        );

        when(inventarioService.findById(2L)).thenReturn(existente);
        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(actualizado);

        mockMvc.perform(put(API_INVENTARIO + "/2")
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonSalidaEmpleado()))
                .andExpect(status().isOk());

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeActualizarInventario() throws Exception {
        // REQ-SEG-INV-09:
        // Valida que el rol cliente no pueda actualizar movimientos de inventario.

        mockMvc.perform(put(API_INVENTARIO + "/3")
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonSalidaClienteNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void usuarioNoAutenticadoNoPuedeActualizarInventario() throws Exception {
        // REQ-SEG-INV-10:
        // Valida que un usuario sin autenticación no pueda actualizar movimientos de inventario.

        mockMvc.perform(put(API_INVENTARIO + "/4")
                        .with(csrf())
                        .contentType(JSON)
                        .content(inventarioJsonSalidaAnonimo()))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeEliminarInventario() throws Exception {
        // REQ-SEG-INV-11:
        // Valida que el rol gerente pueda eliminar movimientos de inventario.

        Inventario inventario = crearInventario(
                1L,
                crearProductoInventario(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        when(inventarioService.findById(1L)).thenReturn(inventario);

        mockMvc.perform(delete(API_INVENTARIO + "/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(inventarioService).deleteById(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoPuedeEliminarInventario() throws Exception {
        // REQ-SEG-INV-12:
        // Valida que el rol empleado pueda eliminar movimientos de inventario.

        Inventario inventario = crearInventario(
                2L,
                crearProductoInventario(2L, "Fideos", 20),
                "ENTRADA",
                10
        );

        when(inventarioService.findById(2L)).thenReturn(inventario);

        mockMvc.perform(delete(API_INVENTARIO + "/2")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(inventarioService).deleteById(2L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeEliminarInventario() throws Exception {
        // REQ-SEG-INV-13:
        // Valida que el rol cliente no pueda eliminar movimientos de inventario.

        Long inventarioIdNoAutorizado = 3L;

        mockMvc.perform(delete(API_INVENTARIO + "/" + inventarioIdNoAutorizado)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).deleteById(inventarioIdNoAutorizado);
    }

    @Test
    void usuarioNoAutenticadoNoPuedeEliminarInventario() throws Exception {
        // REQ-SEG-INV-14:
        // Valida que un usuario sin autenticación no pueda eliminar movimientos de inventario.

        Long inventarioIdAnonimo = 4L;

        mockMvc.perform(delete(API_INVENTARIO + "/" + inventarioIdAnonimo)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(inventarioService, never()).deleteById(inventarioIdAnonimo);
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeConsultarVentas() throws Exception {
        // REQ-SEG-VENTA-01:
        // Valida que el rol gerente pueda consultar ventas registradas.

        when(ventaService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(API_VENTAS))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoPuedeConsultarVentas() throws Exception {
        // REQ-SEG-VENTA-02:
        // Valida que el rol empleado pueda consultar una venta específica por ID.
        // Se usa un endpoint distinto al del gerente para evitar duplicidad de implementación
        // y demostrar otro escenario de consulta permitido.

        Long ventaId = 1L;

        Venta venta = crearVenta(
                ventaId,
                crearUsuarioVenta(1L, "cajero1"),
                List.of(crearDetalleVenta(
                        1L,
                        crearProductoVenta(1L, "Arroz", 1200.0, 8),
                        2,
                        1200.0
                ))
        );

        when(ventaService.findById(ventaId)).thenReturn(venta);

        mockMvc.perform(get(API_VENTAS + "/" + ventaId))
                .andExpect(status().isOk());

        verify(ventaService).findById(ventaId);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeConsultarVentas() throws Exception {
        // REQ-SEG-VENTA-03:
        // Valida que el rol cliente no pueda acceder al módulo de ventas.

        mockMvc.perform(get(API_VENTAS))
                .andExpect(status().isForbidden());

        verify(ventaService, never()).findAll();
    }

    @Test
    void usuarioNoAutenticadoNoPuedeConsultarVentas() throws Exception {
        // REQ-SEG-VENTA-04:
        // Valida que un usuario sin autenticación no pueda consultar una venta específica.
        // Se usa un endpoint por ID para diferenciar esta prueba del caso de cliente autenticado sin permisos.

        Long ventaId = 99L;

        mockMvc.perform(get(API_VENTAS + "/" + ventaId))
                .andExpect(status().isForbidden());

        verify(ventaService, never()).findById(ventaId);
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoPuedeRegistrarVenta() throws Exception {
        // REQ-SEG-VENTA-05:
        // Valida que solo el cajero/empleado pueda registrar ventas.

        Venta ventaRegistrada = crearVenta(
                1L,
                crearUsuarioVenta(1L, "cajero1"),
                List.of(crearDetalleVenta(
                        1L,
                        crearProductoVenta(1L, "Arroz", 1200.0, 8),
                        2,
                        1200.0
                ))
        );

        when(ventaService.registrarVenta(any(Venta.class))).thenReturn(ventaRegistrada);

        mockMvc.perform(post(API_VENTAS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(ventaJsonEmpleadoValida()))
                .andExpect(status().isOk());

        verify(ventaService).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerenteNoPuedeRegistrarVenta() throws Exception {
        // REQ-SEG-VENTA-06:
        // Valida que el gerente no pueda registrar ventas, ya que esa operación corresponde
        // específicamente al rol cajero/empleado.

        mockMvc.perform(post(API_VENTAS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(ventaJsonGerenteNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(ventaService, never()).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeRegistrarVenta() throws Exception {
        // REQ-SEG-VENTA-07:
        // Valida que el rol cliente no pueda registrar ventas.

        mockMvc.perform(post(API_VENTAS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(ventaJsonClienteNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(ventaService, never()).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    void usuarioNoAutenticadoNoPuedeRegistrarVenta() throws Exception {
        // REQ-SEG-VENTA-08:
        // Valida que un usuario sin autenticación no pueda registrar ventas.

        mockMvc.perform(post(API_VENTAS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(ventaJsonAnonimo()))
                .andExpect(status().isForbidden());

        verify(ventaService, never()).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeCrearProducto() throws Exception {
        // REQ-SEG-PROD-01:
        // Valida que solo el administrador/gerente pueda crear productos.

        Categoria categoria = crearCategoria(1L, "Bebidas");
        Producto productoGuardado = crearProducto(1L, "Leche", 1500.0, 10, categoria);

        when(categoriaService.findById(1L)).thenReturn(categoria);
        when(productoService.save(any(Producto.class))).thenReturn(productoGuardado);

        mockMvc.perform(post(API_PRODUCTOS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoJsonValido()))
                .andExpect(status().isOk());

        verify(productoService).save(any(Producto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoNoPuedeCrearProducto() throws Exception {
        // REQ-SEG-PROD-02:
        // Valida que el cajero/empleado no pueda crear productos, ya que esta operación
        // corresponde solo al administrador/gerente.

        mockMvc.perform(post(API_PRODUCTOS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoJsonEmpleadoNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeCrearProducto() throws Exception {
        // REQ-SEG-PROD-03:
        // Valida que un cliente no pueda crear productos.

        mockMvc.perform(post(API_PRODUCTOS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoJsonClienteNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void usuarioNoAutenticadoNoPuedeCrearProducto() throws Exception {
        // REQ-SEG-PROD-04:
        // Valida que un usuario sin autenticación no pueda crear productos.

        mockMvc.perform(post(API_PRODUCTOS)
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoJsonValido()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeActualizarProducto() throws Exception {
        // REQ-SEG-PROD-05:
        // Valida que solo el administrador/gerente pueda actualizar productos.

        Categoria categoria = crearCategoria(1L, "Bebidas");
        Producto productoExistente = crearProducto(1L, "Leche", 1500.0, 10, categoria);
        Producto productoActualizado = crearProducto(1L, "Leche descremada", 1800.0, 8, categoria);

        when(productoService.findById(1L)).thenReturn(productoExistente);
        when(categoriaService.findById(1L)).thenReturn(categoria);
        when(productoService.save(any(Producto.class))).thenReturn(productoActualizado);

        mockMvc.perform(put(API_PRODUCTOS + "/1")
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoActualizadoJsonValido()))
                .andExpect(status().isOk());

        verify(productoService).save(any(Producto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoNoPuedeActualizarProducto() throws Exception {
        // REQ-SEG-PROD-06:
        // Valida que el rol empleado no pueda actualizar productos.

        mockMvc.perform(put(API_PRODUCTOS + "/1")
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoActualizadoJsonEmpleadoNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeActualizarProducto() throws Exception {
        // REQ-SEG-PROD-07:
        // Valida que el rol cliente no pueda actualizar productos.

        Long productoIdNoAutorizado = 2L;

        mockMvc.perform(put(API_PRODUCTOS + "/" + productoIdNoAutorizado)
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoActualizadoJsonClienteNoAutorizado()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void usuarioNoAutenticadoNoPuedeActualizarProducto() throws Exception {
        // REQ-SEG-PROD-08:
        // Valida que un usuario sin autenticación no pueda actualizar productos.

        mockMvc.perform(put(API_PRODUCTOS + "/1")
                        .with(csrf())
                        .contentType(JSON)
                        .content(productoActualizadoJsonValido()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_GERENTE")
    void gerentePuedeEliminarProducto() throws Exception {
        // REQ-SEG-PROD-09:
        // Valida que solo el administrador/gerente pueda eliminar productos.

        Producto producto = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        when(productoService.findById(1L)).thenReturn(producto);

        mockMvc.perform(delete(API_PRODUCTOS + "/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(productoService).deleteById(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLEADO")
    void empleadoNoPuedeEliminarProducto() throws Exception {
        // REQ-SEG-PROD-10:
        // Valida que el rol empleado no pueda eliminar productos.

        Long productoIdNoAutorizado = 1L;

        mockMvc.perform(delete(API_PRODUCTOS + "/" + productoIdNoAutorizado)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).deleteById(productoIdNoAutorizado);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENTE")
    void clienteNoPuedeEliminarProducto() throws Exception {
        // REQ-SEG-PROD-11:
        // Valida que el rol cliente no pueda eliminar productos, incluso si intenta operar
        // sobre un recurso existente o potencialmente válido.

        Long productoIdNoAutorizado = 2L;

        mockMvc.perform(delete(API_PRODUCTOS + "/" + productoIdNoAutorizado)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).deleteById(productoIdNoAutorizado);
    }

    @Test
    void usuarioNoAutenticadoNoPuedeEliminarProducto() throws Exception {
        // REQ-SEG-PROD-12:
        // Valida que un usuario sin autenticación no pueda eliminar productos.

        mockMvc.perform(delete(API_PRODUCTOS + "/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(productoService, never()).deleteById(1L);
    }

    @NonNull
    private String inventarioJsonEntradaGerente() {
        return """
                {
                    "productoId": 1,
                    "tipoMovimiento": "ENTRADA",
                    "cantidad": 5
                }
                """;
    }

    @NonNull
    private String inventarioJsonEntradaEmpleado() {
        return """
                {
                    "productoId": 2,
                    "tipoMovimiento": "ENTRADA",
                    "cantidad": 10
                }
                """;
    }

    @NonNull
    private String inventarioJsonEntradaClienteNoAutorizado() {
        return """
                {
                    "productoId": 3,
                    "tipoMovimiento": "ENTRADA",
                    "cantidad": 7
                }
                """;
    }

    @NonNull
    private String inventarioJsonEntradaAnonimo() {
        return """
                {
                    "productoId": 4,
                    "tipoMovimiento": "ENTRADA",
                    "cantidad": 4
                }
                """;
    }

    @NonNull
    private String inventarioJsonSalidaGerente() {
        return """
                {
                    "productoId": 1,
                    "tipoMovimiento": "SALIDA",
                    "cantidad": 2
                }
                """;
    }

    @NonNull
    private String inventarioJsonSalidaEmpleado() {
        return """
                {
                    "productoId": 2,
                    "tipoMovimiento": "SALIDA",
                    "cantidad": 5
                }
                """;
    }

    @NonNull
    private String inventarioJsonSalidaClienteNoAutorizado() {
        return """
                {
                    "productoId": 3,
                    "tipoMovimiento": "SALIDA",
                    "cantidad": 1
                }
                """;
    }

    @NonNull
    private String inventarioJsonSalidaAnonimo() {
        return """
                {
                    "productoId": 4,
                    "tipoMovimiento": "SALIDA",
                    "cantidad": 1
                }
                """;
    }

    @NonNull
    private String ventaJsonEmpleadoValida() {
        return """
                {
                    "usuarioId": 1,
                    "detalles": [
                        {
                            "productoId": 1,
                            "cantidad": 2,
                            "precio": 1200.0
                        }
                    ]
                }
                """;
    }

    @NonNull
    private String ventaJsonGerenteNoAutorizado() {
        return """
                {
                    "usuarioId": 2,
                    "detalles": [
                        {
                            "productoId": 2,
                            "cantidad": 1,
                            "precio": 1500.0
                        }
                    ]
                }
                """;
    }

    @NonNull
    private String ventaJsonClienteNoAutorizado() {
        return """
                {
                    "usuarioId": 3,
                    "detalles": [
                        {
                            "productoId": 3,
                            "cantidad": 3,
                            "precio": 900.0
                        }
                    ]
                }
                """;
    }

    @NonNull
    private String ventaJsonAnonimo() {
        return """
                {
                    "usuarioId": 4,
                    "detalles": [
                        {
                            "productoId": 4,
                            "cantidad": 1,
                            "precio": 2000.0
                        }
                    ]
                }
                """;
    }

    @NonNull
    private String productoJsonValido() {
        return """
                {
                    "nombre": "Leche",
                    "precio": 1500.0,
                    "stock": 10,
                    "categoriaId": 1
                }
                """;
    }

    @NonNull
    private String productoJsonEmpleadoNoAutorizado() {
        return """
                {
                    "nombre": "Arroz",
                    "precio": 1200.0,
                    "stock": 15,
                    "categoriaId": 1
                }
                """;
    }

    @NonNull
    private String productoJsonClienteNoAutorizado() {
        return """
                {
                    "nombre": "Cereal",
                    "precio": 2500.0,
                    "stock": 6,
                    "categoriaId": 2
                }
                """;
    }

    @NonNull
    private String productoActualizadoJsonValido() {
        return """
                {
                    "nombre": "Leche descremada",
                    "precio": 1800.0,
                    "stock": 8,
                    "categoriaId": 1
                }
                """;
    }

    @NonNull
    private String productoActualizadoJsonEmpleadoNoAutorizado() {
        return """
                {
                    "nombre": "Arroz grado 1",
                    "precio": 1300.0,
                    "stock": 12,
                    "categoriaId": 1
                }
                """;
    }

    @NonNull
    private String productoActualizadoJsonClienteNoAutorizado() {
        return """
                {
                    "nombre": "Cereal familiar",
                    "precio": 2800.0,
                    "stock": 5,
                    "categoriaId": 2
                }
                """;
    }

    private Categoria crearCategoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Producto crearProducto(Long id,
                                   String nombre,
                                   Double precio,
                                   Integer stock,
                                   Categoria categoria) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(categoria);
        return producto;
    }

    private Producto crearProductoInventario(Long id, String nombre, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(stock);
        return producto;
    }

    private Producto crearProductoVenta(Long id, String nombre, Double precio, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        return producto;
    }

    private Usuario crearUsuarioVenta(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        return usuario;
    }

    private DetalleVenta crearDetalleVenta(Long id, Producto producto, Integer cantidad, Double precio) {
        DetalleVenta detalleVenta = new DetalleVenta();
        detalleVenta.setId(id);
        detalleVenta.setProducto(producto);
        detalleVenta.setCantidad(cantidad);
        detalleVenta.setPrecio(precio);
        return detalleVenta;
    }

    private Venta crearVenta(Long id, Usuario usuario, List<DetalleVenta> detalles) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setUsuario(usuario);
        venta.setFecha(new Date());
        venta.setDetalles(detalles);
        return venta;
    }

    private Inventario crearInventario(Long id,
                                       Producto producto,
                                       String tipoMovimiento,
                                       Integer cantidad) {
        Inventario inventario = new Inventario();
        inventario.setId(id);
        inventario.setProducto(producto);
        inventario.setTipoMovimiento(tipoMovimiento);
        inventario.setCantidad(cantidad);
        inventario.setFechaMovimiento(new Date());
        return inventario;
    }
}