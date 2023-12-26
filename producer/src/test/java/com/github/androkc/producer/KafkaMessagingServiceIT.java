package com.github.androkc.producer;

import com.github.androkc.producer.service.messaging.event.OrderSendEvent;
import com.github.androkc.producer.service.messaging.service.KafkaMessagingService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = "send-order-event")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
public class KafkaMessagingServiceIT {
    private final KafkaMessagingService kafkaMessagingService;
    public static final String TOPIC_NAME_SEND_CLIENT = "send-order-event";
    private final EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void checkKafkaMessagingService() {
        OrderSendEvent orderSendEvent = OrderSendEvent.builder()
                .productName("phone")
                .barCode("0003432")
                .price(new BigDecimal(100000))
                .quantity(233)
                .build();
        kafkaMessagingService.sendOrder(orderSendEvent);

        Map<String, Object> consumerProps = KafkaTestUtils
                .consumerProps("group-java-test", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, OrderSendEvent> consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                new StringDeserializer(), new JsonDeserializer<>(OrderSendEvent.class)).createConsumer();
        consumer.subscribe(Collections.singletonList(TOPIC_NAME_SEND_CLIENT));

        ConsumerRecords<String, OrderSendEvent> consumerRecords = KafkaTestUtils.getRecords(consumer);
        consumer.close();


        assertEquals(1, consumerRecords.count());
        assertEquals(orderSendEvent.getProductName(), consumerRecords.iterator().next().value().getProductName());
        assertEquals(orderSendEvent.getBarCode(), consumerRecords.iterator().next().value().getBarCode());
        assertEquals(orderSendEvent.getQuantity(), consumerRecords.iterator().next().value().getQuantity());
        assertEquals(orderSendEvent.getPrice(), consumerRecords.iterator().next().value().getPrice());

    }
}
