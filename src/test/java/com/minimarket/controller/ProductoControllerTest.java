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
        when(productoService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_PRODUCTOS + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void guardarProductoValidoDebeRetornarOk() throws Exception {
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

        verify(productoService).save(any(Producto.class));
    }

    @Test
    void guardarProductoSinBodyDebeRetornarBadRequest() throws Exception {
        postProductoSinBody()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los datos del producto son obligatorios"));
    }

    @Test
    void guardarProductoSinNombreDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("", 1500.0, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del producto es obligatorio"));
    }

    @Test
    void guardarProductoConContenidoPeligrosoDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("<script>alert('xss')</script>", 1500.0, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre del producto contiene caracteres no permitidos"));
    }

    @Test
    void guardarProductoConPrecioNuloDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("Leche", null, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El precio debe ser mayor a cero"));
    }

    @Test
    void guardarProductoConPrecioCeroDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("Leche", 0.0, 10, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El precio debe ser mayor a cero"));
    }

    @Test
    void guardarProductoConStockNegativoDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("Leche", 1500.0, -1, 1L);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El stock no puede ser negativo"));
    }

    @Test
    void guardarProductoSinCategoriaDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, null);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El id de la categoría es obligatorio"));
    }

    @Test
    void guardarProductoConCategoriaInexistenteDebeRetornarBadRequest() throws Exception {
        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, 99L);

        when(categoriaService.findById(99L)).thenReturn(null);

        postProducto(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La categoría indicada no existe"));
    }

    @Test
    void actualizarProductoValidoDebeRetornarOk() throws Exception {
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

        verify(productoService).save(any(Producto.class));
    }

    @Test
    void actualizarProductoInexistenteDebeRetornarNotFound() throws Exception {
        ProductoRequest request = new ProductoRequest("Leche", 1500.0, 10, 1L);

        when(productoService.findById(99L)).thenReturn(null);

        putProducto(99L, request)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarProductoSinBodyDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void actualizarProductoSinNombreDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void actualizarProductoConContenidoPeligrosoDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void actualizarProductoConPrecioInvalidoDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void actualizarProductoConStockNegativoDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void actualizarProductoConCategoriaInexistenteDebeRetornarBadRequest() throws Exception {
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
    }

    @Test
    void eliminarProductoExistenteDebeRetornarNoContent() throws Exception {
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

        verify(productoService).deleteById(1L);
    }

    @Test
    void eliminarProductoInexistenteDebeRetornarNotFound() throws Exception {
        when(productoService.findById(99L)).thenReturn(null);

        mockMvc.perform(delete(API_PRODUCTOS + "/99"))
                .andExpect(status().isNotFound());
    }

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