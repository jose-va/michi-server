package com.michi.productos.uber.mapper;

import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import com.michi.productos.uber.dto.UberDTO.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class UberMapper {

    /**
     * Convierte un String al objeto MultiLanguageText que Uber necesita.
     * Uber no acepta "Sushi", acepta un objeto con su respectiva traduccion.
     */
    public MultiLanguageText toUberText(String text) {
        if (text == null) return null;

        MultiLanguageText multiText = new MultiLanguageText();
        Map<String, String> translations = new HashMap<>();
        translations.put("es_es", text); // Forzamos el locale a español
        multiText.setTranslations(translations);
        return multiText;
    }

    /**
     * Transforma un Producto de nuestra base de datos en un UberItem.
     */
    public UberItem toUberItem(Product product) {
        UberItem item = new UberItem();
        item.setId(product.getId());

        String titleWithPieces = product.getName() +
                (product.getPieces() > 0 ? " (" + product.getPieces() + " uds)" : "");
        item.setTitle(toUberText(titleWithPieces));

        item.setDescription(toUberText(product.getDescription()));

        // Si el producto tiene alérgenos, los convertimos a String (Enum::name).
        if (product.getAllergens() != null && !product.getAllergens().isEmpty()) {
            NutritionalInfo nutri = new NutritionalInfo();

            // Convertimos la lista de Enums de Java a una lista de Strings (nombres)
            List<String> allergenNames = product.getAllergens()
                    .stream()
                    .map(Enum::name)
                    .toList();

            nutri.setAllergens(allergenNames);
            item.setNutritionalInfo(nutri);
        }

        // Si el precio es 10.50, debemos enviar 1050 a uber
        PriceRules priceRules = new PriceRules();
        priceRules.setPrice((int) (product.getPrice() * 100));
        item.setPriceInfo(priceRules);

        return item;
    }

    /**
     * Crea la lista de 'Entities'. Uber organiza las categorías mediante distintas entidades
     * Aquí le decimos a Uber que esta categoría contiene este listado de entidades.
     */
    public List<MenuEntity> toUberEntities(List<Product> products) {
        return products.stream()
                .map(product -> {
                    MenuEntity entity = new MenuEntity();
                    entity.setId(product.getId());
                    entity.setType("ITEM"); // Siempre ITEM para productos individuales
                    return entity;
                })
                .toList();
    }

    /**
     * Define los horarios de disponibilidad de nuestro menú.
     * Uber requiere saber cuándo puede el cliente ver y pedir en el menú.
     */
    public List<ServiceAvailability> getDefaultAvailability() {
        // Turno de día (12:00 - 16:00)
        TimePeriod dayTime = new TimePeriod();
        dayTime.setStartTime("12:00");
        dayTime.setEndTime("16:00");

        // Turno de noche (20:00 - 23:00)
        TimePeriod nightTime = new TimePeriod();
        nightTime.setStartTime("20:00");
        nightTime.setEndTime("23:00");

        List<TimePeriod> dailyturn = List.of(dayTime, nightTime);

        // Días de apertura: lunes a sábado.
        List<String> diasApertura = List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday");

        return new java.util.ArrayList<>(
                diasApertura.stream()
                .map(day -> {
                    ServiceAvailability sa = new ServiceAvailability();
                    sa.setDayOfWeek(day);
                    sa.setTimePeriods(dailyturn); // Asignamos ambos turnos a cada unos de los días
                    return sa;
                })
                .toList());
    }

    public UberCategory toUberCategory(Category c, List<Product> products) {
        UberCategory category = new UberCategory();
        // El ID es el nombre del Enum y el título es el String que se muestra al cliente
        category.setId(c.name());
        category.setTitle(toUberText(c.getTitle()));
        category.setEntities(toUberEntities(products)); // Enlazamos los productos
        return category;
    }
}
