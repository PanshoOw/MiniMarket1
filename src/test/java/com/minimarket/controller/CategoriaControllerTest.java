package com.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.dto.CategoriaRequest;
import com.minimarket.entity.Categoria;
import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.service.CategoriaService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
        controllers = CategoriaController.class,
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
class CategoriaControllerTest {

    private static final String API_CATEGORIAS = "/api/categorias";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoriaService categoriaService;

    @Test
    void listarCategoriasDebeRetornarOk() throws Exception {
        Categoria categoria = crearCategoria(1L, "Bebidas");

        when(categoriaService.findAll()).thenReturn(List.of(categoria));

        mockMvc.perform(get(API_CATEGORIAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Bebidas"));
    }

    @Test
    void listarCategoriasVacioDebeRetornarOk() throws Exception {
        when(categoriaService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(API_CATEGORIAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void obtenerCategoriaPorIdExistenteDebeRetornarOk() throws Exception {
        Categoria categoria = crearCategoria(1L, "Bebidas");

        when(categoriaService.findById(1L)).thenReturn(categoria);

        mockMvc.perform(get(API_CATEGORIAS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bebidas"));
    }

    @Test
    void obtenerCategoriaPorIdInexistenteDebeRetornarNotFound() throws Exception {
        when(categoriaService.findById(99L)).thenReturn(null);

        mockMvc.perform(get(API_CATEGORIAS + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void guardarCategoriaValidaDebeRetornarOk() throws Exception {
        CategoriaRequest request = new CategoriaRequest("Bebidas");
        Categoria categoriaGuardada = crearCategoria(1L, "Bebidas");

        when(categoriaService.save(any(Categoria.class))).thenReturn(categoriaGuardada);

        postCategoria(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bebidas"));

        verify(categoriaService).save(any(Categoria.class));
    }

    @Test
    void guardarCategoriaConEspaciosDebeRetornarNombreLimpio() throws Exception {
        CategoriaRequest request = new CategoriaRequest("   Bebidas   ");
        Categoria categoriaGuardada = crearCategoria(1L, "Bebidas");

        when(categoriaService.save(any(Categoria.class))).thenReturn(categoriaGuardada);

        postCategoria(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bebidas"));

        verify(categoriaService).save(any(Categoria.class));
    }

    @Test
    void guardarCategoriaConBodyNullDebeRetornarBadRequest() throws Exception {
        mockMvc.perform(post(API_CATEGORIAS)
                        .contentType(JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de la categoría es obligatorio"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void guardarCategoriaConNombreInvalidoDebeRetornarBadRequest(String nombreInvalido) throws Exception {
        CategoriaRequest request = new CategoriaRequest(nombreInvalido);

        postCategoria(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de la categoría es obligatorio"));
    }

    @Test
    void actualizarCategoriaValidaDebeRetornarOk() throws Exception {
        Categoria categoriaExistente = crearCategoria(1L, "Bebidas");
        CategoriaRequest request = new CategoriaRequest("Lácteos");
        Categoria categoriaActualizada = crearCategoria(1L, "Lácteos");

        when(categoriaService.findById(1L)).thenReturn(categoriaExistente);
        when(categoriaService.save(any(Categoria.class))).thenReturn(categoriaActualizada);

        putCategoria(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Lácteos"));

        verify(categoriaService).save(any(Categoria.class));
    }

    @Test
    void actualizarCategoriaConEspaciosDebeRetornarNombreLimpio() throws Exception {
        Categoria categoriaExistente = crearCategoria(1L, "Bebidas");
        CategoriaRequest request = new CategoriaRequest("   Lácteos   ");
        Categoria categoriaActualizada = crearCategoria(1L, "Lácteos");

        when(categoriaService.findById(1L)).thenReturn(categoriaExistente);
        when(categoriaService.save(any(Categoria.class))).thenReturn(categoriaActualizada);

        putCategoria(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Lácteos"));

        verify(categoriaService).save(any(Categoria.class));
    }

    @Test
    void actualizarCategoriaInexistenteDebeRetornarNotFound() throws Exception {
        CategoriaRequest request = new CategoriaRequest("Lácteos");

        when(categoriaService.findById(99L)).thenReturn(null);

        putCategoria(99L, request)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarCategoriaConBodyNullDebeRetornarBadRequest() throws Exception {
        Categoria categoriaExistente = crearCategoria(1L, "Bebidas");

        when(categoriaService.findById(1L)).thenReturn(categoriaExistente);

        mockMvc.perform(put(API_CATEGORIAS + "/1")
                        .contentType(JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de la categoría es obligatorio"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void actualizarCategoriaConNombreInvalidoDebeRetornarBadRequest(String nombreInvalido) throws Exception {
        Categoria categoriaExistente = crearCategoria(1L, "Bebidas");
        CategoriaRequest request = new CategoriaRequest(nombreInvalido);

        when(categoriaService.findById(1L)).thenReturn(categoriaExistente);

        putCategoria(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de la categoría es obligatorio"));
    }

    @Test
    void eliminarCategoriaExistenteDebeRetornarNoContent() throws Exception {
        Categoria categoria = crearCategoria(1L, "Bebidas");

        when(categoriaService.findById(1L)).thenReturn(categoria);

        mockMvc.perform(delete(API_CATEGORIAS + "/1"))
                .andExpect(status().isNoContent());

        verify(categoriaService).deleteById(1L);
    }

    @Test
    void eliminarCategoriaInexistenteDebeRetornarNotFound() throws Exception {
        when(categoriaService.findById(99L)).thenReturn(null);

        mockMvc.perform(delete(API_CATEGORIAS + "/99"))
                .andExpect(status().isNotFound());
    }

    private ResultActions postCategoria(CategoriaRequest request) throws Exception {
        return mockMvc.perform(post(API_CATEGORIAS)
                .contentType(JSON)
                .content(toJson(request)));
    }

    private ResultActions putCategoria(Long id, CategoriaRequest request) throws Exception {
        return mockMvc.perform(put(API_CATEGORIAS + "/" + id)
                .contentType(JSON)
                .content(toJson(request)));
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
}