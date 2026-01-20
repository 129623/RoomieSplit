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

    @org.springframework.beans.factory.annotation.Autowired
    private com.roomiesplit.backend.service.ocr.BaiduOCRService baiduOCRService;

    private final Path rootLocation = Paths.get("uploads");

    public FileUploadController() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }
    }

    @PostMapping
    public Result<?> handleFileUpload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "skipOCR", required = false, defaultValue = "false") boolean skipOCR) {
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

            String fileUrl = "/uploads/" + filename;
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("url", fileUrl);

            // Call Baidu OCR only if not skipped
            java.util.Map<String, Object> ocrData = null;
            if (!skipOCR) {
                try {
                    Path filePath = this.rootLocation.resolve(filename);
                    if (baiduOCRService != null) {
                        ocrData = baiduOCRService.recognizeReceipt(filePath);
                    } else {
                        ocrData = new java.util.HashMap<>();
                        ocrData.put("description", "错误: OCR服务未注入(Autowired失败)");
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    ocrData = new java.util.HashMap<>();
                    ocrData.put("description", "后端异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }

                if (ocrData == null) {
                    ocrData = new java.util.HashMap<>();
                    ocrData.put("description", "未知错误: OCR返回空数据");
                } else if (ocrData.containsKey("error")) {
                    String err = String.valueOf(ocrData.get("error"));
                    ocrData.put("description", "API错误: " + err);
                }
            } else {
                ocrData = new java.util.HashMap<>();
                ocrData.put("description", "OCR Skipped");
            }

            // File is now persisted for access via URL
            // Files.deleteIfExists(filePath); replaced by persistence logic

            data.put("ocr", ocrData);
            return Result.success(data);

        } catch (Exception e) {
            return Result.error(500, "Failed to upload file: " + e.getMessage());
        }
    }
}
