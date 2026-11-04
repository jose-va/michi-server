package com.michi.productos.controller;

import com.michi.productos.mapper.ProductMapperImpl;
import com.michi.productos.model.Allergen;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import com.michi.productos.repository.ProductRepository;
import com.michi.productos.service.ProductService;
import com.michi.productos.uber.service.UberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({ProductService.class, ProductMapperImpl.class})
class ProductFeaturedControllerTest {

    private static final String ADMIN = "ROLE_ADMIN";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductRepository repository;

    @MockitoBean
    UberService uberService;

    private Product product(String id, String name, boolean featured, Allergen... allergens) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setCategory(Category.values()[0]);
        p.setAllergens(new ArrayList<>(List.of(allergens)));
        p.setPrice(5);
        p.setStatus(true);
        p.setFeatured(featured);
        return p;
    }

    private Product saved() {
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @BeforeEach
    void stubSave() {
        when(repository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.findFirstByCategoryOrderByPositionDesc(any())).thenReturn(Optional.empty());
    }

    // ---------- creación ----------

    @Test
    void createWithFeaturedTrue() throws Exception {
        mvc.perform(post("/products").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"category\":\"" + Category.values()[0] + "\",\"featured\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(true));
        assertTrue(saved().isFeatured());
    }

    @Test
    void createWithFeaturedFalse() throws Exception {
        mvc.perform(post("/products").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"category\":\"" + Category.values()[0] + "\",\"featured\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
        assertFalse(saved().isFeatured());
    }

    @Test
    void createWithoutFeaturedDefaultsToFalse() throws Exception {
        mvc.perform(post("/products").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"category\":\"" + Category.values()[0] + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
        assertFalse(saved().isFeatured());
    }

    @Test
    void createWithFeaturedNullDefaultsToFalse() throws Exception {
        mvc.perform(post("/products").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"category\":\"" + Category.values()[0] + "\",\"featured\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
        assertFalse(saved().isFeatured());
    }

    // ---------- edición ----------

    @Test
    void updateSetsFeaturedTrue() throws Exception {
        when(repository.findById("1")).thenReturn(Optional.of(product("1", "A", false)));
        mvc.perform(put("/products/1").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(true));
        assertTrue(saved().isFeatured());
    }

    @Test
    void updateSetsFeaturedFalse() throws Exception {
        when(repository.findById("1")).thenReturn(Optional.of(product("1", "A", true)));
        mvc.perform(put("/products/1").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
        assertFalse(saved().isFeatured());
    }

    @Test
    void updateWithoutFeaturedBecomesFalse() throws Exception {
        when(repository.findById("1")).thenReturn(Optional.of(product("1", "A", true)));
        mvc.perform(put("/products/1").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
        assertFalse(saved().isFeatured());
    }

    @Test
    void updateWithFeaturedNullBecomesFalse() throws Exception {
        when(repository.findById("1")).thenReturn(Optional.of(product("1", "A", true)));
        mvc.perform(put("/products/1").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"featured\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
        assertFalse(saved().isFeatured());
    }

    @Test
    void putResendingFullProductKeepsFeatured() throws Exception {
        when(repository.findById("1")).thenReturn(Optional.of(product("1", "A", true, Allergen.values()[0])));
        mvc.perform(put("/products/1").header("User-Role", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\",\"name\":\"A\",\"price\":5,\"featured\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(true));
        assertTrue(saved().isFeatured());
    }

    // ---------- productos antiguos (sin campo en Mongo => false) ----------

    @Test
    void legacyProductReadsAsNotFeatured() throws Exception {
        Product legacy = new Product(); // sin tocar featured, como un documento sin el campo
        legacy.setId("old");
        legacy.setName("Antiguo");
        when(repository.findById("old")).thenReturn(Optional.of(legacy));
        mvc.perform(get("/products/old"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(false));
    }

    // ---------- GET /products/featured ----------

    @Test
    void featuredReturnsOnlyFeaturedAsPlainArray() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                product("1", "A", true), product("2", "B", false), product("3", "C", true)));
        mvc.perform(get("/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("3"))
                .andExpect(jsonPath("$[0].featured").value(true))
                .andExpect(jsonPath("$[1].featured").value(true));
    }

    @Test
    void featuredReturnsEmptyArrayWhenNone() throws Exception {
        when(repository.findAll()).thenReturn(List.of(product("1", "A", false)));
        mvc.perform(get("/products/featured"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void featuredFiltersByName() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                product("1", "Nigiri salmón", true), product("2", "Maki atún", true),
                product("3", "Salmón fuera", false)));
        mvc.perform(get("/products/featured").param("name", "nigiri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void featuredFiltersByRepeatedAllergensLikeSearch() throws Exception {
        Allergen a0 = Allergen.values()[0];
        Allergen a1 = Allergen.values()[1];
        Allergen a2 = Allergen.values()[2];
        when(repository.findAll()).thenReturn(List.of(
                product("1", "A", true, a0),
                product("2", "B", true, a1),
                product("3", "C", true, a2),
                product("4", "D", false, a2)));
        mvc.perform(get("/products/featured").param("allergens", a0.name(), a1.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("3"));
    }

    @Test
    void featuredIgnoresCategory() throws Exception {
        Category other = Category.values()[Category.values().length - 1];
        when(repository.findAll()).thenReturn(List.of(product("1", "A", true)));
        mvc.perform(get("/products/featured").param("category", other.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/products/featured").param("category", "NO_EXISTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void featuredIsNotTreatedAsId() throws Exception {
        when(repository.findAll()).thenReturn(List.of());
        mvc.perform(get("/products/featured")).andExpect(status().isOk());
        verify(repository, never()).findById("featured");
    }

    // ---------- campo en lecturas existentes ----------

    @Test
    void featuredFieldAppearsInExistingReads() throws Exception {
        Product p = product("1", "A", true);
        when(repository.findAll()).thenReturn(List.of(p));
        when(repository.findById("1")).thenReturn(Optional.of(p));

        mvc.perform(get("/products/all")).andExpect(jsonPath("$[0].featured").value(true));
        mvc.perform(get("/products?page=0")).andExpect(jsonPath("$.content[0].featured").value(true));
        mvc.perform(get("/products/1")).andExpect(jsonPath("$.featured").value(true));
        mvc.perform(get("/products/search")).andExpect(jsonPath("$[0].featured").value(true));
    }

    // ---------- activate / deactivate no tocan featured ----------

    @Test
    void activateDeactivateAndActivateAllKeepFeatured() throws Exception {
        when(repository.findById("1")).thenReturn(Optional.of(product("1", "A", true)));
        mvc.perform(patch("/products/1/deactivate").header("User-Role", ADMIN))
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.featured").value(true));
        mvc.perform(patch("/products/1/activate").header("User-Role", ADMIN))
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.featured").value(true));

        Product inactive = product("2", "B", true);
        inactive.setStatus(false);
        when(repository.findByStatusFalse()).thenReturn(List.of(inactive));
        when(repository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        mvc.perform(patch("/products").header("User-Role", ADMIN))
                .andExpect(jsonPath("$[0].status").value(true))
                .andExpect(jsonPath("$[0].featured").value(true));
    }

    // ---------- permisos ----------

    @Test
    void writeEndpointsStillRequireAdmin() throws Exception {
        String user = "ROLE_USER";
        mvc.perform(post("/products").header("User-Role", user).contentType(MediaType.APPLICATION_JSON)
                .content("{\"featured\":true}")).andExpect(status().isForbidden());
        mvc.perform(put("/products/1").header("User-Role", user).contentType(MediaType.APPLICATION_JSON)
                .content("{\"featured\":true}")).andExpect(status().isForbidden());
        mvc.perform(delete("/products/1").header("User-Role", user)).andExpect(status().isForbidden());
        mvc.perform(patch("/products/1/activate").header("User-Role", user)).andExpect(status().isForbidden());
        mvc.perform(patch("/products/1/deactivate").header("User-Role", user)).andExpect(status().isForbidden());
        mvc.perform(patch("/products").header("User-Role", user)).andExpect(status().isForbidden());
        mvc.perform(post("/products/uber").header("User-Role", user)).andExpect(status().isForbidden());
        verify(repository, never()).save(any());
    }
}
