package com.example.chatapp.controller;

import com.example.chatapp.model.*;
import com.example.chatapp.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.List;

@RestController
@CrossOrigin("*")
public class ChatController {

    @Autowired private ChatService chatService;
    @Autowired private SimpMessagingTemplate messaging;

    @MessageMapping("/sendMessage")
    public void sendMessage(Message message) {
        Message saved = chatService.saveMessage(message);
        messaging.convertAndSend("/topic/room/" + message.getRoomName(), (Object) saved);
    }

    @MessageMapping("/typing")
    public void typing(Map<String, String> payload) {
        messaging.convertAndSend("/topic/typing/" + payload.get("roomName"), (Object) payload);
    }

    @MessageMapping("/seen")
    public void markSeen(Map<String, Object> payload) {
        Long msgId = Long.valueOf(payload.get("messageId").toString());
        String username = payload.get("username").toString();
        chatService.markSeen(msgId, username).ifPresent(updated ->
                messaging.convertAndSend("/topic/seen/" + updated.getRoomName(), (Object) updated)
        );
    }

    @MessageMapping("/react")
    public void react(Map<String, String> payload) {
        Long msgId = Long.valueOf(payload.get("messageId"));
        chatService.addReaction(msgId, payload.get("username"), payload.get("emoji")).ifPresent(updated -> {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "reaction");
            response.put("message", updated);
            messaging.convertAndSend("/topic/room/" + updated.getRoomName(), (Object) response);
        });
    }

    @MessageMapping("/online")
    public void setOnline(Map<String, Object> payload) {
        String username = payload.get("username").toString();
        boolean online = Boolean.parseBoolean(payload.get("online").toString());
        chatService.setOnlineStatus(username, online);
        Map<String, Object> presence = new HashMap<>();
        presence.put("username", username);
        presence.put("online", online);
        messaging.convertAndSend("/topic/presence", (Object) presence);
    }

    @GetMapping("/api/messages/{roomName}")
    public List<Message> getMessages(@PathVariable String roomName) {
        return chatService.getMessagesByRoom(roomName);
    }

    @GetMapping("/api/messages/{roomName}/search")
    public List<Message> search(@PathVariable String roomName, @RequestParam String q) {
        return chatService.searchMessages(roomName, q);
    }

    @PostMapping("/api/rooms")
    public ChatRoom createRoom(@RequestBody Map<String, String> body) {
        return chatService.createOrGetRoom(body.get("name"), body.get("createdBy"));
    }

    @GetMapping("/api/rooms")
    public List<ChatRoom> getRooms() {
        return chatService.getAllRooms();
    }

    @PutMapping("/api/messages/{id}/edit")
    public Map<String, Object> editMessage(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        chatService.editMessage(id, body.get("content"), body.get("username"))
                .ifPresentOrElse(updated -> {
                    messaging.convertAndSend("/topic/edit/" + updated.getRoomName(), (Object) updated);
                    res.put("success", true);
                    res.put("message", updated);
                }, () -> res.put("error", "Not found or not authorized"));
        return res;
    }

    @PutMapping("/api/messages/{id}/delete")
    public Map<String, Object> deleteMessage(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        chatService.deleteMessage(id, body.get("username"))
                .ifPresentOrElse(updated -> {
                    messaging.convertAndSend("/topic/delete/" + updated.getRoomName(), (Object) updated);
                    res.put("success", true);
                    res.put("message", updated);
                }, () -> res.put("error", "Not found or not authorized"));
        return res;
    }

    @PutMapping("/api/messages/{id}/star")
    public Map<String, Object> starMessage(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        chatService.toggleStar(id, body.get("username"))
                .ifPresentOrElse(updated -> {
                    res.put("success", true);
                    res.put("starred", updated.isStarred());
                }, () -> res.put("error", "Not found"));
        return res;
    }

    @GetMapping("/api/users/online")
    public List<User> getOnlineUsers() {
        return chatService.getOnlineUsers();
    }
    @PostMapping("/api/rooms/{roomName}/lock")
    public Map<String, Object> lockRoom(@PathVariable String roomName,
                                        @RequestBody Map<String, String> body) {
        return chatService.setRoomPassword(roomName, body.get("password"));
    }

    @PostMapping("/api/rooms/{roomName}/verify")
    public Map<String, Object> verifyRoom(@PathVariable String roomName,
                                          @RequestBody Map<String, String> body) {
        return chatService.verifyRoomPassword(roomName, body.get("password"));
    }

    @PostMapping("/api/messages/delete-selected")
    public Map<String, Object> deleteSelected(@RequestBody Map<String, Object> body) {
        List<Long> ids = ((List<?>) body.get("ids"))
                .stream().map(id -> Long.valueOf(id.toString())).toList();
        return chatService.deleteSelectedMessages(ids, body.get("username").toString());
    }

    @DeleteMapping("/api/rooms/{roomName}/clear")
    public Map<String, Object> clearChat(@PathVariable String roomName,
                                         @RequestParam String username) {
        Map<String, Object> res = chatService.clearChat(roomName, username);
        messaging.convertAndSend("/topic/clear/" + roomName, (Object) res);
        return res;
    }

    @PutMapping("/api/messages/{id}/snap-viewed")
    public Map<String, Object> snapViewed(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        chatService.markSnapViewed(id).ifPresent(m -> {
            res.put("success", true);
            messaging.convertAndSend("/topic/snap/" + m.getRoomName(), (Object) Map.of("id", id, "viewed", true));
        });
        return res;
    }
}