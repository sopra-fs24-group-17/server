package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.service.ContentModerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("dev")
@ExtendWith(MockitoExtension.class)
public class ContentModerationServiceTest {

    @InjectMocks
    private ContentModerationService contentModerationService;

    @Test
    public void testCheckToxicity() {
        String testUsername = "exampleUsername";
        contentModerationService.checkToxicity(testUsername);
        double score = contentModerationService.checkToxicity(testUsername);
        assertTrue(score <0.5, "Expected a toxicity score below 0.5");
    }

    @Test
    public void testCheckToxicity_Toxic() {
        String testUsername = "idiot";
        double score = contentModerationService.checkToxicity(testUsername);
        assertTrue(score > 0.7, "Expected a toxicity score greater than 0.5");
    }

    @Test
    public void invalidCheckToxicity() {
        String testUsername = "123456@?";
        double score = contentModerationService.checkToxicity(testUsername);
        assertTrue(score == 0);
    }
}