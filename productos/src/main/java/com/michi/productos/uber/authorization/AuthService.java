package com.michi.productos.uber.authorization;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UberAuthClient uberAuthClient; // Orquestador de la llamada

    @Value("${uber.client-id}")
    private String clientId; // Nuestro identificador, proporcionado por Uber
    @Value("${uber.client-secret}")
    private String clientSecret; // Nuestro número secreto, también proporcionado por ellos

    /**D
     * Aquí hacemos una petición para obtener el token de acceso a partir de nuestras credenciales
     * Hay que poner que queremos un token de tipo client_credentials y especificar los permisos que queremos
     */
    public String fetchToken() {
        TokenResponse response = uberAuthClient.getAccessToken(
                clientId,
                clientSecret,
                "client_credentials",
                "eats.store"
        );
        return response.getAccessToken();
    }
}
