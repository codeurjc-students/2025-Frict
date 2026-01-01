package com.tfg.backend.utils;

import com.tfg.backend.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@Slf4j //Custom logs enablement
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    // @Async allows that this function can be executed concurrently
    @Async
    public void sendOrderConfirmation(String to, String userName, String orderRef, List<OrderItem> items, Double total) {
        try {
            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderRef", orderRef);
            context.setVariable("items", items);
            context.setVariable("totalAmount", total);

            // Process HTML
            String htmlContent = templateEngine.process("email-order-confirmation", context);

            // Create message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Confirmación de Pedido - " + orderRef);
            helper.setText(htmlContent, true); //It is true that is HTML

            // Send
            mailSender.send(message);
            log.info("Email enviado correctamente a {}", to);

        } catch (MessagingException e) {
            // Do not raise exceptions in order not to interrupt the main thread
            log.error("Error al enviar email: {}", e.getMessage());
        }
    }


    @Async
    public void sendRecoveryOtp(String to, String username, String otpCode) {
        try {
            // Build redirection URL
            String targetUrl = "https://localhost:4202/reset?username=" + username;

            // Set Thymeleaf variables
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("otpCode", otpCode);
            context.setVariable("resetLink", targetUrl);

            // Process HTML
            String htmlContent = templateEngine.process("otp-code-sending", context);

            // Configure message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Código de recuperación: " + otpCode + " - MiTienda");
            helper.setText(htmlContent, true);

            // Send
            mailSender.send(message);
            log.info("OTP de recuperación enviado a {}", to);

        } catch (MessagingException e) {
            log.error("Error al enviar OTP: {}", e.getMessage()); //No exception to avoid interrupting the main thread
        }
    }
}
