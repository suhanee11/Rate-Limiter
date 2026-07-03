package com.suhanee.ratelimiter.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.List;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnesctionFactory(){
        RedisStandaloneConfiguration config=new RedisStandaloneConfiguration(redisHost,redisPort);
        if(redisPassword!=null && !redisPassword.isEmpty()){
            config.setPassword(redisPassword);
        }
    return new LettuceConnectionFactory(config);
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

