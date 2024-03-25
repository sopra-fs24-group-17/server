package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendNewPassword(String toEmail, String username, String oneTimePassword){
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom("noreply.explodingkittens17@gmail.com");
        message.setTo(toEmail);

        String text = "Hello " + username + "!\n\n" +
                "You requested a new password. We are happy to help you out.\n\n" +
                "Your new password is: " +
                oneTimePassword + "\n\n" +
                "Please change this password after login!\n\n" +
                "All the best and happy playing,\n" +
                "Your ExplodingKittens Team";

        message.setText(text);
        message.setSubject("Exploding Kittens: Password Reset");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            System.out.println("Error sending email: " + e.getMessage());
        }
    }
}
