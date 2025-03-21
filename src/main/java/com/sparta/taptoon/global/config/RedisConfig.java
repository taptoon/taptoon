package com.sparta.taptoon.global.config;

import com.sparta.taptoon.global.handler.WebSocketHandler;
import com.sparta.taptoon.global.redis.RedisSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    @Bean
    public ApplicationRunner redisConnectionChecker(RedisConnectionFactory redisConnectionFactory) {
        return args -> {
            int maxRetries = 10;
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    redisConnectionFactory.getConnection().ping(); // Redis 연결 체크
                    System.out.println("✅ Redis 연결 성공!");
                    return;
                } catch (Exception e) {
                    attempt++;
                    System.out.println("⏳ Redis가 아직 실행되지 않음. 재시도 중... (" + attempt + "/" + maxRetries + ")");
                    Thread.sleep(5000); // 5초 대기 후 재시도
                }
            }
            throw new RuntimeException("❌ Redis 서버에 연결할 수 없습니다.");
        };
    }

    /**
     * Redis 서버와 연결을 설정하는 역할
     * LettuceConnectionFactory 를 사용하여 Redis 서버와 연결
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String host = System.getenv("SPRING_DATA_REDIS_HOST") != null ?
                System.getenv("SPRING_DATA_REDIS_HOST") : "localhost";
        int port = System.getenv("SPRING_DATA_REDIS_PORT") != null ?
                Integer.parseInt(System.getenv("SPRING_DATA_REDIS_PORT")) : 6379;
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * Redis 메시지 리스너 컨테이너
     *
     * Redis Pub/Sub에서 메시지를 수신하는 컨테이너
     * connectionFactory를 이용하여 Redis와의 연결을 유지하면서 메시지를 수신
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter(null), new ChannelTopic("chatroom-*")); // 채널 패턴 추가
        log.info("✅ MessageListenerContainer 생성 완료! Active: {}", container.isRunning());
        return container;
    }

    /**
     *Redis 메시지 리스너 어댑터
     *
     * Redis로부터 메시지를 수신했을 때 호출될 메서드를 설정하는 역할
     * redisSubscriber가 Redis에서 전달받은 메시지를 처리하도록 onMessage 메서드에 바인딩
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisSubscriber redisSubscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(redisSubscriber, "onMessage");
        log.info("✅ MessageListenerAdapter 생성 완료! Delegate: {}", redisSubscriber.getClass().getName());
        return adapter;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}




