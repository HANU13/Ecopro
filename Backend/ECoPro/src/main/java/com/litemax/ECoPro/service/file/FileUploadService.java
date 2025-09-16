package com.litemax.ECoPro.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${file.upload.directory}")
    private String uploadDirectory;

    @Value("${app.backend.url}")
    private String backendUrl;

    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        log.info("Uploading file: {} to subfolder: {}", file.getOriginalFilename(), subfolder);

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file type for images
        String contentType = file.getContentType();
        if (contentType == null || !isImageFile(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only images are allowed.");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory, subfolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = backendUrl + "/api/files/" + subfolder + "/" + filename;
        log.info("File uploaded successfully: {}", fileUrl);

        return fileUrl;
    }

    private boolean isImageFile(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }
}