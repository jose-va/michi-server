package com.michi.reservas.repository;

import com.michi.reservas.model.*;
import com.michi.reservas.model.State;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByDate(LocalDate date);
    List<Reservation> findByDateAndStateNot(LocalDate date, State state);
    List<Reservation> findByUser(String user);
    List<Reservation> findByUserAndDate(String user, LocalDate date);

    boolean existsByUserAndDateAndShiftAndState(String user, LocalDate date, Shift shift, State state);
}
