package kr.hhplus.be.server.config.kafka;

import kr.hhplus.be.server.config.KafkaFactoryBuilder;
import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Profile("test")
public class TestKafkaConfig {

    @Bean
    public ProducerFactory<String, OrderExportPayload> orderExportProducerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildProducerFactory(props, JsonSerializer.class);
    }

    @Bean
    public KafkaTemplate<String, OrderExportPayload> orderExportKafkaTemplate(
            ProducerFactory<String, OrderExportPayload> factory
    ) {
        return KafkaFactoryBuilder.buildKafkaTemplate(factory);
    }

    @Bean
    public ConsumerFactory<String, OrderExportPayload> orderExportConsumerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildConsumerFactory(props, JsonDeserializer.class, OrderExportPayload.class);
    }

    @Bean(name = "orderExportKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderExportPayload> orderExportKafkaListenerFactory(
            ConsumerFactory<String, OrderExportPayload> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderExportPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler());
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public CommonErrorHandler commonErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3));
    }
}
