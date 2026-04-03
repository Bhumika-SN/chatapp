package com.example.chatapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vault_items")
public class VaultItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;          // username
    private String type;           // "file", "note", "schedule", "timer"
    private String title;
    private String content;        // note text or file URL
    private String fileUrl;
    private String fileName;
    private String fileType;
    private LocalDateTime scheduledAt;   // for scheduled messages
    private String scheduledRoom;        // which room to send to
    private boolean sent;                // for scheduled msgs
    private boolean completed;           // for timers/tasks
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.sent = false;
        this.completed = false;
    }
}