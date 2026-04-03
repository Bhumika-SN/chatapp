package com.example.chatapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String createdBy;
    private LocalDateTime createdAt;
    private boolean pinned;

    // ✅ ADDED
    private String roomPassword;   // null means no lock
    private boolean locked;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.pinned = false;
    }
}