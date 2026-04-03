package com.example.chatapp.service;

import com.example.chatapp.model.*;
import com.example.chatapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChatService {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private UserRepository userRepository;

    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    public List<Message> getMessagesByRoom(String roomName) {
        return messageRepository.findByRoomNameOrderBySentAtAsc(roomName);
    }

    public List<Message> searchMessages(String roomName, String keyword) {
        return messageRepository.searchInRoom(roomName, keyword);
    }

    public ChatRoom createOrGetRoom(String name, String createdBy) {
        return chatRoomRepository.findByName(name).orElseGet(() -> {
            ChatRoom r = new ChatRoom();
            r.setName(name);
            r.setCreatedBy(createdBy);
            return chatRoomRepository.save(r);
        });
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public Optional<Message> editMessage(Long id, String newContent, String username) {
        return messageRepository.findById(id).map(m -> {
            // allow edit only by sender
            if (m.getSender().equals(username)) {
                m.setContent(newContent);
                m.setEdited(true);
                return messageRepository.save(m);
            }
            return m;
        });
    }

    public Optional<Message> deleteMessage(Long id, String username) {
        return messageRepository.findById(id).map(m -> {
            // allow delete by sender
            if (m.getSender().equals(username)) {
                m.setDeleted(true);
                m.setContent("This message was deleted");
                return messageRepository.save(m);
            }
            return m;
        });
    }

    public Optional<Message> addReaction(Long id, String username, String emoji) {
        return messageRepository.findById(id).map(m -> {
            m.getReactions().removeIf(r -> r.startsWith(username + ":"));
            m.getReactions().add(username + ":" + emoji);
            return messageRepository.save(m);
        });
    }

    public Optional<Message> markSeen(Long id, String username) {
        return messageRepository.findById(id).map(m -> {
            String seen = m.getSeenBy() == null ? "" : m.getSeenBy();
            if (!seen.contains(username)) {
                m.setSeenBy(seen.isEmpty() ? username : seen + "," + username);
                return messageRepository.save(m);
            }
            return m;
        });
    }

    public Optional<Message> toggleStar(Long id, String username) {
        return messageRepository.findById(id).map(m -> {
            m.setStarred(!m.isStarred());
            return messageRepository.save(m);
        });
    }

    public void setOnlineStatus(String username, boolean online) {
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setOnline(online);
            if (!online) u.setLastSeen(java.time.LocalDateTime.now());
            userRepository.save(u);
        });
    }

    public List<User> getOnlineUsers() {
        return userRepository.findAll().stream().filter(User::isOnline).toList();
    }
}