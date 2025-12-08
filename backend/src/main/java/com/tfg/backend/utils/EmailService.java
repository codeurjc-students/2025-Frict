package com.tfg.backend.utils;

import com.tfg.backend.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
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
            System.out.println("Email enviado correctamente a " + to);

        } catch (MessagingException e) {
            // Do not raise exceptions in order not to interrupt the main thread
            System.err.println("Error al enviar email: " + e.getMessage());
        }
    }
}
