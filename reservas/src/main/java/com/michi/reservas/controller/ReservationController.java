package com.michi.reservas.controller;

import com.michi.reservas.dto.AvailableHoursDTO;
import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.mail.EmailService;
import com.michi.reservas.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/hours")
    public AvailableHoursDTO hours(
            @RequestParam LocalDate date,
            @RequestParam int guests) {
        return reservationService.findAvailableHours(date, guests);
    }

    @PostMapping
    public ReservationDTO create(@RequestBody ReservationDTO dto){
//        try {
//            emailService.sendEmail(dto);
//        } catch (Exception e) {
//            System.err.println("Error al enviar el correo: " + e.getMessage());
//        }
        return reservationService.create(dto);
    }

    @PutMapping("/{id}")
    public ReservationDTO update(
            @RequestBody ReservationDTO dto,
            @PathVariable String id,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return reservationService.update(dto, id);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable String id,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        reservationService.delete(id);
    }

    private void checkAdmin(String role) {
        if (!role.equals("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso prohíbido");
        }
    }
}
