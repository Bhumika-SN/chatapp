package com.example.chatapp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String UPLOAD_DIR = "uploads/";

    public String saveFile(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains("."))
            ext = original.substring(original.lastIndexOf("."));
        String filename = UUID.randomUUID() + ext;
        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, file.getBytes());
        return "/uploads/" + filename;
    }

    public String getFileType(String contentType) {
        if (contentType == null) return "file";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("audio/")) return "audio";
        if (contentType.startsWith("video/")) return "video";
        return "file";
    }
}