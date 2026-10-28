package com.michi.productos.uber.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Definimos la estructura del token de acceso que recibimos por parte de la API de Uber para la autorización
 */
@Data
public class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    private String scope;
}
