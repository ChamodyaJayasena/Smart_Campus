package com.smartcampus.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smartcampus.model.Notification;
import com.smartcampus.model.NotificationCategory;
import com.smartcampus.model.UserRole;
import com.smartcampus.repository.NotificationRepository;
import com.smartcampus.repository.UserAccountRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final String adminUsername;

    public NotificationService(NotificationRepository notificationRepository,
                               UserAccountRepository userAccountRepository,
                               @Value("${app.security.admin-username:admin}") String adminUsername) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.adminUsername = normalizeAdminUsername(adminUsername);
    }

    public Notification createNotification(String email, NotificationCategory category, String title, String message, String type, String referenceId) {
        Notification notif = new Notification();
        notif.setRecipientEmail(email);
        notif.setCategory(category);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        notif.setReferenceId(referenceId);
        notif.setRead(false);
        notif.setCreatedAt(Instant.now().toString());
        return notificationRepository.save(notif);
    }

    public Notification createAdminNotification(NotificationCategory category, String title, String message, String type, String referenceId) {
        Notification lastCreated = null;
        List<String> adminEmails = userAccountRepository.findByRole(UserRole.ROLE_ADMIN).stream()
                .map(user -> user.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .toList();

        List<String> recipients = new ArrayList<>(adminEmails);
        if (adminUsername != null && !adminUsername.isBlank()) {
            boolean alreadyIncluded = recipients.stream()
                    .anyMatch(email -> email.equalsIgnoreCase(adminUsername));
            if (!alreadyIncluded) {
                recipients.add(adminUsername);
            }
        }

        for (String adminEmail : recipients) {
            lastCreated = createNotification(adminEmail, category, title, message, type, referenceId);
        }

        if (lastCreated != null) {
            return lastCreated;
        }

        // Fallback keeps legacy behavior when no admin account exists yet in the users collection.
        return createNotification(adminUsername, category, title, message, type, referenceId);
    }

    public List<Notification> getUserNotifications(String email) {
        List<Notification> own = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
        if (!isAdminEmail(email)) {
            return own;
        }

        List<Notification> merged = new ArrayList<>(own);
        if (!"admin".equalsIgnoreCase(email)) {
            addAliasNotifications(merged, "admin");
        }
        if (adminUsername != null && !adminUsername.equalsIgnoreCase(email) && !"admin".equalsIgnoreCase(adminUsername)) {
            addAliasNotifications(merged, adminUsername);
        }
        merged.sort((a, b) -> {
            String left = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String right = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return right.compareTo(left);
        });
        return merged;
    }

    public Notification markAsRead(String id, String email) {
        Notification notif = notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));
        boolean ownsNotification = email.equalsIgnoreCase(notif.getRecipientEmail());
        boolean adminCanReadLegacy = isAdminEmail(email) && isAdminAlias(notif.getRecipientEmail());
        if (!ownsNotification && !adminCanReadLegacy) {
            throw new RuntimeException("Unauthorized");
        }
        notif.setRead(true);
        return notificationRepository.save(notif);
    }
    
    public void markAllAsRead(String email) {
        List<Notification> unread = notificationRepository.findByRecipientEmailAndIsReadFalse(email);
        if (isAdminEmail(email)) {
            if (!"admin".equalsIgnoreCase(email)) {
                unread.addAll(notificationRepository.findByRecipientEmailAndIsReadFalse("admin"));
            }
            if (adminUsername != null && !adminUsername.equalsIgnoreCase(email) && !"admin".equalsIgnoreCase(adminUsername)) {
                unread.addAll(notificationRepository.findByRecipientEmailAndIsReadFalse(adminUsername));
            }
        }
        for (Notification n : unread) {
            n.setRead(true);
        }
        if (!unread.isEmpty()) {
            notificationRepository.saveAll(unread);
        }
    }

    private boolean isAdminEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (isAdminAlias(email)) {
            return true;
        }
        return userAccountRepository.findByEmail(email)
                .map(account -> account.getRole() == UserRole.ROLE_ADMIN)
                .orElse(false);
    }

    private boolean isAdminAlias(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if ("admin".equalsIgnoreCase(email)) {
            return true;
        }
        return adminUsername != null && adminUsername.equalsIgnoreCase(email);
    }

    private void addAliasNotifications(List<Notification> target, String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        target.addAll(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(recipient));
    }

    private String normalizeAdminUsername(String username) {
        if (username == null || username.isBlank()) {
            return "admin";
        }
        return username.trim();
    }
}