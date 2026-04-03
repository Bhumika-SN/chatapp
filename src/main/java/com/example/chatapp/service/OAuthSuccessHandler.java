package com.example.chatapp.service;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.security.JwtUtil;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name  = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");

        // use email prefix as username, sanitized
        String username = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");

        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setPassword("OAUTH_" + email); // not used for login
            u.setStatusMessage("Hey there! I'm using ChatApp");
            u.setAvatarUrl(picture);
            u.setDisplayName(name);
            return userRepository.save(u);
        });

        // update avatar/display name on each login
        user.setAvatarUrl(picture);
        user.setDisplayName(name);
        userRepository.save(user);

        String token = jwtUtil.generateToken(username);

        // redirect to frontend with token in URL — JS picks it up
        response.sendRedirect("/?token=" + token
                + "&username=" + username
                + "&avatarUrl=" + (picture != null ? picture : "")
                + "&displayName=" + (name != null ? name.replace(" ", "%20") : username));
    }
}