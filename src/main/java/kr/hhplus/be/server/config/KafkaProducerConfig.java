package kr.hhplus.be.server.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // Kafka 브로커 주소 설정
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 키 직렬화기 설정
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // 값 직렬화기 설정
        // 필요시 추가: acks, retries, batch.size 등

        config.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 복제본에 전송 확인을 기다림
        config.put(ProducerConfig.RETRIES_CONFIG, 3); // 재시도 횟수 설정
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 재시도 간격 설정 (1초)
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 배치 크기 설정 (16KB)
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 배치 전송 지연 시간
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4"); // 압축 설정


        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
