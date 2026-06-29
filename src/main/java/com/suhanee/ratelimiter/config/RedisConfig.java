package com.suhanee.ratelimiter.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnesctionFactory(){
    return new LettuceConnectionFactory();
}

@Bean
public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory){
    return new StringRedisTemplate(connectionFactory);
}

@Bean
public DefaultRedisScript<List> tokenBucketScript(){
    DefaultRedisScript<List> script=new DefaultRedisScript<>();
    script.setLocation(new org.springframework.core.io.ClassPathResource("scripts/token_bucket.lua"));
    script.setResultType(List.class);
    return script;
}
}

