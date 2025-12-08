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

    // IMPORTANTE: @Async hace que esto se ejecute en un hilo separado
    // Necesitas activar @EnableAsync en tu clase main Application.java
    @Async
    public void sendOrderConfirmation(String to, String userName, String orderRef, List<OrderItem> items, Double total) {
        try {
            // 1. Preparar el contexto de Thymeleaf (las variables)
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderRef", orderRef);
            context.setVariable("items", items);
            context.setVariable("totalAmount", total);

            // 2. Procesar el HTML
            String htmlContent = templateEngine.process("email-order-confirmation", context);

            // 3. Crear el mensaje
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Confirmación de Pedido - " + orderRef);
            helper.setText(htmlContent, true); // true = es HTML

            // 4. Enviar
            mailSender.send(message);
            System.out.println("Email enviado correctamente a " + to);

        } catch (MessagingException e) {
            // Logueamos el error, pero NO lanzamos excepción para no romper la transacción del pedido
            System.err.println("Error al enviar email: " + e.getMessage());
        }
    }
}
