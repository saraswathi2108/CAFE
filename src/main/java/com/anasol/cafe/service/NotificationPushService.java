//package com.anasol.cafe.service;
//
//
//
//import com.anasol.cafe.repository.NotificationRepository;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//@Slf4j
//public class NotificationPushService {
//
//
//    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
//
//    @Autowired
//    private NotificationRepository repository;
//
//    public boolean isOnline(String username) {
//        return emitters.containsKey(username) && !emitters.get(username).isEmpty();
//    }
//
//    public String unSubscribe(String username) {
//        List<SseEmitter> userEmitters = emitters.remove(username);
//        if (userEmitters != null) {
//            userEmitters.forEach(SseEmitter::complete);
//        }
//        return "Unsubscribed " + username;
//    }
//
//    public SseEmitter subscribe(String username) {
//        SseEmitter emitter = new SseEmitter(0L); // infinite timeout
//
//        // Add emitter to the list for the user
//        emitters.computeIfAbsent(username, k -> Collections.synchronizedList(new ArrayList<>())).add(emitter);
//
//        emitter.onCompletion(() -> removeEmitter(username, emitter));
//        emitter.onTimeout(() -> removeEmitter(username, emitter));
//        emitter.onError(e -> removeEmitter(username, emitter));
//
//        System.out.println("SSE connected: " + username);
//
//        // Optional: Send unread notifications (if needed)
////        List<Notification> unread = repository.findByReceiverAndReadFalseOrderByCreatedAtDesc(username);
////        unread.forEach(notification -> sendToEmitter(emitter, username, notification));
//
//        return emitter;
//    }
//
//    public void sendNotificationToUser(String username, Object notification) {
//        List<SseEmitter> userEmitters = emitters.get(username);
//        if (userEmitters != null && !userEmitters.isEmpty()) {
//            List<SseEmitter> deadEmitters = new ArrayList<>();
//            log.info("found {} emitters for user {} sending notification {}", userEmitters.size(), username, notification);
//            userEmitters.forEach(emitter -> {
//                try {
//                    emitter.send(SseEmitter.event()
//                            .name("notification")
//                            .data(notification));
//                    log.info("Notification sent to user {} with notification {}", username, notification);
//                    System.out.println("Real-time notification sent to: " + username);
//                } catch (IOException e) {
//                    // emitter.completeWithError(e);
//                    deadEmitters.add(emitter);
//                    log.error("Failed to send notification to user {}", username);
//                }
//            });
//
//            // Clean up dead emitters
//            userEmitters.removeAll(deadEmitters);
//        } else {
//            log.info("User {} is offline. Notification {} saved only.", username, notification);
//            System.out.println("User " + username + " is offline. Notification saved only.");
//        }
//    }
//
//    private void removeEmitter(String username, SseEmitter emitter) {
//        List<SseEmitter> userEmitters = emitters.get(username);
//        if (userEmitters != null) {
//            userEmitters.remove(emitter);
//            if (userEmitters.isEmpty()) {
//                emitters.remove(username);
//            }
//        }
//    }
//
//    private void sendToEmitter(SseEmitter emitter, String username, Object data) {
//        try {
//            emitter.send(SseEmitter.event()
//                    .name("notification")
//                    .data(data));
//            System.out.println("Unread notification sent to: " + username);
//        } catch (IOException e) {
//            emitter.completeWithError(e);
//            removeEmitter(username, emitter);
//        }
//    }
//}
