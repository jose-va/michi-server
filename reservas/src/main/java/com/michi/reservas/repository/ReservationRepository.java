package com.michi.reservas.repository;

import com.michi.reservas.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByDate(LocalDate date);
}
