package com.example.chatapp.controller;

import com.example.chatapp.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/files")
public class FileController {

    @Autowired private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            String url = fileStorageService.saveFile(file);
            String type = fileStorageService.getFileType(file.getContentType());
            res.put("url", url);
            res.put("type", type);
            res.put("name", file.getOriginalFilename());
            res.put("success", true);
        } catch (Exception e) {
            res.put("error", e.getMessage());
        }
        return res;
    }

    @GetMapping("/serve/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = Paths.get("uploads/").resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            }
        } catch (Exception e) {}
        return ResponseEntity.notFound().build();
    }
}