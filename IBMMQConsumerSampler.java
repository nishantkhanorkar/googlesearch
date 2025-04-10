public static Transaction convertToTransaction(GenericRecord genericRecord) {
        try {
            // Create a copy of the record to avoid modifying the original
            GenericRecord recordCopy = new GenericData.Record(genericRecord.getSchema());
            genericRecord.getSchema().getFields().forEach(field -> {
                Object value = genericRecord.get(field.name());
                
                // Convert Instant to Long (microseconds) if needed
                if (value instanceof Instant && "transactionTime".equals(field.name())) {
                    Instant instant = (Instant) value;
                    value = instant.toEpochMilli() * 1000; // Convert to microseconds
                }
                
                recordCopy.put(field.name(), value);
            });

            // Perform the conversion
            return (Transaction) SpecificData.get().deepCopy(
                Transaction.getClassSchema(),
                recordCopy
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert GenericRecord to Transaction", e);
        }
    }






SpecificDatumReader<AccountTransactionEntity> reader = new SpecificDatumReader<>(
    AccountTransactionEntity.getClassSchema());
Decoder decoder = DecoderFactory.get().binaryDecoder(
    record.value(), null);
AccountTransactionEntity transaction = reader.read(null, decoder);



import io.confluent.kafka.serializers.*;
import org.apache.avro.*;
import org.apache.avro.generic.*;
import org.apache.avro.specific.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.*;
import org.apache.kafka.common.serialization.*;
import java.io.*;
import java.nio.*;
import java.time.*;
import java.util.*;

public class AvroSpecificConsumer {

    private static final String TOPIC = "account-transactions";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";

    public static void main(String[] args) {
        // 1. Configure Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // 2. Create Consumer and Subscribe
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));

        // 3. Schema Registry Client
        CachedSchemaRegistryClient schemaRegistry = new CachedSchemaRegistryClient(
            SCHEMA_REGISTRY_URL, 
            100
        );

        // 4. Main Poll Loop
        try {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        // 5. Deserialize Avro Record
                        AccountTransactionEntity transaction = deserializeAvroRecord(
                            record.value(), 
                            schemaRegistry
                        );

                        // 6. Process Message
                        System.out.printf("Success: offset=%d, key=%s, value=%s%n",
                            record.offset(),
                            record.key(),
                            transaction);

                        // 7. Manual Offset Commit
                        consumer.commitSync(Collections.singletonMap(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)));
                    } catch (Exception e) {
                        handleDeserializationError(record, e);
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }

    private static AccountTransactionEntity deserializeAvroRecord(
        byte[] data, 
        SchemaRegistryClient schemaRegistry
    ) throws IOException {
        // 1. Check for Confluent Wire Format
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.get() != 0x0) {
            throw new SerializationException("Not Confluent Avro wire format");
        }

        // 2. Extract Schema ID
        int schemaId = buffer.getInt();
        Schema writerSchema = schemaRegistry.getByID(schemaId);

        // 3. Get Reader Schema
        Schema readerSchema = AccountTransactionEntity.getClassSchema();

        // 4. Prepare Avro Components
        byte[] avroPayload = new byte[buffer.remaining()];
        buffer.get(avroPayload);

        SpecificDatumReader<AccountTransactionEntity> reader = new SpecificDatumReader<>(
            writerSchema,
            readerSchema
        );

        Decoder decoder = DecoderFactory.get().binaryDecoder(avroPayload, null);
        return reader.read(null, decoder);
    }

    private static void handleDeserializationError(
        ConsumerRecord<String, byte[]> record,
        Exception e
    ) {
        System.err.printf("Failed to process record at offset %d: %s%n",
            record.offset(), e.getMessage());

        // Log raw message for debugging
        System.err.println("Raw data (hex): " + bytesToHex(record.value()));

        // Optionally send to dead letter queue
        // deadLetterProducer.send(new ProducerRecord<>("dead-letters", record.key(), record.value()));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }
}




import org.apache.avro.*;
import org.apache.avro.data.*;
import org.apache.avro.generic.*;
import org.apache.avro.io.*;
import org.apache.avro.specific.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.*;
import org.joda.time.*;
import java.time.*;
import java.nio.*;
import java.util.*;

public class AvroLogicalTypeConsumer {

    private static final String TOPIC = "account-transactions";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";

    public static void main(String[] args) {
        // 1. Configure Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "logical-type-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 2. Create and subscribe consumer
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            // 3. Register logical type conversions
            GenericData genericData = new GenericData();
            genericData.addLogicalTypeConversion(new TimeConversions.DateConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.TimeMillisConversion());

            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        // 4. Deserialize with logical type support
                        AccountTransactionEntity transaction = deserializeWithLogicalTypes(
                            record.value(), 
                            genericData
                        );

                        // 5. Process temporal fields
                        processTemporalFields(transaction);

                        System.out.printf("Processed: offset=%d, date=%s, timestamp=%s\n",
                            record.offset(),
                            transaction.getTransactionDate(),
                            transaction.getTransactionTime());
                    } catch (Exception e) {
                        System.err.printf("Failed to process offset %d: %s\n",
                            record.offset(), e.getMessage());
                    }
                }
            }
        }
    }

    private static AccountTransactionEntity deserializeWithLogicalTypes(
        byte[] data, 
        GenericData genericData
    ) throws Exception {
        // Handle Confluent wire format
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.get() == 0x0) { // Confluent magic byte
            buffer.getInt(); // Skip schema ID
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);
            data = payload;
        }

        SpecificDatumReader<AccountTransactionEntity> reader = new SpecificDatumReader<>(
            AccountTransactionEntity.getClassSchema());
        reader.setData(genericData); // Inject logical type support

        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }

    private static void processTemporalFields(AccountTransactionEntity transaction) {
        // Convert Joda to Java 8 time (recommended)
        LocalDate transactionDate = transaction.getTransactionDate().toLocalDate();
        Instant transactionTime = Instant.ofEpochMilli(
            transaction.getTransactionTime().getMillis()
        );

        // Use in business logic
        Duration age = Duration.between(
            transactionTime,
            Instant.now()
        );
        
        System.out.printf("Transaction age: %d days\n", age.toDays());
    }
}













