package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("dev")
public class ImageServiceTest {

    private final ImageService imageService = new ImageService();

    @TempDir
    Path tempDirectory;

    @Test
    void testSaveImageToStorage() throws IOException {
        MultipartFile mockMultipartFile = new MockMultipartFile(
                "test-image",
                "test-image.png",
                "image/png",
                "test image content".getBytes()
        );

        String savedFileName = imageService.saveImageToStorage(tempDirectory.toString(), mockMultipartFile);
        assertTrue(Files.exists(tempDirectory.resolve(savedFileName)));
    }

    @Test
    void testGetImage() throws IOException {
        Path imagePath = Files.createFile(tempDirectory.resolve("test-image.png"));

        Files.writeString(imagePath, "test image content");

        byte[] imageBytes = imageService.getImage(tempDirectory.toString(), "test-image.png");

        assertNotNull(imageBytes);
        assertEquals("test image content", new String(imageBytes));
    }
}
