package com.anasol.cafe.producer;


import com.anasol.cafe.entity.Notification;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
// org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationProducer {



//    @Autowired
//    private RedisTemplate redisTemplate;

    public void sendNotification(Notification notification) {
        String topic = "notifications";
        String key = notification.getReceiver();
        log.info("Notification received and sending to redis: {}", notification);

//        CompletableFuture<SendResult<String, Notification>> future = kafkaTemplate.send(topic, key, notification);
        //edisTemplate.convertAndSend("notifications:all", notification);
//        future.whenComplete((result, ex) -> {
//            if (ex != null) {
//                System.err.printf(" Failed to send to %s: %s%n", key, ex.getMessage());
//            } else {
//                RecordMetadata metadata = result.getRecordMetadata();
//                System.out.printf("Notification sent to %s | Partition: %d | Offset: %d%n",
//                        key, metadata.partition(), metadata.offset());
//            }
//        });
    }
}
