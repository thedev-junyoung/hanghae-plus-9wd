package kr.hhplus.be.server.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

public class KafkaFactoryBuilder {

    public static <K, T> ProducerFactory<K, T> buildProducerFactory(
            KafkaProperties props,
            Class<?> valueSerializerClass
    ) {
        Map<String, Object> config = new HashMap<>(props.buildProducerProperties());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        if (valueSerializerClass == JsonSerializer.class) {
            config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        }

        return new DefaultKafkaProducerFactory<>(config);
    }

    public static <T> ConsumerFactory<String, T> buildConsumerFactory(
            KafkaProperties props,
            Class<?> valueDeserializerClass,
            Class<T> targetType
    ) {
        Map<String, Object> config = new HashMap<>(props.buildConsumerProperties());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);

        if (valueDeserializerClass == JsonDeserializer.class) {
            config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
            config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
            config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
            return new DefaultKafkaConsumerFactory<>(
                    config,
                    new StringDeserializer(),
                    new JsonDeserializer<>(targetType, false)
            );
        }

        return new DefaultKafkaConsumerFactory<>(config);
    }

    public static <T> KafkaTemplate<String, T> buildKafkaTemplate(
            ProducerFactory<String, T> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
