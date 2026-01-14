package com.anasol.cafe.service;

import com.anasol.cafe.entity.User;
import com.anasol.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final UserRepository userRepository;

    // Store emitters: Map<UserEmail, SseEmitter>
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribeToCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("⚠️ Subscription attempt by unauthenticated user");
            return null;
        }

        String email = authentication.getName();
        log.info("🔔 SSE Subscription Request from: {}", email);

        // 1. Create Emitter with Infinite Timeout (Important!)
        // Default is 30 sec, we set to Long.MAX_VALUE
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 2. Add callbacks to remove emitter when done
        emitter.onCompletion(() -> {
            log.info("✅ SSE Connection Completed for: {}", email);
            emitters.remove(email);
        });

        emitter.onTimeout(() -> {
            log.warn("⌛ SSE Connection Timed Out for: {}", email);
            emitter.complete();
            emitters.remove(email);
        });

        emitter.onError((e) -> {
            log.error("❌ SSE Connection Error for: {}: {}", email, e.getMessage());
            emitter.complete();
            emitters.remove(email);
        });

        // 3. Store the emitter
        emitters.put(email, emitter);

        // 4. Send an initial "CONNECTED" message to confirm connection
        try {
            emitter.send(SseEmitter.event()
                    .name("open")
                    .data("Connection Established for " + email));
            log.info("🚀 SSE Emitter created and stored for: {}", email);
        } catch (IOException e) {
            log.error("❌ Failed to send initial SSE event: {}", e.getMessage());
            emitters.remove(email);
        }

        return emitter;
    }

    public void sendNotificationToUser(String email, Object notificationData) {
        SseEmitter emitter = emitters.get(email);
        if (emitter != null) {
            try {
                log.info("📤 Sending Notification to: {}", email);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(notificationData));
            } catch (IOException e) {
                log.error("❌ Failed to send notification to {}: {}", email, e.getMessage());
                emitters.remove(email);
            }
        } else {
            // log.debug("User {} is not online/subscribed", email);
        }
    }

    // Optional: Keep-alive heartbeat every 1 minute to prevent load balancer timeouts
    @Scheduled(fixedRate = 60000)
    public void sendHeartbeat() {
        emitters.forEach((email, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.warn("💓 Heartbeat failed for {}, removing emitter.", email);
                emitters.remove(email);
            }
        });
    }

    public String unsubscribeCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String email = authentication.getName();
            SseEmitter emitter = emitters.remove(email);
            if (emitter != null) {
                emitter.complete();
                log.info("🔕 User Unsubscribed: {}", email);
                return "Unsubscribed successfully";
            }
        }
        return "User not found or not subscribed";
    }

    public boolean isCurrentUserOnline() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && emitters.containsKey(authentication.getName());
    }
}