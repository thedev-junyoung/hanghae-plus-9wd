package kr.hhplus.be.server.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisScriptExecutor {

    private final RedisTemplate<String, Object> redisTemplate;

    public <T> T execute(String luaScript, List<String> keys, List<String> args, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(resultType);
        return redisTemplate.execute(script, keys, args.toArray());
    }
}
