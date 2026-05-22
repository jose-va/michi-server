package com.michi.productos.model;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
public enum Category {
    HOT_STARTERS("Entrantes calientes"),
    FRESH_STARTERS("Entrantes fríos"),
    HOT_DISHES("Platos calientes"),
    SOGIZUKURI("Sogizukuri"),
    NIGIRI_SUSHI("Nigiri sushi"),
    GUNKAN_SUSHI("Gunkan sushi"),
    ZUKE_DON("Zuke don"),
    TEMAKI("Temaki"),
    HOSOMAKI("Hosomaki"),
    KATSUMAKI("Katsumaki"),
    URAMAKI("Uramaki"),
    FUTOMAKI("Futomaki"),
    HOT_MAKI("Maki caliente"),
    SPECIAL_MAKI("Maki especial"),
    COMBOS("Combinados"),
    DESSERTS("Postres");

    private final String title;
}
