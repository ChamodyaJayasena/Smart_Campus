package com.smartcampus.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.model.Notification;
import com.smartcampus.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "${app.frontend.base-url:http://localhost:5173}", allowCredentials = "true")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String getEmail(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        }
        return authentication.getName(); // For regular users, name is email
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        String email = getEmail(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(notificationService.getUserNotifications(email));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id, Authentication authentication) {
        String email = getEmail(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(notificationService.markAsRead(id, email));
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String email = getEmail(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok().build();
    }
}