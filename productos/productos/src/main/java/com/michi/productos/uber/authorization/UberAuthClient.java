package com.michi.productos.uber.authorization;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;

@FeignClient(name = "uber-auth", url = "https://sandbox-login.uber.com")
public interface UberAuthClient {

    /**
     * Esta es la petición que realizaremos a la API de Uber para la autorización.
     * Contiene el identificador de cliente, el secret, el tipo de token y los permisos asociados a este.
     */
    @PostMapping(value = "/oauth/v2/token", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    TokenResponse getAccessToken(
            @RequestPart("client_id") String clientId,
            @RequestPart("client_secret") String clientSecret,
            @RequestPart("grant_type") String grantType,
            @RequestPart("scope") String scope
    );
}
