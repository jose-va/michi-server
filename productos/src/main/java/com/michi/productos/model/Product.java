package com.michi.productos.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.List;

@Data
@Document (value = "products")
public class Product {
    @Id
    private String id;

    private String name;
    private String japaneseName;
    private String description;
    private Category category;
    private List<Allergen> allergens;
    private int pieces;
    private double price;
    private boolean status;
    private int position;
}
