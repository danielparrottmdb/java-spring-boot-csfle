package com.mongodb.quickstart.javaspringbootcsfle;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import com.mongodb.quickstart.javaspringbootcsfle.services.KeyGenerationService;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.PropertyValueConverterFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.MongoConverterConfigurationAdapter;
import org.springframework.data.mongodb.core.convert.encryption.MongoEncryptionConverter;
import org.springframework.data.mongodb.core.encryption.Encryption;
import org.springframework.data.mongodb.core.encryption.EncryptionKeyResolver;
import org.springframework.data.mongodb.core.encryption.MongoClientEncryption;

import java.util.*;

import static com.mongodb.quickstart.javaspringbootcsfle.constants.DBStrings.KEY_VAULT_COLL;
import static com.mongodb.quickstart.javaspringbootcsfle.constants.DBStrings.KEY_VAULT_DB;

@SpringBootApplication
public class JavaSpringBootCSFLEApplication  extends AbstractMongoClientConfiguration {

    @Autowired
    ApplicationContext appContext;

    private static final String KEY_VAULT_NAMESPACE = KEY_VAULT_DB + "." + KEY_VAULT_COLL;
    @Value("${spring.data.mongodb.uri}")
    private String connectionString;
    @Value("${spring.data.mongodb.database}")
    private String DATABASE;
    @Value("${spring.data.mongodb.collection}")
    private String COLLECTION;
    @Value("${crypt.shared.lib.path}")
    private String CRYPT_SHARED_LIB_PATH;
    private final KeyGenerationService keyGenerationService;

    public JavaSpringBootCSFLEApplication(KeyGenerationService keyGenerationService) {
        this.keyGenerationService = keyGenerationService;
    }

    public static void main(String[] args) {
        SpringApplication.run(JavaSpringBootCSFLEApplication.class, args);
    }

    @Override
    public String getDatabaseName() {
        return this.DATABASE;
    }

    @Bean
    public MongoClient mongoClient() {

//        String dekId = "<paste-base-64-encoded-data-encryption-key-id>>";

        System.out.println("==== HERE - with kAN ====\n\n");
        final Map<String, Map<String, Object>> kmsProviders = keyGenerationService.getKmsProviders();

        // Unused locally but ensures the second-data-key used for explicit encryption exists 
        keyGenerationService.generateLocalKeyId(KEY_VAULT_NAMESPACE, kmsProviders, connectionString, "second-data-key");

        final String localDEK = keyGenerationService.generateLocalKeyId(KEY_VAULT_NAMESPACE, kmsProviders, connectionString);

        //String localDEK = "<<Paste ur dek id here>>";

        final Map<String, BsonDocument> schemaMap = generateSchemaMap(localDEK);

        Map<String, Object> extraOptions = new HashMap<String, Object>();
        // extraOptions.put("cryptSharedLibPath", CRYPT_SHARED_LIB_PATH);
        // extraOptions.put("cryptSharedLibRequired", true);
        extraOptions.put("mongocryptdBypassSpawn", true);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .autoEncryptionSettings(AutoEncryptionSettings.builder()
                        .keyVaultNamespace(KEY_VAULT_NAMESPACE)
                        .kmsProviders(kmsProviders)
                        .schemaMap(schemaMap)
                        .extraOptions(extraOptions)
                        .build())
                .build();

        MongoClient client = MongoClients.create(clientSettings);

        // Document p = new Document().append("firstName", "Elmer").append("lastName", "Fudd").append("aadharNumber", "54321");
        // client.getDatabase(this.getDatabaseName()).getCollection(COLLECTION).insertOne(p);
        return client;
    }

    private Map<String, BsonDocument> generateSchemaMap(final String DEK_ID) {

        Document jsonSchema = new Document().append("bsonType", "object").append("encryptMetadata",
                        new Document().append("keyId", new ArrayList<>((Collections.singletonList(new Document().append("$binary", new Document()
                                .append("base64", DEK_ID)
                                .append("subType", "04")))))))
                .append("properties", new Document()
                        .append("aadharNumber", new Document().append("encrypt", new Document()
                                .append("bsonType", "string")
                                .append("algorithm", "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"))));

        Map<String, BsonDocument> schemaMap = new HashMap<String, BsonDocument>();
        schemaMap.put(DATABASE + "." + COLLECTION, BsonDocument.parse(jsonSchema.toJson()));

        return schemaMap;
    }

    @Bean
    ClientEncryption clientEncryption() {
        ClientEncryptionSettings encryptionSettings = ClientEncryptionSettings.builder()
            .keyVaultNamespace(KEY_VAULT_DB + "." + KEY_VAULT_COLL)
            .kmsProviders(keyGenerationService.getKmsProviders())
            .keyVaultMongoClientSettings(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
            )
            .build();
        
        return ClientEncryptions.create(encryptionSettings);    
    }

    @Bean
    MongoEncryptionConverter mongoEncrpytionConverter(ClientEncryption clientEncryption) {
        Encryption<BsonValue, BsonBinary> encryption = MongoClientEncryption.just(clientEncryption);
        EncryptionKeyResolver keyResolver = EncryptionKeyResolver.annotated((ctx) -> null);             

        return new MongoEncryptionConverter(encryption, keyResolver);  
    }

    /*
     * 
     */
    @Override
    protected void configureConverters(MongoConverterConfigurationAdapter adapter) {
        adapter.registerPropertyValueConverterFactory(
            PropertyValueConverterFactory.beanFactoryAware(appContext)
        );
    }
}
