package com.tfg.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tfg.backend.model.Order;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j //Custom logs enablement
@RequiredArgsConstructor
public class EmailService {

    private static final ClassPathResource LOGO = new ClassPathResource("static/img/frictLogo.png");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.frontend.url:https://localhost:4202}")
    private String frontendUrl;

    // @Async allows that this function can be executed concurrently
    @Async
    public void sendOrderConfirmation(String to, Order order) {
        try {
            byte[] qrBytes = generateQrPng(order.getQrDeliveryToken());

            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("appName", "TecHub");
            context.setVariable("hasQr", qrBytes != null);

            String htmlContent = templateEngine.process("email-order-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (!fromAddress.isBlank()) helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Confirmación de Pedido - " + order.getReferenceCode());
            helper.setText(htmlContent, true);

            helper.addInline("logo", LOGO, "image/png");
            if (qrBytes != null) helper.addInline("qr", new ByteArrayResource(qrBytes), "image/png");

            mailSender.send(message);
            log.info("Email enviado correctamente a {}", to);

        } catch (MessagingException e) {
            // Do not raise exceptions in order not to interrupt the main thread
            log.error("Error al enviar email: {}", e.getMessage());
        }
    }

    @Async
    public void sendRecoveryOtp(String to, String username, String otpCode, String userRole) {
        try {
            String appName = "USER".equals(userRole) ? "TecHub" : "Frict";
            String targetUrl = frontendUrl + "/reset?username=" + username;

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("otpCode", otpCode);
            context.setVariable("resetLink", targetUrl);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("otp-code-sending", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (!fromAddress.isBlank()) helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Código de recuperación: " + otpCode + " - " + appName);
            helper.setText(htmlContent, true);

            helper.addInline("logo", LOGO, "image/png");

            try {
                mailSender.send(message);
            } catch (MailException e) {
                log.error("Error al enviar el correo: {}", e.getMessage());
            }
            log.info("OTP de recuperación enviado a {}", to);

        } catch (MessagingException e) {
            log.error("Error al enviar OTP: {}", e.getMessage()); //No exception to avoid interrupting the main thread
        }
    }

    private byte[] generateQrPng(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            var matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 120, 120);
            var image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 120; x++) {
                for (int y = 0; y < 120; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            var baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("No se pudo generar el QR para el email de pedido: {}", e.getMessage());
            return null;
        }
    }
}