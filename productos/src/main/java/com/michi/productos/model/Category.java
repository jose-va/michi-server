package com.michi.productos.model;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
public enum Category {
    NIGIRI_SUSHI("Nigiri sushi"),
    SPECIAL_MAKI("Maki especial"),
    HOSOMAKI("Hosomaki"),
    KATSUMAKI("Katsumaki"),
    URAMAKI("Uramaki"),
    COMBOS("Combinados"),
    HOT_DISHES("Platos calientes"),
    FUTOMAKI("Futomaki"),
    TARTAR("Tartar"),
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
