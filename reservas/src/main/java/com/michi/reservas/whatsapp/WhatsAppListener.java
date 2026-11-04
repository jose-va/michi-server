package com.michi.reservas.whatsapp;

import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.service.CreatedReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppListener {

    private final WhatsAppService whatsAppService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "ES"));

    @Async
    @EventListener
    public void onReservationCreated(CreatedReservation event) {
        try {
            ReservationDTO r = event.reservation();

            if (r.phone() == null || r.phone().isBlank()) {
                log.warn("Reserva {} sin teléfono, no se envía WhatsApp", r.id());
                return;
            }

            String digits = r.phone().replaceAll("\\D", "");
            if (digits.startsWith("00")) digits = digits.substring(2);
            if (digits.length() == 9) digits = "34" + digits;

            log.info("Enviando WhatsApp a {} (reserva {})", digits, r.id());

            whatsAppService.sendConfirmation(
                    digits,
                    r.name(),
                    r.date().format(FMT),
                    r.startTime().toString());
        } catch (Exception e) {
            log.error("Error preparando WhatsApp de la reserva", e);
        }
    }
}
