package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.dto.CarritoRequest;
import com.minimarket.entity.Carrito;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.service.CarritoService;
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

import java.util.List;
import java.util.Optional;

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
        controllers = CarritoController.class,
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
class CarritoControllerTest {

    private static final String API_CARRITO = "/api/carrito";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CarritoService carritoService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ProductoRepository productoRepository;

    @Test
    void agregarProductoConStockSuficienteDebeRetornarOk() throws Exception {
        // REQ-CARRITO-01:
        // Valida que el sistema permita agregar un producto al carrito cuando el usuario existe,
        // el producto existe y la cantidad solicitada no supera el stock disponible.

        CarritoRequest request = new CarritoRequest(1L, 1L, 2);

        simularUsuarioExistente(1L, "cliente1");
        simularProductoExistente(1L, "Leche", 10);
        simularGuardadoCarritoConId(1L);

        postCarrito(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.username").value("cliente1"))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.nombreProducto").value("Leche"))
                .andExpect(jsonPath("$.cantidad").value(2));

        // Verifica que, al cumplirse las reglas de negocio, el carrito efectivamente se guarde.
        verify(carritoService).save(any(Carrito.class));
    }

    @Test
    void agregarProductoConStockInsuficienteDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-02:
        // Valida que el sistema rechace agregar un producto cuando la cantidad solicitada
        // supera el stock disponible.

        CarritoRequest request = new CarritoRequest(1L, 1L, 10);

        simularUsuarioExistente(1L, "cliente1");
        simularProductoExistente(1L, "Leche", 5);

        postCarrito(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Stock insuficiente para el producto indicado"));

        // Verifica que no se persista información cuando la regla de stock falla.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void agregarProductoConUsuarioInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-03:
        // Valida que no se pueda agregar un producto al carrito si el usuario indicado no existe.

        CarritoRequest request = new CarritoRequest(99L, 1L, 1);

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        simularProductoExistente(1L, "Leche", 10);

        postCarrito(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El usuario indicado no existe"));

        // Evita que se cree un carrito asociado a un usuario inválido.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void agregarProductoConProductoInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-04:
        // Valida que no se pueda agregar al carrito un producto inexistente.

        CarritoRequest request = new CarritoRequest(1L, 99L, 1);

        simularUsuarioExistente(1L, "cliente1");
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        postCarrito(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El producto indicado no existe"));

        // Verifica que el servicio de guardado no sea invocado ante un producto inválido.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void agregarProductoConCantidadNulaDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-05:
        // Valida el caso límite donde la cantidad enviada es nula.

        CarritoRequest request = new CarritoRequest(1L, 1L, null);

        postCarrito(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser mayor a cero"));

        // Una cantidad nula no debe generar persistencia del carrito.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void agregarProductoConCantidadCeroDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-06:
        // Valida el caso límite donde la cantidad enviada es cero.

        CarritoRequest request = new CarritoRequest(1L, 1L, 0);

        postCarrito(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser mayor a cero"));

        // Una cantidad igual a cero no debe generar persistencia del carrito.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void agregarProductoConCantidadNegativaDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-07:
        // Valida el caso límite donde la cantidad enviada es negativa.

        CarritoRequest request = new CarritoRequest(1L, 1L, -1);

        postCarrito(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser mayor a cero"));

        // Una cantidad negativa no debe generar persistencia del carrito.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void agregarProductoSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-08:
        // Valida que el endpoint rechace solicitudes sin cuerpo JSON.

        postCarritoSinBody()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del carrito son obligatorios"));

        // Sin datos de entrada, el sistema no debe intentar guardar un carrito.
        verify(carritoService, never()).save(any(Carrito.class));
    }

    @Test
    void listarCarritoDebeRetornarOk() throws Exception {
        // REQ-CARRITO-09:
        // Valida que el endpoint de listado retorne los carritos existentes con los datos
        // relevantes de usuario, producto y cantidad.

        Carrito carrito = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                2
        );

        when(carritoService.findAll()).thenReturn(List.of(carrito));

        mockMvc.perform(get(API_CARRITO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].usuarioId").value(1))
                .andExpect(jsonPath("$[0].username").value("cliente1"))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].nombreProducto").value("Leche"))
                .andExpect(jsonPath("$[0].cantidad").value(2));
    }

    @Test
    void obtenerCarritoPorIdExistenteDebeRetornarOk() throws Exception {
        // REQ-CARRITO-10:
        // Valida la consulta exitosa de un carrito existente mediante su identificador.

        Carrito carrito = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                2
        );

        when(carritoService.findById(1L)).thenReturn(carrito);

        mockMvc.perform(get(API_CARRITO + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("cliente1"))
                .andExpect(jsonPath("$.nombreProducto").value("Leche"));
    }

    @Test
    void obtenerCarritoPorIdInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-CARRITO-11:
        // Valida que el sistema responda 404 cuando se consulta un carrito inexistente.

        when(carritoService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_CARRITO + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerCarritoConUsuarioNuloDebeRetornarOkSinDatosUsuario() throws Exception {
        // REQ-CARRITO-12:
        // Valida que la respuesta sea tolerante cuando el carrito no tiene usuario asociado,
        // evitando errores por referencias nulas.

        Carrito carrito = crearCarrito(
                1L,
                null,
                crearProducto(1L, "Leche", 10),
                2
        );

        when(carritoService.findById(1L)).thenReturn(carrito);

        mockMvc.perform(get(API_CARRITO + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuarioId").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    void obtenerCarritoConProductoNuloDebeRetornarOkSinDatosProducto() throws Exception {
        // REQ-CARRITO-13:
        // Valida que la respuesta sea tolerante cuando el carrito no tiene producto asociado,
        // evitando errores por referencias nulas.

        Carrito carrito = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                null,
                2
        );

        when(carritoService.findById(1L)).thenReturn(carrito);

        mockMvc.perform(get(API_CARRITO + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productoId").doesNotExist())
                .andExpect(jsonPath("$.nombreProducto").doesNotExist());
    }

    @Test
    void actualizarCarritoConStockSuficienteDebeRetornarOk() throws Exception {
        // REQ-CARRITO-14:
        // Valida que el sistema permita actualizar un carrito existente cuando los nuevos
        // datos son válidos y existe stock suficiente.

        CarritoRequest request = new CarritoRequest(1L, 1L, 3);
        Carrito existente = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                1
        );

        when(carritoService.findById(1L)).thenReturn(existente);
        simularUsuarioExistente(1L, "cliente1");
        simularProductoExistente(1L, "Leche", 10);
        when(carritoService.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        putCarrito(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.username").value("cliente1"))
                .andExpect(jsonPath("$.nombreProducto").value("Leche"));
    }

    @Test
    void actualizarCarritoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-CARRITO-15:
        // Valida que no se pueda actualizar un carrito inexistente.

        CarritoRequest request = new CarritoRequest(1L, 1L, 3);

        when(carritoService.findById(99L)).thenReturn(null);

        putCarrito(99L, request)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarCarritoConStockInsuficienteDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-16:
        // Valida que la actualización del carrito sea rechazada cuando la nueva cantidad
        // supera el stock disponible.

        CarritoRequest request = new CarritoRequest(1L, 1L, 20);
        Carrito existente = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                1
        );

        when(carritoService.findById(1L)).thenReturn(existente);
        simularUsuarioExistente(1L, "cliente1");
        simularProductoExistente(1L, "Leche", 10);

        putCarrito(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Stock insuficiente para el producto indicado"));
    }

    @Test
    void actualizarCarritoSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-17:
        // Valida que la actualización sea rechazada cuando no se envía cuerpo JSON.

        Carrito existente = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                1
        );

        when(carritoService.findById(1L)).thenReturn(existente);

        putCarritoSinBody(1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del carrito son obligatorios"));
    }

    @Test
    void actualizarCarritoConUsuarioInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-18:
        // Valida que no se pueda actualizar un carrito asociándolo a un usuario inexistente.

        CarritoRequest request = new CarritoRequest(99L, 1L, 1);
        Carrito existente = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                1
        );

        when(carritoService.findById(1L)).thenReturn(existente);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        simularProductoExistente(1L, "Leche", 10);

        putCarrito(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El usuario indicado no existe"));
    }

    @Test
    void actualizarCarritoConProductoInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-19:
        // Valida que no se pueda actualizar un carrito asociándolo a un producto inexistente.

        CarritoRequest request = new CarritoRequest(1L, 99L, 1);
        Carrito existente = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                1
        );

        when(carritoService.findById(1L)).thenReturn(existente);
        simularUsuarioExistente(1L, "cliente1");
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        putCarrito(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El producto indicado no existe"));
    }

    @Test
    void actualizarCarritoConCantidadNegativaDebeRetornarBadRequest() throws Exception {
        // REQ-CARRITO-20:
        // Valida que la actualización rechace cantidades negativas como caso límite.

        CarritoRequest request = new CarritoRequest(1L, 1L, -1);
        Carrito existente = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                1
        );

        when(carritoService.findById(1L)).thenReturn(existente);

        putCarrito(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser mayor a cero"));
    }

    @Test
    void eliminarCarritoExistenteDebeRetornarNoContent() throws Exception {
        // REQ-CARRITO-21:
        // Valida que un carrito existente pueda ser eliminado correctamente.

        Carrito carrito = crearCarrito(
                1L,
                crearUsuario(1L, "cliente1"),
                crearProducto(1L, "Leche", 10),
                2
        );

        when(carritoService.findById(1L)).thenReturn(carrito);

        mockMvc.perform(delete(API_CARRITO + "/1"))
                .andExpect(status().isNoContent());

        // Verifica que el servicio de eliminación sea invocado con el ID correcto.
        verify(carritoService).deleteById(1L);
    }

    @Test
    void eliminarCarritoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-CARRITO-22:
        // Valida que el sistema responda 404 al intentar eliminar un carrito inexistente.

        when(carritoService.findById(99L)).thenReturn(null);

        mockMvc.perform(delete(API_CARRITO + "/99"))
                .andExpect(status().isNotFound());
    }

    // Métodos auxiliares para centralizar la ejecución de peticiones HTTP sobre el endpoint de carrito.

    private ResultActions postCarrito(CarritoRequest request) throws Exception {
        return mockMvc.perform(post(API_CARRITO)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions postCarritoSinBody() throws Exception {
        return mockMvc.perform(post(API_CARRITO)
                .contentType(JSON));
    }

    private ResultActions putCarrito(Long id, CarritoRequest request) throws Exception {
        return mockMvc.perform(put(API_CARRITO + "/" + id)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions putCarritoSinBody(Long id) throws Exception {
        return mockMvc.perform(put(API_CARRITO + "/" + id)
                .contentType(JSON));
    }

    private String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    // Métodos auxiliares para configurar datos simulados con Mockito.

    private void simularUsuarioExistente(Long id, String username) {
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(crearUsuario(id, username)));
    }

    private void simularProductoExistente(Long id, String nombre, Integer stock) {
        when(productoRepository.findById(id)).thenReturn(Optional.of(crearProducto(id, nombre, stock)));
    }

    private void simularGuardadoCarritoConId(Long id) {
        when(carritoService.save(any(Carrito.class))).thenAnswer(invocation -> {
            Carrito carrito = invocation.getArgument(0);
            carrito.setId(id);
            return carrito;
        });
    }

    // Métodos de construcción de entidades usados exclusivamente para las pruebas unitarias.

    private Usuario crearUsuario(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        return usuario;
    }

    private Producto crearProducto(Long id, String nombre, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(stock);
        return producto;
    }

    private Carrito crearCarrito(Long id, Usuario usuario, Producto producto, Integer cantidad) {
        Carrito carrito = new Carrito();
        carrito.setId(id);
        carrito.setUsuario(usuario);
        carrito.setProducto(producto);
        carrito.setCantidad(cantidad);
        return carrito;
    }
}