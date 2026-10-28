package com.michi.productos.uber.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UberDTO {

    private List<UberMenu> menus; // Menús
    private List<UberCategory> categories; // Categorías
    private List<UberItem> items; // Productos
    @JsonProperty("modifier_groups")
    private List<Object> modifierGroups; // Modificadores

    /**
     * Uber no admite cadenas simples para sus títulos y descripciones (String). Necesita un objeto MultiLanguageText
     * Este objeto está formado por un mapa; siendo la key la traducción y el value la cadena de texto.
     */
    @Data
    public static class MultiLanguageText {
        private Map<String, String> translations; // Traducciones
    }

    /**
     * Objeto que requiere Uber para definir el horario de apertura de nuestro restaurante.
     * Está formado por una lista de String con los días de la semana y contiene los horarios.
     */
    @Data
    public static class ServiceAvailability {
        @JsonProperty("day_of_week")
        private String dayOfWeek; // Día de la semana
        @JsonProperty("time_periods")
        private List<TimePeriod> timePeriods; // Horario
    }

    /**
     * Uber diferencia distintos "menús" dentro de la aplicación.
     * Estos menús contienen las distintas categorías de nuestra carta y el horario de apertura.
     */
    @Data
    public static class UberMenu {
        private String id; // Identificador
        private MultiLanguageText title; // Título
        @JsonProperty("category_ids")
        private List<String> categoryIds; // Identificador de la categoría
        @JsonProperty("service_availability")
        private List<ServiceAvailability> serviceAvailability; // Horario de apertura
    }

    /**
     * Periodo de tiempo para los horarios
     * Contiene el horario de apertura y el de cierre
     */
    @Data
    public static class TimePeriod {
        @JsonProperty("start_time")
        private String startTime; // Hora de apertura
        @JsonProperty("end_time")
        private String endTime; // Hora de cierre
    }

    /**
     * Entidades que conforman las categorías de nuestro menú
     * Para la Uber API v2, las categorías no contienen los productos (items), contiene entidades
     */
    @Data
    public static class MenuEntity {
        private String id; // Identificador
        private String type = "ITEM"; // Tipo de entidad
    }
    /**
     * Objeto que define las disintas categoría que conforman nuestra carta
     */
    @Data
    public static class UberCategory {
        private String id; // Identificador
        private MultiLanguageText title; // Título
        private List<MenuEntity> entities; // Listado de Productos
    }

    /**
     * Precio de los productos, Uber exige que sea de tipo Integer incluso si tiene decimales
     */
    @Data
    public static class PriceRules {
        @JsonProperty("price")
        private Integer price; // Precio
    }
    /**
     * Listado de alérgenos de nuestros productos, en Uber se llama "nutritional info"
     */
    @Data
    public static class NutritionalInfo {
        @JsonProperty("allergens")
        private List<String> allergens; // Alérgenos
    }
    /**
     * En Uber, los productos que conforman la carta son "items"
     */
    @Data
    public static class UberItem {
        private String id; // Identificador
        private MultiLanguageText title; // Título
        private MultiLanguageText description; // Descripción
        @JsonProperty("price_info")
        private PriceRules priceInfo; // Precio
        @JsonProperty("nutritional_info")
        private NutritionalInfo nutritionalInfo; //Información nutricional
    }
}