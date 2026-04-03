package com.example.chatapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String content;
    private String roomName;

    private LocalDateTime sentAt;
    private String formattedTime;

    private boolean edited;
    private boolean deleted;

    private String seenBy;   // comma-separated usernames
    private boolean starred;

    private String replyToId; // id of message being replied to

    // ✅ FILE / MEDIA SUPPORT
    private String fileUrl;
    private String fileType;   // "image", "audio", "file"
    private String fileName;

    // ✅ SNAP (DISAPPEARING MESSAGE)
    private boolean snapView;    // disappears after viewing
    private boolean snapViewed;  // already viewed once

    // ✅ SCHEDULED MESSAGE
    private boolean scheduledSend;
    private LocalDateTime scheduledAt;

    // ✅ REACTIONS
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_reactions", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "reaction")
    private List<String> reactions = new ArrayList<>();  // "alice:❤️", "bob:😂"

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
        this.formattedTime = sentAt.format(DateTimeFormatter.ofPattern("hh:mm a"));

        this.edited = false;
        this.deleted = false;
        this.starred = false;

        this.seenBy = "";

        this.snapView = false;
        this.snapViewed = false;

        this.scheduledSend = false;
    }
}