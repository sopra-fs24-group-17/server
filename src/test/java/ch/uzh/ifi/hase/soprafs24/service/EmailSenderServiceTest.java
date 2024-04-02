package ch.uzh.ifi.hase.soprafs24.service;

import static org.mockito.ArgumentCaptor.forClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@ExtendWith(MockitoExtension.class) // Initialize mocks with Mockito
public class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender; // Mock JavaMailSender

    @InjectMocks
    private EmailSenderService emailSenderService;

    @Test
    public void testEmailContent() {
        String toEmail = "user@example.com";
        String username = "testUser";
        String oneTimePassword = "123456";

        emailSenderService.sendNewPassword(toEmail, username, oneTimePassword);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        // Verify the content of the message
        assertEquals(toEmail, sentMessage.getTo()[0]);
        assertEquals("Exploding Kittens: Password Reset", sentMessage.getSubject());
        assertEquals("noreply.explodingkittens17@gmail.com", sentMessage.getFrom());
    }
}
