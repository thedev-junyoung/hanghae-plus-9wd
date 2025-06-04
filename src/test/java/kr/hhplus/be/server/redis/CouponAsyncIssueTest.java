package kr.hhplus.be.server.redis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@SpringBootTest
@Slf4j
@Profile("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"coupon.issue", "coupon.issue.DLT"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
class CouponAsyncIssueTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SET_KEY = "coupon:users";  // 중복 방지
    private static final String RANK_KEY = "coupon:rank";  // 순서 보장
    private static final String STREAM_KEY = "coupon:stream";  // 이벤트 큐
    private static final int MAX_ISSUE = 100;  // 정확히 100명만
    private static final int REQUESTS = 1_000_000;


    @Test
    void 선착순_쿠폰_발급_비동기_테스트() throws InterruptedException {
        redisTemplate.delete(SET_KEY);
        redisTemplate.delete(RANK_KEY);
        redisTemplate.delete(STREAM_KEY);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(REQUESTS);

        long start = System.currentTimeMillis();

        IntStream.range(0, REQUESTS).forEach(i -> executor.submit(() -> {
            try {
                String userId = "user_" + i;

                // Step 1: 발급 제한 체크
                Long current = redisTemplate.opsForZSet().zCard(RANK_KEY);
                if (current != null && current >= MAX_ISSUE) return;

                // Step 2: 순위 기록 (ZSET)
                Double score = (double) System.nanoTime();
                Boolean zAdded = redisTemplate.opsForZSet().add(RANK_KEY, userId, score);
                if (Boolean.FALSE.equals(zAdded)) return;

                // Step 3: 중복 방지 (SET)
                Boolean added = redisTemplate.opsForSet().add(SET_KEY, userId) == 1;
                if (!added) return;

                // Step 4: 이벤트 저장 (STREAM)
                redisTemplate.opsForStream().add(
                        MapRecord.create(STREAM_KEY, Map.of("userId", userId))
                );

            } finally {
                latch.countDown();
            }
        }));


        latch.await();
        long end = System.currentTimeMillis();

        log.info("전체 요청 완료 시간: {} ms", end - start);
        log.info("쿠폰 발급 수 (Set size): {}", redisTemplate.opsForSet().size(SET_KEY));
        log.info("쿠폰 랭킹 수 (ZSet size): {}", redisTemplate.opsForZSet().zCard(RANK_KEY));
        log.info("이벤트 기록 수 (Stream): {}", redisTemplate.opsForStream().size(String.valueOf(StreamOffset.fromStart(STREAM_KEY))));

        executor.shutdown();
    }
}