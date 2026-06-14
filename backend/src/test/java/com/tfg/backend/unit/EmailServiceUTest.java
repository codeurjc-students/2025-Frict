package com.tfg.backend.unit;

import com.tfg.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceUTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;

    private final String TARGET_EMAIL = "user@example.com";
    private final String USERNAME = "testuser";
    private final String OTP_CODE = "123456";

    private final String FRONTEND_URL = "https://localhost:4202";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "");
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendRecoveryOtp properly binds variables to Thymeleaf and sends the email")
    void sendRecoveryOtp_Success() {
        // Arrange
        String expectedHtml = "<html>Dummy HTML</html>";
        when(templateEngine.process(eq("otp-code-sending"), any(Context.class))).thenReturn(expectedHtml);

        // Act
        emailService.sendRecoveryOtp(TARGET_EMAIL, USERNAME, OTP_CODE, "USER");

        // Assert 1: Verify Thymeleaf context was built correctly
        verify(templateEngine).process(eq("otp-code-sending"), contextCaptor.capture());
        Context capturedContext = contextCaptor.getValue();

        assertEquals(USERNAME, capturedContext.getVariable("username"));
        assertEquals(OTP_CODE, capturedContext.getVariable("otpCode"));
        assertEquals(FRONTEND_URL + "/reset?username=" + USERNAME, capturedContext.getVariable("resetLink"));

        // Assert 2: Verify mail sender was called
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendRecoveryOtp swallows MessagingException and does not interrupt execution")
    void sendRecoveryOtp_SwallowsMessagingException_WhenMailFails() {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>...</html>");

        // Simulating the email server being down
        doThrow(new MailException("SMTP Server down") {})
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        // assertDoesNotThrow guarantees that the exception was caught and swallowed internally
        assertDoesNotThrow(() -> emailService.sendRecoveryOtp(TARGET_EMAIL, USERNAME, OTP_CODE, "USER"));

        // Verify it actually tried to send it
        verify(mailSender).send(mimeMessage);
    }
}