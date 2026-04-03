package com.example.chatapp.service;

import com.example.chatapp.model.*;
import com.example.chatapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.List;

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

    // Lock / unlock room
    public Map<String, Object> setRoomPassword(String roomName, String password) {
        Map<String, Object> res = new HashMap<>();
        chatRoomRepository.findByName(roomName).ifPresentOrElse(room -> {
            room.setRoomPassword(password);
            room.setLocked(password != null && !password.isEmpty());
            chatRoomRepository.save(room);
            res.put("success", true);
            res.put("locked", room.isLocked());
        }, () -> res.put("error", "Room not found"));
        return res;
    }

    public Map<String, Object> verifyRoomPassword(String roomName, String password) {
        Map<String, Object> res = new HashMap<>();
        chatRoomRepository.findByName(roomName).ifPresentOrElse(room -> {
            if (!room.isLocked()) { res.put("success", true); return; }
            if (room.getRoomPassword().equals(password)) res.put("success", true);
            else res.put("error", "Wrong password");
        }, () -> res.put("error", "Room not found"));
        return res;
    }

    public Map<String, Object> deleteSelectedMessages(List<Long> ids, String username) {
        Map<String, Object> res = new HashMap<>();
        ids.forEach(id -> messageRepository.findById(id).ifPresent(m -> {
            if (m.getSender().equals(username)) {
                m.setDeleted(true);
                m.setContent("This message was deleted");
                messageRepository.save(m);
            }
        }));
        res.put("success", true);
        res.put("count", ids.size());
        return res;
    }

    public Map<String, Object> clearChat(String roomName, String username) {
        Map<String, Object> res = new HashMap<>();
        List<Message> msgs = messageRepository.findByRoomNameOrderBySentAtAsc(roomName);
        msgs.forEach(m -> {
            m.setDeleted(true);
            m.setContent("This message was deleted");
            messageRepository.save(m);
        });
        res.put("success", true);
        return res;
    }

    public Optional<Message> markSnapViewed(Long id) {
        return messageRepository.findById(id).map(m -> {
            m.setSnapViewed(true);
            return messageRepository.save(m);
        });
    }
}