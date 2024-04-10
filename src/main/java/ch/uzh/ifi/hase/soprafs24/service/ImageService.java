package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageService {

    /**
     * Saves an image file to a specified local directory and names it uniquely to avoid overwrites.
     * @param uploadDirectory the directory path where the image should be saved.
     * @param imageFile the multipart file uploaded by the user, to be saved on disk.
     * @return a String containing the unique file name under which the image was saved.
     * @throws IOException if an I/O error occurs during file saving.
     */
    public String saveImageToStorage(String uploadDirectory, MultipartFile imageFile) throws IOException {
        String uniqueFileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

        Path uploadPath = Path.of(uploadDirectory);
        Path filePath = uploadPath.resolve(uniqueFileName);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    /**
     * Retrieves an image as a byte array from the specified directory.
     * @param imageDirectory the directory from which to retrieve the image.
     * @param imageName the name of the image file to retrieve.
     * @return a byte array representing the image, or null if the image does not exist.
     * @throws IOException if an I/O error occurs during file reading.
     */
    public byte[] getImage(String imageDirectory, String imageName) throws IOException {
        Path imagePath = Path.of(imageDirectory, imageName);

        if (Files.exists(imagePath)) {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            return imageBytes;
        } else {
            return null; // Handle missing images
        }
    }

    /**
     * Deletes an image file from the specified directory.
     * @param imageDirectory the directory from which the image should be deleted.
     * @param imageName the name of the image file to delete.
     * @return a String indicating the outcome of the delete operation ("Success" or "Failed").
     * @throws IOException if an I/O error occurs during file deletion.
     */
    public String deleteImage(String imageDirectory, String imageName) throws IOException {
        Path imagePath = Path.of(imageDirectory, imageName);

        if (Files.exists(imagePath)) {
            Files.delete(imagePath);
            return "Success";
        } else {
            return "Failed"; // Handle missing images
        }
    }
}
