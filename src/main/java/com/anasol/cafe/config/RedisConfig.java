//package com.anasol.cafe.config;
//
//import com.anasol.cafe.consumer.EventListener;
//import com.anasol.cafe.entity.Notification;
//import com.anasol.cafe.service.NotificationPushService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisPassword;
//import org.springframework.data.redis.connection.RedisSentinelConfiguration;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.listener.ChannelTopic;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//
//import java.time.Duration;
//
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//
//    private final RedisProperties redisProperties;
//
//    public RedisConfig(RedisProperties redisProperties) {
//        this.redisProperties = redisProperties;
//    }
//    @Bean
//    public EventListener eventListener(ObjectMapper objectMapper, NotificationPushService notificationPushService) {
//        return new EventListener(objectMapper, notificationPushService);
//    }
////
////    @Bean
////    public LettuceConnectionFactory redisConnectionFactory() {
////        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
////                .master(redisProperties.getSentinel().getMaster());
////
////        for (String s : redisProperties.getSentinel().getNodes()) {
////            String[] parts = s.split(":");
////            if (parts.length != 2) {
////                throw new IllegalArgumentException("Invalid sentinel node: " + s);
////            }
////            sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
////        }
////
////        // This sets the password for Redis master
////        sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
////
////        return new LettuceConnectionFactory(sentinelConfig);
////    }
//
//     @Bean
//     public LettuceConnectionFactory redisConnectionFactory() {
//         RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
//         redisConfig.setHostName("hrms.anasolconsultancyservices.com");
//         redisConfig.setPort(6378);
//         redisConfig.setPassword(RedisPassword.of("RedisPasswordMaster")); // your Redis password
//
//         return new LettuceConnectionFactory(redisConfig);
//     }
//
//
//    @Bean
//    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(Duration.ofMinutes(5)))
//                .build();
//    }
//
//    @Bean
//    public RedisTemplate<String, Notification> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
//        RedisTemplate<String, Notification> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(serializer);
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(serializer);
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplateObject(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(serializer);
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(serializer);
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public MessageListenerAdapter messageListener(EventListener eventListener) {
//        return new MessageListenerAdapter(eventListener);
//    }
//
//    @Bean
//    public TaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(10);
//        executor.setMaxPoolSize(20);
//        executor.setQueueCapacity(100);
//        executor.setThreadNamePrefix("redis-listener-");
//        executor.initialize();
//        return executor;
//    }
//
//    @Bean
//    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory, MessageListenerAdapter messageListener) {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setTaskExecutor(taskExecutor());
//        container.addMessageListener(messageListener, new ChannelTopic("notifications:all"));
//        return container;
//    }
//
//    @Bean
//    public ObjectMapper objectMapper() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        return objectMapper;
//    }
//}