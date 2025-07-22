package com.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CACHE_KEY = "ai:cache:lfu";
    private static final int MAX_CAPACITY = 1000;

    // 获取缓存（LFU策略：访问次数+1）
    public String get(String question) {
        String answer = redisTemplate.opsForValue().get(buildKey(question));
        if (answer != null) {
            redisTemplate.opsForZSet().incrementScore(CACHE_KEY, question, 1);
        }
        return answer;
    }

    // 存入缓存（超容量则淘汰访问量最低的）
    public void put(String question, String answer) {
        redisTemplate.opsForValue().set(buildKey(question), answer, 24, TimeUnit.HOURS);
        redisTemplate.opsForZSet().add(CACHE_KEY, question, 1);

        if (redisTemplate.opsForZSet().size(CACHE_KEY) > MAX_CAPACITY) {
            Set<String> leastUsed = redisTemplate.opsForZSet().range(CACHE_KEY, 0, 0);
            String lfuKey = leastUsed.iterator().next();
            redisTemplate.delete(buildKey(lfuKey));
            redisTemplate.opsForZSet().remove(CACHE_KEY, lfuKey);
        }
    }

    private String buildKey(String question) {
        return "ai:cache:q:" + question;
    }
}