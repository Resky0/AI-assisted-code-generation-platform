package com.resky.yuaicodemother.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisChatMemoryStoreConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .ttl(ttl);
        if (password != null && !password.isBlank()) {
            builder.user(username == null || username.isBlank() ? "default" : username)
                    .password(password);
        }
        return builder.build();
    }
}
