package com.michi.reservas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;

public record AvailableHoursDTO(
        @JsonFormat(pattern = "HH:mm")
        List<LocalTime> insideHours,

        @JsonFormat(pattern = "HH:mm")
        List<LocalTime> outsideHours
) {}
