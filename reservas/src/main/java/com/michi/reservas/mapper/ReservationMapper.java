package com.michi.reservas.mapper;

import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    Reservation toEntity(ReservationDTO reservationDTO);
    ReservationDTO toDto (Reservation reservation);

    @Mapping(target = "id" , ignore = true)
    void updateEntityFromDto (ReservationDTO reservationDTO, @MappingTarget Reservation reservation);
}
