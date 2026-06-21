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
        when(inventarioService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_INVENTARIO + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarMovimientoValidoDebeRetornarOk() throws Exception {
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

        verify(inventarioService).registrarMovimiento(any(Inventario.class));
    }

    @Test
    void registrarMovimientoSinBodyDebeRetornarBadRequest() throws Exception {
        postInventarioSinBody()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del movimiento de inventario son obligatorios"));
    }

    @Test
    void registrarMovimientoInvalidoDebeRetornarBadRequest() throws Exception {
        InventarioRequest request = new InventarioRequest(1L, "SALIDA", 99);

        when(inventarioService.registrarMovimiento(any(Inventario.class)))
                .thenThrow(new IllegalArgumentException("No existe stock suficiente para registrar la salida"));

        postInventario(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No existe stock suficiente para registrar la salida"));
    }

    @Test
    void actualizarMovimientoExistenteDebeRetornarOk() throws Exception {
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
        InventarioRequest request = new InventarioRequest(1L, "ENTRADA", 5);

        when(inventarioService.findById(99L)).thenReturn(null);

        putInventario(99L, request)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarMovimientoSinBodyDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void actualizarMovimientoInvalidoDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void eliminarMovimientoExistenteDebeRetornarNoContent() throws Exception {
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
        when(inventarioService.findById(99L)).thenReturn(null);

        mockMvc.perform(delete(API_INVENTARIO + "/99"))
                .andExpect(status().isNotFound());
    }

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