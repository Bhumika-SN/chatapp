package com.example.chatapp.config;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.springframework.web.socket.messaging.SessionConnectedEvent;   // ✅ FIX
import org.springframework.web.socket.messaging.SessionDisconnectEvent; // ✅ FIX

import java.util.*;
@Component
public class WebSocketEventListener {

    public static Set<String> onlineUsers = new HashSet<>();

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        System.out.println("User connected");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        System.out.println("User disconnected");
    }
}