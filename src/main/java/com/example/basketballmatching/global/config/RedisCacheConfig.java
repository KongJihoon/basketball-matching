package com.example.basketballmatching.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {


        ObjectMapper cacheMapper = new ObjectMapper();

        cacheMapper.registerModule(new JavaTimeModule());
        cacheMapper.findAndRegisterModules();
        cacheMapper.activateDefaultTyping(
                cacheMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );


        var key = RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer());

        var value = RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(key)
                .serializeValuesWith(value)
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(3));

        Map<String, RedisCacheConfiguration> cacheConfig = new HashMap<>();

        cacheConfig.put("gameSearch", defaultConfig.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfig)
                .build();


    }



}
