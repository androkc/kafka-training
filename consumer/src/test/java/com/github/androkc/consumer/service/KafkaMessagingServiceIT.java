package com.github.androkc.consumer.service;

import com.github.androkc.consumer.domain.Order;
import com.github.androkc.consumer.domain.Status;
import com.github.androkc.consumer.domain.repository.OrderRepository;
import com.github.androkc.consumer.service.messaging.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.*;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Testcontainers
@RequiredArgsConstructor
@TestExecutionListeners(TransactionalTestExecutionListener.class)
public class KafkaMessagingServiceIT {
    public static final Long ORDER_ID = 1L;
    public static final String TOPIC_NAME_SEND_ORDER = "send-order-event";

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .withUsername("username")
            .withPassword("password")
            .withExposedPorts(5432)
            .withReuse(true);
    ;
    @Container
    static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.4"))
                    .withEmbeddedZookeeper()
                    .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9093 ,BROKER://0.0.0.0:9092")
                    .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                    .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                    .withEnv("KAFKA_BROKER_ID", "1")
                    .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                    .withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "")
                    .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");


    static {
        Startables.deepStart(Stream.of(postgreSQLContainer, kafkaContainer)).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
    }

    private final OrderRepository ordersRepository;

    @Test
    void checkSave() throws InterruptedException {
        //given
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        OrderEvent orderEvent = OrderEvent.builder()
                .productName("elephant")
                .barCode("020303")
                .price(new BigDecimal("0.99"))
                .quantity(100)
                .build();

        Order order = Order.builder()
                .id(1L)
                .productName("elephant")
                .barCode("020303")
                .quantity(100)
                .price(new BigDecimal("0.99"))
                .orderDate(LocalDateTime.of(2023, 12, 26, 9, 5, 0))
                .amount(new BigDecimal("99.0"))
                .status(Status.APPROVED)
                .build();

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, OrderEvent> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        KafkaTemplate<String, OrderEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        //when

        SECONDS.sleep(5);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, orderEvent.getBarCode(), orderEvent);
        SECONDS.sleep(5);

        //then
        Order orderFromDB = ordersRepository.findById(ORDER_ID).get();
        System.out.println(orderFromDB);
        assertEquals(orderFromDB.getId(), ORDER_ID);
        assertEquals(orderFromDB.getProductName(), order.getProductName());
        assertEquals(orderFromDB.getBarCode(), order.getBarCode());
        assertEquals(orderFromDB.getQuantity(), order.getQuantity());
        assertEquals(orderFromDB.getPrice(), order.getPrice().setScale(2, RoundingMode.HALF_DOWN));
        assertEquals(orderFromDB.getAmount(), order.getAmount().setScale(2));
        assertEquals(orderFromDB.getOrderDate().getYear(), order.getOrderDate().getYear());
        assertEquals(orderFromDB.getStatus(), order.getStatus());
    }
}
