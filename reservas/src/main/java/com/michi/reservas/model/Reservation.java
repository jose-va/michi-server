package com.michi.reservas.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.*;
import java.util.List;

@Data
@Document(value = "reservations")
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

    public LocalTime getEndTime() {
        return startTime.plusMinutes(90);
    }
}

