package com.michi.productos.uber.service;

import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import com.michi.productos.repository.ProductRepository;
import com.michi.productos.uber.client.UberClient;
import com.michi.productos.uber.authorization.AuthService;
import com.michi.productos.uber.dto.UberDTO;
import com.michi.productos.uber.mapper.UberMapper;
import com.michi.productos.uber.dto.UberDTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UberService {

    private final ProductRepository productRepository; // Productos almacenados en nuestra base de datos MongoDB
    private final AuthService authService; // Generación del token de acceso para la petición
    private final UberClient uberClient; // Orquestador de la llamada a la API de Uber
    private final UberMapper uberMapper; // Transformador de nuestras productos en la base de datos

    @Value("${uber.store-id}")
    private String storeId; // Identificador de nuestra tienda proporcionado por Uber

    public void sync() {
        try {
            // 1. AUTORIZACIÓN: Obtenemos el token OAuth2 y le añadimos el prefijo Bearer.
            String accessToken = "Bearer " + authService.fetchToken();

            // 2. EXTRACCIÓN: Traemos todos los productos de MongoDB.
            List<Product> products = productRepository.findAll();

            // 3. MAPEO DE ITEMS: Filtramos solo los que tienen stock y los convertimos al formato UberItem.
            List<UberItem> uberItems = products.stream()
                    .filter(Product::isStatus)
                    .map(uberMapper::toUberItem)
                    .toList();

            // 4. AGRUPACIÓN: Organizamos los productos por su Categoría para crear las secciones de Uber
            Map<Category, List<Product>> groupedProducts = products
                    .stream()
                    .filter(Product::isStatus)
                    .collect(Collectors.groupingBy(Product::getCategory));

            // 5. MAPEO DE CATEGORÍAS: Convertimos cada grupo en una UberCategory.
            List<UberCategory> uberCategories = groupedProducts
                    .entrySet()
                    .stream()
                    .map(entry -> uberMapper.toUberCategory(entry.getKey(), entry.getValue()))
                    .toList();

            // 6. ORDENACIÓN: Definimos qué categorías queremos que salgan primero en la app.
            Map<String, Integer> priorityMap = Map.of(
                    "HOT_STARTERS", 1,
                    "FRESH_STARTERS", 2,
                    "HOT_DISHES", 3,
                    "NIGIRI_SUSHI", 4,
                    "SPECIAL_MAKI", 5
            );

            // 7. SEPARACIÓN DE MENÚS: Carta principal y Bebidas/Postres
            // Menú 1: Comida
            List<String> mainIds = uberCategories
                    .stream()
                    .map(UberCategory::getId)
                    .filter(id -> !id.equals("DRINKS") && !id.equals("DESSERTS"))
                    .sorted(Comparator.comparing(id -> priorityMap.getOrDefault(id, 50))) // 50 es el orden por defecto
                    .toList();

            UberMenu principalMenu = new UberMenu();
            principalMenu.setId("PRINCIPAL_MENU");
            principalMenu.setTitle(uberMapper.toUberText("Carta Principal"));
            principalMenu.setCategoryIds(mainIds);
            principalMenu.setServiceAvailability(uberMapper.getDefaultAvailability());

            // Menú 2: Bebidas y postres
            List<String> drinksIds = uberCategories.stream()
                    .map(UberCategory::getId)
                    .filter(id -> id.equals("DRINKS") || id.equals("DESSERTS"))
                    .toList();

            UberMenu secondaryMenu = new UberMenu();
            secondaryMenu.setId("SECONDARY_MENU");
            secondaryMenu.setTitle(uberMapper.toUberText("Bebidas y Postres"));
            secondaryMenu.setCategoryIds(drinksIds);
            secondaryMenu.setServiceAvailability(uberMapper.getDefaultAvailability());

            // 8. CONSTRUCCIÓN DEL DTO FINAL: Se requiere un único objeto Uber que contenga a todos.
            UberDTO uber = new UberDTO();
            uber.setMenus(List.of(principalMenu, secondaryMenu));
            uber.setItems(uberItems);
            uber.setCategories(uberCategories);
            uber.setModifierGroups(List.of());

            // 9. ENVÍO: Usamos FeignClient para hacer la peticion a los servidores Uber.
            uberClient.updateProducts(accessToken, storeId, uber);
        } catch (Exception e) {
            throw new RuntimeException("Se ha producido un error en la conexión con Uber. " + e);
        }
    }
}