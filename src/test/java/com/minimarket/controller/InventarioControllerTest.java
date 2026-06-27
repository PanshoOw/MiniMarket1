package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.dto.InventarioRequest;
import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.service.InventarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = InventarioController.class,
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
class InventarioControllerTest {

    private static final String API_INVENTARIO = "/api/inventario";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventarioService inventarioService;

    @Test
    void listarMovimientosDebeRetornarOk() throws Exception {
        // REQ-INV-01:
        // Valida que el sistema permita listar movimientos de inventario registrados,
        // incluyendo datos del producto asociado, tipo de movimiento y cantidad.

        Inventario inventario = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        when(inventarioService.findAll()).thenReturn(List.of(inventario));

        mockMvc.perform(get(API_INVENTARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$[0].cantidad").value(5))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].productoNombre").value("Arroz"));
    }

    @Test
    void obtenerMovimientoExistenteDebeRetornarOk() throws Exception {
        // REQ-INV-02:
        // Valida la consulta exitosa de un movimiento de inventario existente.

        Inventario inventario = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 10),
                "SALIDA",
                3
        );

        when(inventarioService.findById(1L)).thenReturn(inventario);

        mockMvc.perform(get(API_INVENTARIO + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.productoNombre").value("Arroz"));
    }

    @Test
    void obtenerMovimientoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-INV-03:
        // Valida que el sistema responda 404 cuando se consulta un movimiento inexistente.

        when(inventarioService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_INVENTARIO + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarMovimientoValidoDebeRetornarOk() throws Exception {
        // REQ-INV-04:
        // Valida que se pueda registrar correctamente un movimiento de entrada de inventario.

        InventarioRequest request = new InventarioRequest(1L, "ENTRADA", 5);

        Inventario guardado = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 15),
                "ENTRADA",
                5
        );

        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(guardado);

        postInventario(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$.cantidad").value(5))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.productoNombre").value("Arroz"));

        // Verifica que el movimiento válido sea delegado al servicio de inventario.
        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void registrarMovimientoSalidaValidaDebeRetornarOk() throws Exception {
        // REQ-INV-05:
        // Valida que se pueda registrar correctamente un movimiento de salida de inventario.

        InventarioRequest request = new InventarioRequest(1L, "SALIDA", 2);

        Inventario guardado = crearInventario(
                2L,
                crearProducto(1L, "Arroz", 8),
                "SALIDA",
                2
        );

        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(guardado);

        postInventario(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA"))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.productoNombre").value("Arroz"));

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void registrarMovimientoSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-INV-06:
        // Valida que no se pueda registrar un movimiento sin cuerpo JSON.

        postInventarioSinBody()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del movimiento de inventario son obligatorios"));

        // Sin datos de entrada, el sistema no debe intentar registrar movimientos.
        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void registrarMovimientoSinProductoDebeRetornarBadRequest() throws Exception {
        // REQ-INV-07:
        // Valida que no se pueda registrar un movimiento sin producto asociado.

        InventarioRequest request = new InventarioRequest(null, "ENTRADA", 5);

        when(inventarioService.registrarMovimiento(any(Inventario.class)))
                .thenThrow(new IllegalArgumentException("El producto asociado al movimiento es obligatorio"));

        postInventario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El producto asociado al movimiento es obligatorio"));

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void registrarMovimientoConCantidadInvalidaDebeRetornarBadRequest() throws Exception {
        // REQ-INV-08:
        // Valida que no se pueda registrar un movimiento con cantidad igual o menor a cero.

        InventarioRequest request = new InventarioRequest(1L, "ENTRADA", 0);

        when(inventarioService.registrarMovimiento(any(Inventario.class)))
                .thenThrow(new IllegalArgumentException("El tipo de movimiento y la cantidad son obligatorios y válidos"));

        postInventario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El tipo de movimiento y la cantidad son obligatorios y válidos"));

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void registrarMovimientoInvalidoDebeRetornarBadRequest() throws Exception {
        // REQ-INV-09:
        // Valida que una salida de inventario sea rechazada cuando no existe stock suficiente.

        InventarioRequest request = new InventarioRequest(1L, "SALIDA", 99);

        when(inventarioService.registrarMovimiento(any(Inventario.class)))
                .thenThrow(new IllegalArgumentException("No existe stock suficiente para registrar la salida"));

        postInventario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No existe stock suficiente para registrar la salida"));

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void actualizarMovimientoExistenteDebeRetornarOk() throws Exception {
        // REQ-INV-10:
        // Valida que se pueda actualizar un movimiento existente con datos válidos.

        Inventario existente = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        InventarioRequest request = new InventarioRequest(1L, "SALIDA", 2);

        Inventario actualizado = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 8),
                "SALIDA",
                2
        );

        when(inventarioService.findById(1L)).thenReturn(existente);
        when(inventarioService.registrarMovimiento(any(Inventario.class))).thenReturn(actualizado);

        putInventario(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA"))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.productoNombre").value("Arroz"));

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void actualizarMovimientoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-INV-11:
        // Valida que no se pueda actualizar un movimiento inexistente.

        InventarioRequest request = new InventarioRequest(1L, "ENTRADA", 5);

        when(inventarioService.findById(99L)).thenReturn(null);

        putInventario(99L, request)
                .andExpect(status().isNotFound());

        // Si el movimiento no existe, no debe registrarse ninguna actualización.
        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void actualizarMovimientoSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-INV-12:
        // Valida que no se pueda actualizar un movimiento sin cuerpo JSON.

        Inventario existente = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        when(inventarioService.findById(1L)).thenReturn(existente);

        putInventarioSinBody(1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del movimiento de inventario son obligatorios"));

        verify(inventarioService, never()).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void actualizarMovimientoInvalidoDebeRetornarBadRequest() throws Exception {
        // REQ-INV-13:
        // Valida que no se pueda actualizar un movimiento con tipo de movimiento o cantidad inválida.

        Inventario existente = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        InventarioRequest request = new InventarioRequest(1L, "", 5);

        when(inventarioService.findById(1L)).thenReturn(existente);
        when(inventarioService.registrarMovimiento(any(Inventario.class)))
                .thenThrow(new IllegalArgumentException("El tipo de movimiento y la cantidad son obligatorios y válidos"));

        putInventario(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El tipo de movimiento y la cantidad son obligatorios y válidos"));

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void eliminarMovimientoExistenteDebeRetornarNoContent() throws Exception {
        // REQ-INV-14:
        // Valida que se pueda eliminar un movimiento de inventario existente.

        Inventario inventario = crearInventario(
                1L,
                crearProducto(1L, "Arroz", 10),
                "ENTRADA",
                5
        );

        when(inventarioService.findById(1L)).thenReturn(inventario);

        mockMvc.perform(delete(API_INVENTARIO + "/1"))
                .andExpect(status().isNoContent());

        verify(inventarioService).deleteById(1L);
    }

    @Test
    void eliminarMovimientoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-INV-15:
        // Valida que el sistema responda 404 al intentar eliminar un movimiento inexistente.

        when(inventarioService.findById(99L)).thenReturn(null);

        mockMvc.perform(delete(API_INVENTARIO + "/99"))
                .andExpect(status().isNotFound());

        verify(inventarioService, never()).deleteById(99L);
    }

    // Métodos auxiliares para centralizar la ejecución de peticiones HTTP sobre el endpoint de inventario.

    private ResultActions postInventario(InventarioRequest request) throws Exception {
        return mockMvc.perform(post(API_INVENTARIO)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions postInventarioSinBody() throws Exception {
        return mockMvc.perform(post(API_INVENTARIO)
                .contentType(JSON));
    }

    private ResultActions putInventario(Long id, InventarioRequest request) throws Exception {
        return mockMvc.perform(put(API_INVENTARIO + "/" + id)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions putInventarioSinBody(Long id) throws Exception {
        return mockMvc.perform(put(API_INVENTARIO + "/" + id)
                .contentType(JSON));
    }

    private String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    // Métodos de construcción de entidades usados exclusivamente para las pruebas unitarias.

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

    private Producto crearProducto(Long id, String nombre, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(stock);
        return producto;
    }
}