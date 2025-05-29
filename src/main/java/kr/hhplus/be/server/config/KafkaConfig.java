package kr.hhplus.be.server.config;

import kr.hhplus.be.server.domain.orderexport.OrderExportPayload;
import kr.hhplus.be.server.infrastructure.kafka.CouponIssueKafkaMessage;
import kr.hhplus.be.server.infrastructure.kafka.StockDecreaseFailedKafkaMessage;
import kr.hhplus.be.server.infrastructure.kafka.StockDecreaseRequestedKafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("!test")
public class KafkaConfig {


    @Bean
    public KafkaTemplate<Object, Object> defaultKafkaTemplate(ProducerFactory<Object, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
    @Bean
    public ProducerFactory<Object, Object> defaultProducerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildProducerFactory(props, JsonSerializer.class);
    }

    // === Default String Producer and Consumer ===
    @Bean
    public ProducerFactory<String, String> stringProducerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildProducerFactory(props, StringSerializer.class);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(
            ProducerFactory<String, String> factory
    ) {
        return KafkaFactoryBuilder.buildKafkaTemplate(factory);
    }

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildConsumerFactory(props, StringDeserializer.class, String.class);
    }

    @Bean(name = "stringKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }


    // === Order Export Payload Producer and Consumer ===
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
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    // === StockDecreaseRequestedKafkaMessage Producer and Consumer ===

    @Bean
    public ProducerFactory<String, StockDecreaseRequestedKafkaMessage> stockDecreaseProducerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildProducerFactory(props, JsonSerializer.class);
    }

    @Bean
    public KafkaTemplate<String, StockDecreaseRequestedKafkaMessage> stockDecreaseKafkaTemplate(
            ProducerFactory<String, StockDecreaseRequestedKafkaMessage> factory
    ) {
        return KafkaFactoryBuilder.buildKafkaTemplate(factory);
    }

    @Bean
    public ConsumerFactory<String, StockDecreaseRequestedKafkaMessage> stockDecreaseConsumerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildConsumerFactory(
                props,
                JsonDeserializer.class,
                StockDecreaseRequestedKafkaMessage.class
        );
    }

    @Bean(name = "stockDecreaseKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, StockDecreaseRequestedKafkaMessage> stockDecreaseKafkaListenerFactory(
            ConsumerFactory<String, StockDecreaseRequestedKafkaMessage> consumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, StockDecreaseRequestedKafkaMessage>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    // === StockDecreaseFailedKafkaMessage Producer and Consumer ===

    @Bean
    public ProducerFactory<String, StockDecreaseFailedKafkaMessage> stockDecreaseFailedProducerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildProducerFactory(props, JsonSerializer.class);
    }

    @Bean
    public KafkaTemplate<String, StockDecreaseFailedKafkaMessage> stockDecreaseFailedKafkaTemplate(
            ProducerFactory<String, StockDecreaseFailedKafkaMessage> factory
    ) {
        return KafkaFactoryBuilder.buildKafkaTemplate(factory);
    }

    @Bean
    public ConsumerFactory<String, StockDecreaseFailedKafkaMessage> stockDecreaseFailedConsumerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildConsumerFactory(
                props,
                JsonDeserializer.class,
                StockDecreaseFailedKafkaMessage.class
        );
    }
    @Bean(name = "stockDecreaseFailedKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, StockDecreaseFailedKafkaMessage> stockDecreaseFailedKafkaListenerFactory(
            ConsumerFactory<String, StockDecreaseFailedKafkaMessage> consumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, StockDecreaseFailedKafkaMessage>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        return factory;
    }


    // === CouponIssueKafkaMessage Producer and Consumer ===
    @Bean
    public ProducerFactory<String, CouponIssueKafkaMessage> couponProducerFactory(KafkaProperties props) {
        return KafkaFactoryBuilder.buildProducerFactory(props, JsonSerializer.class);
    }

    @Bean
    public KafkaTemplate<String, CouponIssueKafkaMessage> couponKafkaTemplate(
            ProducerFactory<String, CouponIssueKafkaMessage> factory
    ) {
        return KafkaFactoryBuilder.buildKafkaTemplate(factory);
    }

    @Bean
    public ConsumerFactory<String, CouponIssueKafkaMessage> couponConsumerFactory(KafkaProperties props) {
        Map<String, Object> consumerProps = new HashMap<>(props.buildConsumerProperties());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumerProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CouponIssueKafkaMessage.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>())
        );
    }

    @Bean(name = "couponKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, CouponIssueKafkaMessage> couponKafkaListenerFactory(
            ConsumerFactory<String, CouponIssueKafkaMessage> consumerFactory,
            DefaultErrorHandler couponErrorHandler // 주입
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, CouponIssueKafkaMessage>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(couponErrorHandler); // 등록
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }



    // === Common Error Handler ===
    @Bean
    public CommonErrorHandler commonErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3));
    }
    @Bean
    public DefaultErrorHandler couponErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }

    // Dead Letter Queue 설정
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<Object, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    }


}
