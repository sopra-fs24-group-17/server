package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("dev")
public class PasswordServiceTest {

    private PasswordService passwordService;
    private String testPassword;
    private String testHash;

    @BeforeEach
    public void setUp() {
        passwordService = new PasswordService();
        testPassword = "correctPassword";
        testHash = passwordService.securePassword(testPassword);
    }

    @Test
    public void verifyPassword_WithCorrectPassword_ShouldReturnTrue() {
        assertTrue(passwordService.verifyPassword(testHash, testPassword),
                "The password should be verified successfully.");
    }

    @Test
    public void verifyPassword_WithIncorrectPassword_ShouldReturnFalse() {
        String incorrectPassword = "wrongPassword";
        assertFalse(passwordService.verifyPassword(testHash, incorrectPassword),
                "The password verification should fail.");
    }

    @Test
    public void generateRandomPassword_WithPositiveLength_ShouldReturnCorrectLength() {
        assertEquals(10, passwordService.generateRandomPassword(10).length(),
                "The generated password should have the specified length.");
    }
}

