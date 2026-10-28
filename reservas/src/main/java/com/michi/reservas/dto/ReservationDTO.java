package com.michi.reservas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.Nullable;
import com.michi.reservas.model.Location;
import com.michi.reservas.model.State;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationDTO(
        String id,
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
        String user,
        @Nullable
        State state,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String cancelToken
) {
        public ReservationDTO withCancelToken(String token) {
                return new ReservationDTO(id, date, guests, location, startTime,
                        name, phone, email, observations, user, state, token);
        }
}
