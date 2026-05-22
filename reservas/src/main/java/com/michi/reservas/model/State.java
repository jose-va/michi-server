package com.michi.reservas.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum State {
    CONFIRMED("Confirmada"),
    CANCELLED("Cancelada");

    private final String name;
}
