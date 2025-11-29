package com.example.springboot_api.services.shared;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // Lưu file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về relative path để lưu vào database (không lưu full URL)
        return "/uploads/" + filename;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // Xử lý cả full URL và relative path
            String path = fileUrl;
            
            // Nếu là full URL, extract relative path
            if (path.startsWith("http://") || path.startsWith("https://")) {
                // Extract path sau domain (ví dụ: /uploads/filename)
                int pathStart = path.indexOf("/", path.indexOf("://") + 3);
                if (pathStart > 0) {
                    path = path.substring(pathStart);
                }
            }
            
            // Loại bỏ prefix /uploads/ để lấy filename
            String filename = path.replaceFirst("^/uploads/", "");
            Path filePath = Paths.get(uploadDir).resolve(filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileUrl, e);
        }
    }
}

