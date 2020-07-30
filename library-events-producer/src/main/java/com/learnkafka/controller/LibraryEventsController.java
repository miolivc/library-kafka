package com.learnkafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import com.learnkafka.producer.LibraryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class LibraryEventsController {

    @Autowired
    private LibraryEventProducer libraryEventProducer;

    @PostMapping("/v1/libraryevent")
    public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent)
            throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

        libraryEvent.setLibraryEventType(LibraryEventType.NEW);

        log.info("before sendLibraryEveny");
//        libraryEventProducer.sendLibraryEvent(libraryEvent);
        libraryEventProducer.sendLibraryEventTopic(libraryEvent);
//        SendResult<Integer, String> result = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);
//        log.info("result is {}", result.toString());
        log.info("after sendLibraryEveny");

        return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
    }

}
