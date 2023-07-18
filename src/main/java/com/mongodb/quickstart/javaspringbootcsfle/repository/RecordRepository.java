package com.mongodb.quickstart.javaspringbootcsfle.repository;

import com.mongodb.quickstart.javaspringbootcsfle.model.Record;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<Record, String> {
}
