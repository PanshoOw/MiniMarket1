package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.dto.ProductoRequest;
import com.minimarket.entity.Categoria;
import com.minimarket.entity.Producto;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.service.CategoriaService;
import com.minimarket.service.ProductoService;
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
        controllers = ProductoController.class,
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
class ProductoControllerTest {

    private static final String API_PRODUCTOS = "/api/productos";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private CategoriaService categoriaService;

    @Test
    void listarProductosDebeRetornarOk() throws Exception {
        // REQ-PROD-01:
        // Valida que el endpoint permita listar productos existentes y retorne sus datos principales,
        // incluyendo información asociada de categoría.

        Producto producto = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        when(productoService.findAll()).thenReturn(List.of(producto));

        mockMvc.perform(get(API_PRODUCTOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Leche"))
                .andExpect(jsonPath("$[0].precio").value(1500.0))
                .andExpect(jsonPath("$[0].stock").value(10))
                .andExpect(jsonPath("$[0].categoriaId").value(1))
                .andExpect(jsonPath("$[0].categoriaNombre").value("Bebidas"));
    }

    @Test
    void obtenerProductoPorIdExistenteDebeRetornarOk() throws Exception {
        // REQ-PROD-02:
        // Valida la consulta exitosa de un producto existente mediante su identificador.

        Producto producto = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        when(productoService.findById(1L)).thenReturn(producto);

        mockMvc.perform(get(API_PRODUCTOS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Leche"))
                .andExpect(jsonPath("$.precio").value(1500.0))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.categoriaId").value(1))
                .andExpect(jsonPath("$.categoriaNombre").value("Bebidas"));
    }

    @Test
    void obtenerProductoPorIdInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-PROD-03:
        // Valida que el sistema responda 404 cuando se consulta un producto inexistente.

        when(productoService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_PRODUCTOS + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void guardarProductoValidoDebeRetornarOk() throws Exception {
        // REQ-PROD-04:
        // Valida que el sistema permita crear un producto cuando los datos son válidos,
        // existe una categoría asociada y se cumplen las reglas de negocio.

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, 1L);
        Categoria categoria = crearCategoria(1L, "Bebidas");

        Producto productoGuardado = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                categoria
        );

        when(categoriaService.findById(1L)).thenReturn(categoria);
        when(productoService.save(any(Producto.class))).thenReturn(productoGuardado);

        postProducto(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Leche"))
                .andExpect(jsonPath("$.precio").value(1500.0))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.categoriaId").value(1))
                .andExpect(jsonPath("$.categoriaNombre").value("Bebidas"));

        // Verifica que el servicio de guardado sea invocado cuando la solicitud es válida.
        verify(productoService).save(any(Producto.class));
    }

    @Test
    void guardarProductoSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-05:
        // Valida que no se pueda crear un producto si no se envía cuerpo JSON.

        postProductoSinBody()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del producto son obligatorios"));

        // Sin datos de entrada, el sistema no debe intentar persistir un producto.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoSinNombreDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-06:
        // Valida que no se pueda crear un producto sin nombre válido.

        ProductoRequest request = new ProductoRequest("", 1500.0, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del producto es obligatorio"));

        // Un producto sin nombre válido no debe ser guardado.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoConContenidoPeligrosoDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-07:
        // Valida que el sistema rechace nombres de producto con contenido potencialmente peligroso.

        ProductoRequest request = new ProductoRequest("<script>alert('xss')</script>", 1500.0, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del producto contiene caracteres no permitidos"));

        // El contenido peligroso debe bloquear la persistencia del producto.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoConPrecioNuloDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-08:
        // Valida que no se pueda crear un producto con precio nulo.

        ProductoRequest request = new ProductoRequest("Leche", null, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El precio debe ser mayor a cero"));

        // Un precio nulo invalida la operación de guardado.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoConPrecioCeroDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-09:
        // Valida que no se pueda crear un producto con precio igual a cero.

        ProductoRequest request = new ProductoRequest("Leche", 0.0, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El precio debe ser mayor a cero"));

        // Un precio igual a cero invalida la operación de guardado.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoConStockNegativoDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-10:
        // Valida que no se pueda crear un producto con stock negativo.

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, -1, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El stock no puede ser negativo"));

        // El stock negativo no debe persistirse.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoSinCategoriaDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-11:
        // Valida que no se pueda crear un producto sin categoría asociada.

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, null);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El id de la categoría es obligatorio"));

        // Sin categoría, el producto no debe guardarse.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void guardarProductoConCategoriaInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-12:
        // Valida que no se pueda crear un producto asociado a una categoría inexistente.

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, 99L);

        when(categoriaService.findById(99L)).thenReturn(null);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La categoría indicada no existe"));

        // Si la categoría no existe, no debe persistirse el producto.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoValidoDebeRetornarOk() throws Exception {
        // REQ-PROD-13:
        // Valida que el sistema permita actualizar un producto existente cuando los datos enviados son válidos.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        ProductoRequest request = new ProductoRequest("Leche descremada", 1800.0, 8, 1L);
        Categoria categoria = crearCategoria(1L, "Bebidas");

        Producto productoActualizado = crearProducto(
                1L,
                "Leche descremada",
                1800.0,
                8,
                categoria
        );

        when(productoService.findById(1L)).thenReturn(productoExistente);
        when(categoriaService.findById(1L)).thenReturn(categoria);
        when(productoService.save(any(Producto.class))).thenReturn(productoActualizado);

        putProducto(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Leche descremada"))
                .andExpect(jsonPath("$.precio").value(1800.0))
                .andExpect(jsonPath("$.stock").value(8))
                .andExpect(jsonPath("$.categoriaId").value(1))
                .andExpect(jsonPath("$.categoriaNombre").value("Bebidas"));

        // Verifica que la actualización válida invoque la persistencia del producto.
        verify(productoService).save(any(Producto.class));
    }

    @Test
    void actualizarProductoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-PROD-14:
        // Valida que no se pueda actualizar un producto inexistente.

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, 1L);

        when(productoService.findById(99L)).thenReturn(null);

        putProducto(99L, request)
                .andExpect(status().isNotFound());

        // Si el producto no existe, no debe intentarse guardar una actualización.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoSinBodyDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-15:
        // Valida que no se pueda actualizar un producto si no se envía cuerpo JSON.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        when(productoService.findById(1L)).thenReturn(productoExistente);

        putProductoSinBody(1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del producto son obligatorios"));

        // Sin datos de actualización, no debe persistirse ningún cambio.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoSinNombreDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-16:
        // Valida que no se pueda actualizar un producto dejando el nombre vacío.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        ProductoRequest request = new ProductoRequest("   ", 1500.0, 10, 1L);

        when(productoService.findById(1L)).thenReturn(productoExistente);

        putProducto(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del producto es obligatorio"));

        // Un nombre vacío invalida la actualización.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoConContenidoPeligrosoDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-17:
        // Valida que no se pueda actualizar un producto usando contenido potencialmente peligroso.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        ProductoRequest request = new ProductoRequest("javascript:alert('xss')", 1500.0, 10, 1L);

        when(productoService.findById(1L)).thenReturn(productoExistente);

        putProducto(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del producto contiene caracteres no permitidos"));

        // La validación de seguridad debe impedir que el producto sea actualizado.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoConPrecioInvalidoDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-18:
        // Valida que no se pueda actualizar un producto con precio menor o igual a cero.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        ProductoRequest request = new ProductoRequest("Leche", -100.0, 10, 1L);

        when(productoService.findById(1L)).thenReturn(productoExistente);

        putProducto(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El precio debe ser mayor a cero"));

        // Un precio inválido no debe persistirse.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoConStockNegativoDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-19:
        // Valida que no se pueda actualizar un producto dejando stock negativo.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, -1, 1L);

        when(productoService.findById(1L)).thenReturn(productoExistente);

        putProducto(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El stock no puede ser negativo"));

        // El stock negativo invalida la actualización del producto.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoConCategoriaInexistenteDebeRetornarBadRequest() throws Exception {
        // REQ-PROD-20:
        // Valida que no se pueda actualizar un producto asociándolo a una categoría inexistente.

        Producto productoExistente = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, 99L);

        when(productoService.findById(1L)).thenReturn(productoExistente);
        when(categoriaService.findById(99L)).thenReturn(null);

        putProducto(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La categoría indicada no existe"));

        // Si la categoría no existe, la actualización no debe persistirse.
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void eliminarProductoExistenteDebeRetornarNoContent() throws Exception {
        // REQ-PROD-21:
        // Valida que el sistema permita eliminar un producto existente.

        Producto producto = crearProducto(
                1L,
                "Leche",
                1500.0,
                10,
                crearCategoria(1L, "Bebidas")
        );

        when(productoService.findById(1L)).thenReturn(producto);

        mockMvc.perform(delete(API_PRODUCTOS + "/1"))
                .andExpect(status().isNoContent());

        // Verifica que el servicio de eliminación sea invocado con el ID correcto.
        verify(productoService).deleteById(1L);
    }

    @Test
    void eliminarProductoInexistenteDebeRetornarNotFound() throws Exception {
        // REQ-PROD-22:
        // Valida que el sistema responda 404 al intentar eliminar un producto inexistente.

        when(productoService.findById(99L)).thenReturn(null);

        mockMvc.perform(delete(API_PRODUCTOS + "/99"))
                .andExpect(status().isNotFound());

        // Si el producto no existe, no debe intentarse eliminar.
        verify(productoService, never()).deleteById(99L);
    }

    // Métodos auxiliares para centralizar la ejecución de peticiones HTTP sobre el endpoint de productos.

    private ResultActions postProducto(ProductoRequest request) throws Exception {
        return mockMvc.perform(post(API_PRODUCTOS)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions postProductoSinBody() throws Exception {
        return mockMvc.perform(post(API_PRODUCTOS)
                .contentType(JSON));
    }

    private ResultActions putProducto(Long id, ProductoRequest request) throws Exception {
        return mockMvc.perform(put(API_PRODUCTOS + "/" + id)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions putProductoSinBody(Long id) throws Exception {
        return mockMvc.perform(put(API_PRODUCTOS + "/" + id)
                .contentType(JSON));
    }

    private String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    // Métodos de construcción de entidades usados exclusivamente para las pruebas unitarias.

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
}