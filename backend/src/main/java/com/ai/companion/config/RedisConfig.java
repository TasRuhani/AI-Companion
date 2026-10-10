package com.ai.companion.config;

import com.ai.companion.dto.ChatMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ChatMessage> redisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, ChatMessage> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        JsonMapper mapper = JsonMapper.builder().build();

        JacksonJsonRedisSerializer<ChatMessage> serializer =
                new JacksonJsonRedisSerializer<>(mapper, ChatMessage.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }
}