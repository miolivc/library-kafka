package com.learnkafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import com.learnkafka.repository.LibraryEventsRepository;
import com.learnkafka.service.LibraryEventsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class LibraryEventsConsumerTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @SpyBean
    private LibraryEventsConsumer libraryEventsConsumerSpy;

    @SpyBean
    private LibraryEventsService libraryEventsServiceSpy;

    @Autowired
    private LibraryEventsRepository libraryEventsRepository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        for (var messageListenerContainer : endpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
    }

    @Test
    void publishNewLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        var json = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":123,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        kafkaTemplate.sendDefault(json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        List<LibraryEvent> libraryEventList = (List<LibraryEvent>) libraryEventsRepository.findAll();
        assert libraryEventList.size() == 1;
        libraryEventList.forEach(libraryEvent -> {
            assert libraryEvent.getLibraryEventId() != null;
            assertEquals(libraryEvent.getLibraryEventId(), 456);
        });
    }

    @Test
    void publishUpdateLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        var json = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":123,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        var libraryEvent = mapper.readValue(json, LibraryEvent.class);
        libraryEventsRepository.save(libraryEvent);

        var updatedBook = Book.builder()
                .bookId(456)
                .bookName("Kafka Using Spring Boot 2.x")
                .bookAuthor("Dilip")
                .build();
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.setBook(updatedBook);

        var updatedJson = mapper.writeValueAsString(libraryEvent);

        kafkaTemplate.sendDefault(libraryEvent.getLibraryEventId(), updatedJson).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        var persisted = libraryEventsRepository.findById(libraryEvent.getLibraryEventId()).get();
        assertEquals("Kafka Using Spring Boot 2.x", persisted.getBook().getBookName());
    }

}
