package com.example.chatapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String avatarColor;
    private String avatarUrl;       // Google profile picture URL
    private String displayName;     // Full name from Google
    private String statusMessage;
    private boolean online;
    private LocalDateTime lastSeen;

    @PrePersist
    public void prePersist() {
        String[] colors = {"#FF6B6B","#4ECDC4","#45B7D1","#96CEB4","#DDA0DD","#F7DC6F","#82E0AA","#F1948A"};
        this.avatarColor = colors[(int)(Math.random() * colors.length)];
        this.statusMessage = "Hey there! I'm using ChatApp";
        this.online = false;
        this.lastSeen = LocalDateTime.now();
    }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}