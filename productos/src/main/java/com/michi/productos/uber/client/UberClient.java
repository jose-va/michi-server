package com.michi.productos.uber.client;

import com.michi.productos.uber.dto.UberDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "uber-client", url = "https://test-api.uber.com")
public interface UberClient {

    @PutMapping("/v2/eats/stores/{storeId}/menus")
    void updateProducts(
            @RequestHeader("Authorization") String accessToken,
            @PathVariable String storeId,
            @RequestBody UberDTO uber
    );
}

