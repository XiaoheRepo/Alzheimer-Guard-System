package com.xiaohelab.guard.server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class AppConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, ObjectMapper mapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        StringRedisSerializer keySer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valSer = new GenericJackson2JsonRedisSerializer(mapper);
        template.setKeySerializer(keySer);
        template.setHashKeySerializer(keySer);
        template.setValueSerializer(valSer);
        template.setHashValueSerializer(valSer);
        template.afterPropertiesSet();
        return template;
    }
}
