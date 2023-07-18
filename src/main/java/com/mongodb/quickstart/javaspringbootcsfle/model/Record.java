package com.mongodb.quickstart.javaspringbootcsfle.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.EncryptionAlgorithms;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.ExplicitEncrypted;

@Document("records")
public class Record {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Field
    private String label;

    @Field
    @ExplicitEncrypted(algorithm = EncryptionAlgorithms.AEAD_AES_256_CBC_HMAC_SHA_512_Deterministic, keyAltName = "second-data-key")
    private String contents;

    public Record() {
    }

    public Record(String label, String contents) {
        this.label = label;
        this.contents = contents;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}
