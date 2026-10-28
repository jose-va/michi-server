package com.michi.productos.model;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
public enum Category {
    // El orden de declaración define el orden de la carta
    NIGIRI_SUSHI("Nigiri sushi"),
    SPECIAL_MAKI("Maki especial"),
    HOSOMAKI("Hosomaki"),
    KATSUMAKI("Katsumaki"),
    URAMAKI("Uramaki"),
    COMBOS("Combinados"),
    HOT_DISHES("Platos calientes"),
    FUTOMAKI("Futomaki"),
    // Sin imágenes por ahora, en el orden original de la carta
    HOT_STARTERS("Entrantes calientes"),
    FRESH_STARTERS("Entrantes fríos"),
    SOGIZUKURI("Sogizukuri"),
    GUNKAN_SUSHI("Gunkan sushi"),
    ZUKE_DON("Zuke don"),
    TEMAKI("Temaki"),
    HOT_MAKI("Maki caliente"),
    DESSERTS("Postres");

    private final String title;
}
