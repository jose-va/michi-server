package com.michi.reservas.mail;

import com.michi.reservas.dto.ReservationDTO;
import com.michi.reservas.model.Location;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", ES);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendEmail(ReservationDTO dto, String reservationId, String cancelToken) {
        if (dto.email() == null || dto.email().isBlank()) return;

        String cancelUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/cancelar")
                .queryParam("id", reservationId)
                .queryParam("token", cancelToken)
                .build().toUriString();

        String editUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reservation/edit")
                .queryParam("id", reservationId)
                .build().toUriString();

        String longDate = capitalize(dto.date().format(LONG_DATE));
        String time = dto.startTime().format(TIME);
        String dateTimeText = longDate + ", " + time;
        String location = dto.location() == Location.OUTSIDE ? "Terraza" : "Interior";

        Context context = new Context(ES);
        context.setVariable("guestName", dto.name());
        context.setVariable("dateTimeText", dateTimeText);
        context.setVariable("guestCount", dto.guests());
        context.setVariable("location", location);
        context.setVariable("phone", dto.phone());
        context.setVariable("observations", dto.observations());
        context.setVariable("cancelUrl", cancelUrl);
        context.setVariable("editUrl", editUrl);
        context.setVariable("previewText", "Su reserva para el " + dateTimeText + " está confirmada.");

        String html = templateEngine.process("reservation-email", context);

        StringBuilder plain = new StringBuilder()
                .append("Su reserva ha sido confirmada\n\n")
                .append("Nombre: ").append(dto.name()).append('\n')
                .append("Fecha y hora: ").append(dateTimeText).append('\n')
                .append("Número de invitados: ").append(dto.guests()).append('\n')
                .append("Ubicación: ").append(location).append('\n');
        if (dto.phone() != null && !dto.phone().isBlank())
            plain.append("Teléfono: ").append(dto.phone()).append('\n');
        if (dto.observations() != null && !dto.observations().isBlank())
            plain.append("Observaciones: ").append(dto.observations()).append('\n');
        plain.append("\nLa mesa está reservada durante 90 minutos. ")
                .append("Para modificar su reserva contacte con nosotros al +34 673 82 11 48.\n")
                .append("Puede cancelarla hasta 2 horas antes en: ").append(cancelUrl).append('\n')
                .append("Michi Café & Sushi Bar");

        String subject = "Reserva confirmada · " + dto.date().format(SHORT_DATE) + ", " + time
                + " · Michi Sushi Bar";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(dto.email());
            helper.setSubject(subject);
            helper.setText(plain.toString(), html);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo electrónico. ", e);
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
