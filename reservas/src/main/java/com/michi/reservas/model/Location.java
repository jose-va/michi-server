package com.michi.reservas.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Location {
    INSIDE("Interior"),
    OUTSIDE("Terraza");

    private final String name;
}