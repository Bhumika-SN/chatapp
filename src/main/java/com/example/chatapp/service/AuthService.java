package com.example.chatapp.service;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    public Map<String, Object> register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Username already taken");
            return err;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        String token = jwtUtil.generateToken(username);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", username);
        result.put("avatarColor", user.getAvatarColor());
        result.put("statusMessage", user.getStatusMessage());
        return result;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", "Invalid username or password");

        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(u -> {
                    String token = jwtUtil.generateToken(username);
                    Map<String, Object> result = new HashMap<>();
                    result.put("token", token);
                    result.put("username", username);
                    result.put("avatarColor", u.getAvatarColor() != null ? u.getAvatarColor() : "#4ECDC4");
                    result.put("statusMessage", u.getStatusMessage() != null ? u.getStatusMessage() : "");
                    return result;
                })
                .orElse(err);
    }

    public Map<String, Object> updateProfile(String username, String statusMessage) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", "User not found");

        return userRepository.findByUsername(username).map(u -> {
            if (statusMessage != null) u.setStatusMessage(statusMessage);
            userRepository.save(u);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("statusMessage", u.getStatusMessage());
            return result;
        }).orElse(err);
    }
}