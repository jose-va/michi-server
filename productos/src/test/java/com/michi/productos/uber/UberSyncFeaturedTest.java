package com.michi.productos.uber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import com.michi.productos.repository.ProductRepository;
import com.michi.productos.uber.authorization.AuthService;
import com.michi.productos.uber.client.UberClient;
import com.michi.productos.uber.dto.UberDTO;
import com.michi.productos.uber.mapper.UberMapper;
import com.michi.productos.uber.service.UberService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UberSyncFeaturedTest {

    @Test
    void syncDoesNotModifyNorSendFeatured() throws Exception {
        ProductRepository repository = mock(ProductRepository.class);
        AuthService authService = mock(AuthService.class);
        UberClient client = mock(UberClient.class);
        when(authService.fetchToken()).thenReturn("t");

        Product p = new Product();
        p.setId("1");
        p.setName("A");
        p.setCategory(Category.values()[0]);
        p.setStatus(true);
        p.setFeatured(true);
        when(repository.findAll()).thenReturn(List.of(p));

        UberService service = new UberService(repository, authService, client, new UberMapper());
        ReflectionTestUtils.setField(service, "storeId", "store");
        service.sync();

        assertTrue(p.isFeatured());
        verify(repository, never()).save(any());
        verify(repository, never()).saveAll(any());

        ArgumentCaptor<UberDTO> captor = ArgumentCaptor.forClass(UberDTO.class);
        verify(client).updateProducts(any(), eq("store"), captor.capture());
        String json = new ObjectMapper().writeValueAsString(captor.getValue());
        assertFalse(json.toLowerCase().contains("featured"));
    }
}
