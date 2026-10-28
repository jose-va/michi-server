package com.michi.reservas.mapper;

import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.model.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    Reservation toEntity(ReservationDTO reservationDTO);
    ReservationDTO toDto (Reservation reservation);
}
