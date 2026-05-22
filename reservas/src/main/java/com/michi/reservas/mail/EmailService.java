package com.michi.reservas.mail;

import com.michi.reservas.dto.ReservationDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendEmail(ReservationDTO dto) {
        Context context = new Context();
        context.setVariable("name", dto.name());
        context.setVariable("date", dto.date());
        context.setVariable("time", dto.startTime());
        context.setVariable("guests", dto.guests());

        String html = templateEngine.process("reservation-email", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(dto.email());
            helper.setSubject("Confirmación de reserva en Michi Sushi");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo electrónico. ", e);
        }
    }
}
