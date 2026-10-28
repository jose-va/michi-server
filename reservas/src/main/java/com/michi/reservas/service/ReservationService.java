package com.michi.reservas.service;

import com.michi.reservas.dto.AvailableHoursDTO;
import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.mapper.ReservationMapper;
import com.michi.reservas.model.*;
import com.michi.reservas.repository.ReservationRepository;
import com.michi.reservas.repository.TableRepository;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("Europe/Madrid");
    private static final long CANCELLATION_HOURS = 2;
    private static final LocalTime MONDAY_OPENING = LocalTime.of(20, 0);

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

    public List<ReservationDTO> findAll(LocalDate date) {
        return reservationRepository.findByDate(date)
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    public List<ReservationDTO> findByUser(String userId, LocalDate date) {
        List<Reservation> reservations = date == null
                ? reservationRepository.findByUser(userId)
                : reservationRepository.findByUserAndDate(userId, date);
        return reservations.stream()
                .map(r -> reservationMapper.toDto(r).withCancelToken(
                        r.getState() == State.CONFIRMED ? r.getCancelToken() : null))
                .toList();
    }

    private boolean isBookable(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).isAfter(LocalDateTime.now(RESTAURANT_ZONE));
    }

    /**
     * Horas permitidas según el día de la semana.
     * Los lunes solo se reserva de 20:00 a 22:00.
     */
    private List<LocalTime> hoursFor(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
            return HOURS_LIST.stream()
                    .filter(h -> !h.isBefore(MONDAY_OPENING))
                    .toList();
        }
        return HOURS_LIST;
    }

    public AvailableHoursDTO findAvailableHours(LocalDate date, int guests) {
        List<LocalTime> candidateHours = hoursFor(date).stream()
                .filter(hour -> isBookable(date, hour))
                .toList();

        if (candidateHours.isEmpty()) {
            return new AvailableHoursDTO(List.of(), List.of());
        }

        List<Reservation> allReservations =
                reservationRepository.findByDateAndStateNot(date, State.CANCELLED);
        List<Table> allTables = tableRepository.findAll();

        List<LocalTime> insideHours = candidateHours.stream()
                .filter(hour -> isAvailable(hour, allReservations, allTables, guests, Location.INSIDE))
                .toList();

        List<LocalTime> outsideHours = candidateHours.stream()
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

    private Shift shiftOf(LocalTime time) {
        return time.isBefore(LocalTime.of(18, 0)) ? Shift.LUNCH : Shift.DINNER;
    }

    public CreatedReservation create(ReservationDTO dto) {
        Reservation reservation = reservationMapper.toEntity(dto);

        if (reservation.getDate() == null
                || reservation.getStartTime() == null
                || reservation.getGuests() < 1
                || !hoursFor(reservation.getDate()).contains(reservation.getStartTime())
                || !isBookable(reservation.getDate(), reservation.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TIME");
        }

        reservation.setShift(shiftOf(reservation.getStartTime()));

        String user = reservation.getUser();
        if (user != null && !user.isBlank()
                && reservationRepository.existsByUserAndDateAndShiftAndState(
                user, reservation.getDate(), reservation.getShift(), State.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESERVATION_ALREADY_EXISTS");
        }

        reservation.setTableIds(assignTables(reservation));
        reservation.setState(State.CONFIRMED);
        reservation.setCancelToken(UUID.randomUUID().toString());

        Reservation saved;
        try {
            saved = reservationRepository.save(reservation);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESERVATION_ALREADY_EXISTS");
        }
        return new CreatedReservation(reservationMapper.toDto(saved), saved.getId(), saved.getCancelToken());
    }

    private List<String> assignTables(Reservation reservation) {
        LocalTime startTime = reservation.getStartTime();
        LocalTime endTime = startTime.plusMinutes(90);

        List<Reservation> allReservations = reservationRepository.findByDateAndStateNot(
                reservation.getDate(), State.CANCELLED
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

        throw new ResponseStatusException(HttpStatus.CONFLICT, "NO_TABLES_AVAILABLE");
    }

    public void cancel(String id, String token, boolean admin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if (!admin) {
            if (token == null || reservation.getCancelToken() == null
                    || !MessageDigest.isEqual(
                            token.getBytes(StandardCharsets.UTF_8),
                            reservation.getCancelToken().getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token de cancelación no válido");
            }
            if (reservation.getState() == State.CANCELLED) return;
            LocalDateTime limit = LocalDateTime.of(reservation.getDate(), reservation.getStartTime())
                    .minusHours(CANCELLATION_HOURS);
            if (LocalDateTime.now(RESTAURANT_ZONE).isAfter(limit)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya no es posible cancelar online. Llámenos al +34 673 82 11 48");
            }
        }

        reservation.setState(State.CANCELLED);
        reservationRepository.save(reservation);
    }
}
