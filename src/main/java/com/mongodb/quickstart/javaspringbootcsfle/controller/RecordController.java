package com.mongodb.quickstart.javaspringbootcsfle.controller;

import com.mongodb.quickstart.javaspringbootcsfle.model.Record;
import com.mongodb.quickstart.javaspringbootcsfle.repository.RecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class RecordController {

    private final RecordRepository recordRepository;

    public RecordController(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @GetMapping("/records")
    public Object getAllRecords() {
        try {
            return this.recordRepository.findAll();
        } catch (Exception exception) {
            return exception;
        }
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    public Record createRecord(@RequestBody Record record) {
        return this.recordRepository.save(record);
    }
}
