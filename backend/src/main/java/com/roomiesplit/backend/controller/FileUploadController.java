package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
public class FileUploadController {

    private final Path rootLocation = Paths.get("uploads");

    public FileUploadController() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }
    }

    @PostMapping
    public Result<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error(400, "Failed to store empty file.");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename));

            // Return URL (Assuming server is accessible at same host)
            // Ideally, host should be dynamic or configured. For now, returning relative
            // path or full URL if possible.
            // Client will prepend base URL. But wait, client assumes full URL for avatar.
            // Let's return the relative path "/uploads/filename" and let the client handle
            // it?
            // Or better, return the full resource path if we knew the host.
            // Since we are adding ResourceHandler for /uploads/**, the URL is just
            // /uploads/filename

            // NOTE: Returning a path starting with "http" is better for the client logic I
            // saw earlier.
            // But I don't easily know the server IP seen by the emulator (10.0.2.2
            // usually).
            // Let's return a path "/uploads/..." and ensure client handles it, or try to
            // construct full URL.
            // The client code checks `avatarUrl.startsWith("http")`.
            // So I should probably modify client to handle "/uploads" or construct a full
            // URL here.

            // Strategy: Return "/uploads/" + filename. Client needs to prepend BASE_URL if
            // it doesn't start with http.
            // But wait, the existing client code:
            // if (avatarUrl.startsWith("http")) ...
            // else if (avatarUrl.startsWith("avatar_")) ...
            // So if I return "/uploads/...", the client will ignore it!

            // I should modify the client to handle non-http paths by prepending base url,
            // OR make this controller return a full URL.
            // Constructing full URL requires HttpServletRequest.

            String fileUrl = "/uploads/" + filename;
            return Result.success(fileUrl);

        } catch (Exception e) {
            return Result.error(500, "Failed to upload file: " + e.getMessage());
        }
    }
}
