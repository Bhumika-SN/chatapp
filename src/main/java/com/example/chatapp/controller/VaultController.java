package com.example.chatapp.controller;

import com.example.chatapp.model.VaultItem;
import com.example.chatapp.service.FileStorageService;
import com.example.chatapp.service.VaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/vault")
public class VaultController {

    @Autowired private VaultService vaultService;
    @Autowired private FileStorageService fileStorageService;

    @GetMapping("/{owner}")
    public List<VaultItem> getAll(@PathVariable String owner) {
        return vaultService.getAll(owner);
    }

    @GetMapping("/{owner}/{type}")
    public List<VaultItem> getByType(@PathVariable String owner, @PathVariable String type) {
        return vaultService.getByType(owner, type);
    }

    @PostMapping("/note")
    public VaultItem saveNote(@RequestBody Map<String, String> body) {
        VaultItem item = new VaultItem();
        item.setOwner(body.get("owner"));
        item.setType("note");
        item.setTitle(body.get("title"));
        item.setContent(body.get("content"));
        return vaultService.save(item);
    }

    @PostMapping("/schedule")
    public VaultItem saveSchedule(@RequestBody Map<String, String> body) {
        VaultItem item = new VaultItem();
        item.setOwner(body.get("owner"));
        item.setType("schedule");
        item.setTitle(body.get("title"));
        item.setContent(body.get("content"));
        item.setScheduledRoom(body.get("room"));
        if (body.get("scheduledAt") != null)
            item.setScheduledAt(java.time.LocalDateTime.parse(body.get("scheduledAt")));
        return vaultService.save(item);
    }

    @PostMapping("/timer")
    public VaultItem saveTimer(@RequestBody Map<String, String> body) {
        VaultItem item = new VaultItem();
        item.setOwner(body.get("owner"));
        item.setType("timer");
        item.setTitle(body.get("title"));
        item.setContent(body.get("content"));
        return vaultService.save(item);
    }

    @PostMapping("/file")
    public Map<String, Object> uploadVaultFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("owner") String owner) {
        Map<String, Object> res = new HashMap<>();
        try {
            String url = fileStorageService.saveFile(file);
            String type = fileStorageService.getFileType(file.getContentType());
            VaultItem item = new VaultItem();
            item.setOwner(owner);
            item.setType("file");
            item.setTitle(file.getOriginalFilename());
            item.setFileUrl(url);
            item.setFileName(file.getOriginalFilename());
            item.setFileType(type);
            VaultItem saved = vaultService.save(item);
            res.put("success", true);
            res.put("item", saved);
        } catch (Exception e) {
            res.put("error", e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, @RequestParam String owner) {
        return vaultService.delete(id, owner);
    }

    @PutMapping("/{id}/complete")
    public Map<String, Object> toggleComplete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return vaultService.toggleComplete(id, body.get("owner"));
    }
}