package com.minimarket.controller;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.service.VentaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = VentaController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class VentaControllerTest {

    private static final String API_VENTAS = "/api/ventas";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaService ventaService;

    @Test
    void listarVentasDebeRetornarOk() throws Exception {
        // REQ-VENTA-01:
        // Valida que el sistema permita listar ventas registradas con usuario, detalles,
        // productos, cantidades y precios asociados.

        Venta venta = crearVenta(
                1L,
                crearUsuario(1L, "cajero1"),
                List.of(crearDetalleVenta(
                        1L,
                        crearProducto(1L, "Arroz", 1200.0, 10),
                        2,
                        1200.0
                ))
        );

        when(ventaService.findAll()).thenReturn(List.of(venta));

        mockMvc.perform(get(API_VENTAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].usuario.id").value(1))
                .andExpect(jsonPath("$[0].usuario.username").value("cajero1"))
                .andExpect(jsonPath("$[0].detalles[0].producto.id").value(1))
                .andExpect(jsonPath("$[0].detalles[0].producto.nombre").value("Arroz"))
                .andExpect(jsonPath("$[0].detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$[0].detalles[0].precio").value(1200.0));
    }

    @Test
    void obtenerVentaPorIdExistenteDebeRetornarOk() throws Exception {
        // REQ-VENTA-02:
        // Valida que el sistema permita consultar una venta existente mediante su identificador.

        Venta venta = crearVenta(
                1L,
                crearUsuario(1L, "cajero1"),
                List.of(crearDetalleVenta(
                        1L,
                        crearProducto(1L, "Arroz", 1200.0, 10),
                        2,
                        1200.0
                ))
        );

        when(ventaService.findById(1L)).thenReturn(venta);

        mockMvc.perform(get(API_VENTAS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuario.id").value(1))
                .andExpect(jsonPath("$.usuario.username").value("cajero1"))
                .andExpect(jsonPath("$.detalles[0].producto.id").value(1))
                .andExpect(jsonPath("$.detalles[0].producto.nombre").value("Arroz"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$.detalles[0].precio").value(1200.0));
    }

    @Test
    void obtenerVentaPorIdInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-VENTA-03:
        // Valida que el sistema responda 404 cuando se consulta una venta inexistente.

        when(ventaService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_VENTAS + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarVentaValidaDebeRetornarOk() throws Exception {
        // REQ-VENTA-04:
        // Valida que el controlador registre una venta usando VentaRequest,
        // transformando usuarioId y productoId en entidades antes de delegar al servicio.

        Venta ventaRegistrada = crearVenta(
                1L,
                crearUsuario(1L, "cajero1"),
                List.of(crearDetalleVenta(
                        1L,
                        crearProducto(1L, "Arroz", 1200.0, 8),
                        2,
                        1200.0
                ))
        );

        when(ventaService.registrarVenta(any(Venta.class))).thenReturn(ventaRegistrada);

        postVenta(ventaJsonValida())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuario.id").value(1))
                .andExpect(jsonPath("$.usuario.username").value("cajero1"))
                .andExpect(jsonPath("$.detalles[0].producto.id").value(1))
                .andExpect(jsonPath("$.detalles[0].producto.nombre").value("Arroz"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$.detalles[0].precio").value(1200.0));

        // La venta válida debe pasar por registrarVenta(), no por save() directo.
        verify(ventaService).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-VENTA-05:
        // Valida que no se pueda registrar una venta sin cuerpo JSON.

        postVentaSinBody()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos de la venta son obligatorios"));

        // Sin datos de entrada, el controlador no debe delegar la operación al servicio.
        verify(ventaService, never()).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaSinStockSuficienteDebeRetornarBadRequest() throws Exception {
        // REQ-VENTA-06:
        // Valida que una venta sea rechazada cuando no existe stock suficiente.

        when(ventaService.registrarVenta(any(Venta.class)))
                .thenThrow(new IllegalArgumentException("No existe stock suficiente para registrar la venta"));

        postVenta(ventaJsonSinStockSuficiente())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No existe stock suficiente para registrar la venta"));

        verify(ventaService).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaConUsuarioInvalidoDebeRetornarBadRequest() throws Exception {
        // REQ-VENTA-07:
        // Valida que una venta sea rechazada cuando no está asociada a un usuario válido.

        when(ventaService.registrarVenta(any(Venta.class)))
                .thenThrow(new IllegalArgumentException("La venta debe estar asociada a un usuario válido"));

        postVenta(ventaJsonConUsuarioInvalido())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La venta debe estar asociada a un usuario válido"));

        verify(ventaService).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaSinDetallesDebeRetornarBadRequest() throws Exception {
        // REQ-VENTA-08:
        // Valida que una venta sin detalles de productos sea rechazada por la lógica de negocio.

        when(ventaService.registrarVenta(any(Venta.class)))
                .thenThrow(new IllegalArgumentException("La venta debe contener al menos un detalle"));

        postVenta(ventaJsonSinDetalles())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La venta debe contener al menos un detalle"));

        verify(ventaService).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaConProductoInvalidoDebeRetornarBadRequest() throws Exception {
        // REQ-VENTA-09:
        // Valida que una venta sea rechazada cuando uno de sus detalles no tiene producto válido.

        when(ventaService.registrarVenta(any(Venta.class)))
                .thenThrow(new IllegalArgumentException("Cada detalle debe estar asociado a un producto válido"));

        postVenta(ventaJsonConProductoInvalido())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cada detalle debe estar asociado a un producto válido"));

        verify(ventaService).registrarVenta(any(Venta.class));
        verify(ventaService, never()).save(any(Venta.class));
    }

    private ResultActions postVenta(String json) throws Exception {
        return mockMvc.perform(post(API_VENTAS)
                .contentType(JSON)
                .content(json));
    }

    private ResultActions postVentaSinBody() throws Exception {
        return mockMvc.perform(post(API_VENTAS)
                .contentType(JSON));
    }

    @NonNull
    private String ventaJsonValida() {
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
    private String ventaJsonSinStockSuficiente() {
        return """
                {
                    "usuarioId": 1,
                    "detalles": [
                        {
                            "productoId": 1,
                            "cantidad": 99,
                            "precio": 1200.0
                        }
                    ]
                }
                """;
    }

    @NonNull
    private String ventaJsonConUsuarioInvalido() {
        return """
                {
                    "usuarioId": 99,
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
    private String ventaJsonSinDetalles() {
        return """
                {
                    "usuarioId": 1,
                    "detalles": []
                }
                """;
    }

    @NonNull
    private String ventaJsonConProductoInvalido() {
        return """
                {
                    "usuarioId": 1,
                    "detalles": [
                        {
                            "productoId": null,
                            "cantidad": 2,
                            "precio": 1200.0
                        }
                    ]
                }
                """;
    }

    private Usuario crearUsuario(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
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
}