package com.michi.productos.service;

import com.michi.productos.model.Category;
import com.michi.productos.model.MainCategory;
import com.michi.productos.model.Product;

import java.util.Comparator;
import java.util.List;

public final class ProductOrder {

    private ProductOrder() {
    }

    public static final List<MainCategory> MAIN_CATEGORY_ORDER = List.of(
            MainCategory.STARTERS,
            MainCategory.HOT_KITCHEN,
            MainCategory.SUSHI_CLASSIC,
            MainCategory.MAKIS_ROLLS,
            MainCategory.SPECIAL,
            MainCategory.COMBOS,
            MainCategory.DESSERTS
    );

    public static final List<Category> CATEGORY_ORDER = List.of(
            Category.FRESH_STARTERS,
            Category.HOT_STARTERS,
            Category.TARTAR,
            Category.SOGIZUKURI,
            Category.HOT_DISHES,
            Category.ZUKE_DON,
            Category.NIGIRI_SUSHI,
            Category.GUNKAN_SUSHI,
            Category.TEMAKI,
            Category.HOSOMAKI,
            Category.URAMAKI,
            Category.FUTOMAKI,
            Category.KATSUMAKI,
            Category.HOT_MAKI,
            Category.SPECIAL_MAKI,
            Category.COMBOS,
            Category.DESSERTS
    );

    public static int mainCategoryRank(MainCategory mainCategory) {
        if (mainCategory == null) return Integer.MAX_VALUE;
        int index = MAIN_CATEGORY_ORDER.indexOf(mainCategory);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public static int categoryRank(Category category) {
        if (category == null) return Integer.MAX_VALUE;
        int index = CATEGORY_ORDER.indexOf(category);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public static final Comparator<Product> PRODUCT_ORDER = Comparator
            .comparing((Product p) -> !p.isFeatured())
            .thenComparingInt(p -> mainCategoryRank(p.getMainCategory()))
            .thenComparingInt(p -> categoryRank(p.getCategory()))
            .thenComparingInt(Product::getPosition)
            .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    public static final Comparator<Product> UBER_ORDER = Comparator
            .comparingInt((Product p) -> mainCategoryRank(p.getMainCategory()))
            .thenComparingInt(p -> categoryRank(p.getCategory()))
            .thenComparingInt(Product::getPosition)
            .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
}
