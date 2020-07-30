package com.learnkafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryEventsController.class)
@AutoConfigureMockMvc
class LibraryEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibraryEventProducer libraryEventProducer;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void postLibraryEvent() throws Exception {
//        Given
        Book book = Book.builder()
                .bookId(123)
                .bookName("Kafka Using Spring Boot")
                .bookAuthor("Dilip")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .build();

        var json = mapper.writeValueAsString(libraryEvent);

//        Inject livraryProducerBean
        doNothing().when(libraryEventProducer).sendLibraryEventTopic(isA(LibraryEvent.class));

//        When
        mockMvc.perform(
                post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isCreated());
    }

    @Test
    void postLibraryEventError() throws Exception {
//        Given
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(null)
                .build();

        var json = mapper.writeValueAsString(libraryEvent);

//        Inject livraryProducerBean
        doNothing().when(libraryEventProducer).sendLibraryEventTopic(isA(LibraryEvent.class));

//        When
        var expected = "book - must not be null";
        mockMvc.perform(
                post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError())
        .andExpect(content().string(expected));
    }
}