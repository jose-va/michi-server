package com.michi.reservas.controller;

import com.michi.reservas.dto.AvailableHoursDTO;
import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.mail.EmailService;
import com.michi.reservas.service.CreatedReservation;
import com.michi.reservas.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final EmailService emailService;

    @GetMapping
    public List<ReservationDTO> findAll(
            @RequestParam LocalDate date,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return reservationService.findAll(date);
    }

    @GetMapping("/preview")
    public List<ReservationDTO> preview(
            @RequestParam(required = false) LocalDate date,
            @RequestHeader(value = "User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return reservationService.findByUser(userId, date);
    }

    @GetMapping("/hours")
    public AvailableHoursDTO hours(
            @RequestParam LocalDate date,
            @RequestParam int guests,
            @RequestParam(required = false) String excludeReservationId) {
        return reservationService.findAvailableHours(date, guests, excludeReservationId);
    }

    @GetMapping("/{id}")
    public ReservationDTO findById(
            @PathVariable String id,
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader(value = "User-Role", required = false) String role) {
        return reservationService.findById(id, userId, "ROLE_ADMIN".equals(role));
    }

    @PostMapping
    public ReservationDTO create(
            @RequestBody ReservationDTO body,
            @RequestHeader(value = "User-Id", required = false) String userId){
        String owner = (userId == null || userId.isBlank()) ? null : userId;
        ReservationDTO dto = new ReservationDTO(body.id(),
                body.date(), body.guests(), body.location(), body.startTime(),
                body.name(), body.phone(), body.email(), body.observations(), owner, null, null);
        CreatedReservation created = reservationService.create(dto);
        try {
            emailService.sendEmail(created.reservation(), created.id(), created.cancelToken());
        } catch (Exception e) {
            log.error("Error al enviar el correo", e);
        }
        return created.reservation();
    }

    @PutMapping("/{id}")
    public ReservationDTO update(
            @PathVariable String id,
            @RequestBody ReservationDTO body,
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader(value = "User-Role", required = false) String role) {
        return reservationService.update(id, body, userId, "ROLE_ADMIN".equals(role));
    }

    @DeleteMapping("/{id}")
    public void cancel(
            @PathVariable String id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "User-Role", required = false) String role) {
        reservationService.cancel(id, token, "ROLE_ADMIN".equals(role));
    }

    private void checkAdmin(String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso prohíbido");
        }
    }
}
