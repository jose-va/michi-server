package com.michi.reservas.push;

import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.service.CreatedReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushListener {

    private final PushNotificationService pushNotificationService;

    private static final String PREVIEW_URL = "/reservation/preview";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "ES"));

    @Async
    @EventListener
    public void onReservationCreated(CreatedReservation event) {
        try {
            ReservationDTO r = event.reservation();
            List<String> parts = new ArrayList<>();
            if (r.guests() != null) {
                parts.add("Mesa para " + r.guests());
            }
            if (r.date() != null) {
                parts.add(r.date().format(DATE_FMT));
            }
            if (r.startTime() != null) {
                parts.add(r.startTime().toString());
            }
            pushNotificationService.sendToAdmins("Nueva reserva", String.join(" · ", parts), PREVIEW_URL);
        } catch (Exception e) {
            log.error("Error preparando push de la reserva", e);
        }
    }
}
