package com.michi.productos.dto;

import com.michi.productos.model.Allergen;
import com.michi.productos.model.Category;
import com.michi.productos.model.MainCategory;

import java.math.BigDecimal;

public record ProductDTO (
        String id,
        String name,
        String description,
        BigDecimal price,
        MainCategory mainCategory,
        Category category,
        Allergen[] allergens,
        String japaneseName,
        int pieces,
        boolean status,
        Integer position,
        Boolean featured
) {}
