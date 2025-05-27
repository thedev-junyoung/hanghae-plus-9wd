package kr.hhplus.be.server.redis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@Slf4j
@Tag("benchmark") // 👈 태그 추가
class RedisRankingComparisonTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String STREAM_KEY = "stream:order:benchmark";
    private static final String ZSET_DIRECT_KEY = "ranking:direct:benchmark";
    private static final String ZSET_STREAM_KEY = "ranking:stream:benchmark";
    private static final String ZSET_BATCH_KEY = "ranking:batch:benchmark";

    private static final int MESSAGE_COUNT = 1_000_000;
    private static final int CONSUMER_COUNT = 4;
    private static final int BATCH_SIZE = 100;

    @Test
    void 비교_테스트_단순_ZINCRBY_vs_Stream_비동기_배치() throws InterruptedException {
        redisTemplate.delete(STREAM_KEY);
        redisTemplate.delete(ZSET_DIRECT_KEY);
        redisTemplate.delete(ZSET_STREAM_KEY);
        redisTemplate.delete(ZSET_BATCH_KEY);

        var streamOps = redisTemplate.opsForStream();
        var zSetOps = redisTemplate.opsForZSet();
        var productIds = List.of("p1", "p2", "p3", "p4", "p5");
        Random random = new Random();

        // 1. 단순 ZINCRBY
        long startDirect = System.currentTimeMillis();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String productId = productIds.get(random.nextInt(productIds.size()));
            zSetOps.incrementScore(ZSET_DIRECT_KEY, productId, 1);
        }
        long endDirect = System.currentTimeMillis();
        log.info("단순 ZINCRBY 처리 시간: {} ms", (endDirect - startDirect));
        log.info("초당 처리량: {} ops/sec", MESSAGE_COUNT / ((endDirect - startDirect) / 1000.0));
        log.info("--------------------------------");
        // 2. Stream 적재
        random = new Random();
        long streamWriteStart = System.currentTimeMillis();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String productId = productIds.get(random.nextInt(productIds.size()));
            streamOps.add(MapRecord.create(STREAM_KEY, Map.of("productId", productId)));
        }
        long streamWriteEnd = System.currentTimeMillis();
        log.info("Stream XADD 처리 시간: {} ms", (streamWriteEnd - streamWriteStart));

        // 3. 병렬 Consumer (Stream 단건 처리)
        ExecutorService executor = Executors.newFixedThreadPool(CONSUMER_COUNT);
        CountDownLatch latch1 = new CountDownLatch(CONSUMER_COUNT);
        long streamConsumeStart = System.currentTimeMillis();

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<MapRecord<String, Object, Object>> records = streamOps.read(StreamOffset.fromStart(STREAM_KEY));
                    for (int j = 0; j < records.size(); j++) {
                        if (j % CONSUMER_COUNT == threadId) {
                            String productId = String.valueOf(records.get(j).getValue().get("productId"));
                            zSetOps.incrementScore(ZSET_STREAM_KEY, productId, 1);
                        }
                    }
                } finally {
                    latch1.countDown();
                }
            });
        }

        latch1.await();
        long streamConsumeEnd = System.currentTimeMillis();
        log.info("Stream 처리 시간 (Consumer): {} ms", (streamConsumeEnd - streamConsumeStart));
        log.info("초당 처리량 (XADD + Consumer): {} ops/sec",
                MESSAGE_COUNT / ((streamConsumeEnd - streamWriteStart) / 1000.0));
        log.info("--------------------------------");
        // 4. 병렬 Consumer (배치 처리)
        CountDownLatch latch2 = new CountDownLatch(CONSUMER_COUNT);
        long batchStart = System.currentTimeMillis();

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<MapRecord<String, Object, Object>> records = streamOps.read(StreamOffset.fromStart(STREAM_KEY));
                    Map<String, Integer> localBuffer = new HashMap<>();

                    for (int j = 0; j < records.size(); j++) {
                        if (j % CONSUMER_COUNT == threadId) {
                            String productId = String.valueOf(records.get(j).getValue().get("productId"));
                            localBuffer.merge(productId, 1, Integer::sum);

                            if (localBuffer.size() >= BATCH_SIZE) {
                                localBuffer.forEach((k, v) -> zSetOps.incrementScore(ZSET_BATCH_KEY, k, v));
                                localBuffer.clear();
                            }
                        }
                    }
                    // 마지막 flush
                    localBuffer.forEach((k, v) -> zSetOps.incrementScore(ZSET_BATCH_KEY, k, v));
                } finally {
                    latch2.countDown();
                }
            });
        }

        latch2.await();
        long batchEnd = System.currentTimeMillis();
        log.info("Stream-Batch 처리 시간: {} ms", (batchEnd - batchStart));
        log.info("초당 처리량 (XADD + Batch Consumer): {} ops/sec",
                MESSAGE_COUNT / ((batchEnd - streamWriteStart) / 1000.0));

        executor.shutdown();
    }
}
