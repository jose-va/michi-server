package com.michi.reservas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.lang.Nullable;
import com.michi.reservas.model.Location;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationDTO(
        LocalDate date,
        Integer guests,
        Location location,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        String name,
        String phone,
        @Nullable
        String email,
        @Nullable
        String observations,
        @Nullable
        String user
) {}
