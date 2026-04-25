package com.ecommerce.main.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Dosyaları local diske kaydeden StorageService implementasyonu.
 *
 * Cloud'a geçmek için bu class'ı kullanmayı bırakıp
 * CloudinaryStorageService (veya S3StorageService) yazman yeterli.
 * Hiçbir Controller/Service/Entity değişmez.
 */
@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID() + extension;

            Path targetDir = Paths.get(uploadDir, folder);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            return baseUrl + "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Dosya kaydedilemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) return;
        try {
            // "http://localhost:8080/uploads/products/abc.jpg" → "products/abc.jpg"
            String relativePath = url.replace(baseUrl + "/uploads/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Silme başarısız olursa log at ama işlemi durdurma
            System.err.println("Dosya silinemedi: " + url + " — " + e.getMessage());
        }
    }
}
