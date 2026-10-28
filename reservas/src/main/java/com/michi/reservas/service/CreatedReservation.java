package com.michi.reservas.service;

import com.michi.reservas.dto.ReservationDTO;

public record CreatedReservation(ReservationDTO reservation, String id, String cancelToken) {}
