package com.michi.reservas.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.*;
import java.util.List;

@Document("reservations")
@CompoundIndex(
        name = "uq_active_user_date_shift",
        def = "{'user': 1, 'date': 1, 'shift': 1}",
        unique = true,
        partialFilter = "{ 'state': 'CONFIRMED', 'user': { $gt: '' } }"
)
@Data
public class Reservation {
    @Id
    private String id;

    private LocalDate date;
    private Integer guests;
    private Location location;
    private LocalTime startTime;
    private String name;
    private String email;
    private String phone;
    private String observations;
    private List<String> tableIds;
    private State state;
    private String user;
    private String cancelToken;

    private Shift shift;

    public LocalTime getEndTime() {
        return startTime.plusMinutes(90);
    }
}

