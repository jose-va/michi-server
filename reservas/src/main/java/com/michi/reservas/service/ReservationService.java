package com.michi.reservas.service;

import com.michi.reservas.dto.AvailableHoursDTO;
import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.mapper.ReservationMapper;
import com.michi.reservas.model.Location;
import com.michi.reservas.model.Reservation;
import com.michi.reservas.model.State;
import com.michi.reservas.model.Table;
import com.michi.reservas.repository.ReservationRepository;
import com.michi.reservas.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final TableRepository tableRepository;

    private static final List<LocalTime> HOURS_LIST = List.of(
            LocalTime.of(13, 0),
            LocalTime.of(13, 30),
            LocalTime.of(14, 0),
            LocalTime.of(14, 30),
            LocalTime.of(15, 0),
            LocalTime.of(20, 0),
            LocalTime.of(20, 30),
            LocalTime.of(21, 0),
            LocalTime.of(21, 30),
            LocalTime.of(22, 0)
    );

    public List<ReservationDTO> findAll() {
        return reservationRepository.findAll()
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    public AvailableHoursDTO findAvailableHours(LocalDate date, int guests) {
        List<Reservation> allReservations = reservationRepository.findByDate(date);

        List<Table> allTables = tableRepository.findAll();

        List<LocalTime> insideHours = HOURS_LIST.stream()
                .filter(hour -> isAvailable(hour, allReservations, allTables, guests, Location.INSIDE))
                .toList();

        List<LocalTime> outsideHours = HOURS_LIST.stream()
                .filter(hour -> isAvailable(hour, allReservations, allTables, guests, Location.OUTSIDE))
                .toList();

        return new AvailableHoursDTO(insideHours, outsideHours);
    }

    private boolean isAvailable(LocalTime startTime,
                                List<Reservation> allReservations,
                                List<Table> allTables,
                                int guests, Location location) {

        LocalTime endTime = startTime.plusMinutes(90);

        List<String> unavailableTables = allReservations.stream()
                .filter(r -> startTime.isBefore(r.getEndTime()) && r.getStartTime().isBefore(endTime))
                .flatMap(r -> r.getTableIds().stream())
                .toList();

        List<Table> availableTables = allTables.stream()
                .filter(t -> !unavailableTables.contains(t.getId()))
                .filter(t -> t.getLocation() == location)
                .toList();

        return hasCapacity(availableTables, guests, location);
    }

    private boolean hasCapacity(List<Table> availableTables, int guests, Location location) {
        boolean singleTable = availableTables.stream()
                .anyMatch(t -> t.getCapacity() >= guests);
        if (singleTable) return true;

        if (location == Location.INSIDE && guests <= 10) {
            long joinable = availableTables.stream()
                    .filter(t -> t.getId().equals("INS-1") || t.getId().equals("INS-2"))
                    .count();
            return joinable == 2;
        }

        if (location == Location.OUTSIDE && guests <= 12) {
            int tablesNeeded = switch (guests) {
                case 5, 6 -> 2;
                case 7, 8 -> 3;
                case 9, 10 -> 4;
                case 11, 12 -> 5;
                default -> 1;
            };
            return availableTables.size() >= tablesNeeded;
        }
        return false;
    }

    public ReservationDTO create(ReservationDTO dto) {
        Reservation reservation = reservationMapper.toEntity(dto);
        reservation.setTableIds(assignTables(reservation));
        reservation.setState(State.CONFIRMED);
        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    private List<String> assignTables(Reservation reservation) {
        LocalTime startTime = reservation.getStartTime();
        LocalTime endTime = startTime.plusMinutes(90);

        List<Reservation> allReservations = reservationRepository.findByDate(
                reservation.getDate()
        );
        List<Table> allTables = tableRepository.findAll();

        List<String> unavailableIds = allReservations.stream()
                .filter(r -> startTime.isBefore(r.getEndTime()) && r.getStartTime().isBefore(endTime))
                .flatMap(r -> r.getTableIds().stream())
                .toList();

        List<Table> availableTables = allTables.stream()
                .filter(t -> !unavailableIds.contains(t.getId()))
                .filter(t -> t.getLocation() == reservation.getLocation())
                .toList();

        return selectTables(availableTables, reservation.getGuests(), reservation.getLocation());
    }

    private List<String> selectTables(List<Table> availableTables, int guests, Location location) {
        Optional<Table> single = availableTables.stream()
                .filter(t -> t.getCapacity() >= guests)
                .findFirst();
        if (single.isPresent()) return List.of(single.get().getId());

        if (location == Location.INSIDE && guests <= 10) {
            List<String> joinable = availableTables.stream()
                    .filter(t -> t.getId().equals("INS-1") || t.getId().equals("INS-2"))
                    .map(Table::getId)
                    .toList();
            if (joinable.size() == 2) return joinable;
        }

        if (location == Location.OUTSIDE && guests <= 12) {
            int tablesNeeded = switch (guests) {
                case 5, 6 -> 2;
                case 7, 8 -> 3;
                case 9, 10 -> 4;
                case 11, 12 -> 5;
                default -> 1;
            };
            List<String> ids = availableTables.stream()
                    .map(Table::getId)
                    .limit(tablesNeeded)
                    .toList();
            if (ids.size() >= tablesNeeded) return ids;
        }

        throw new RuntimeException("No hay mesas disponibles para la reserva solicitada");
    }

    public ReservationDTO update(ReservationDTO dto, String id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado ninguna reserva con ID " + id));
        reservationMapper.updateEntityFromDto(dto, reservation);
        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    public void delete(String id) {
        if (!reservationRepository.existsById(id))
            throw new RuntimeException("No se ha encontrado ninguna reserva con ID " + id);
        reservationRepository.deleteById(id);
    }
}
