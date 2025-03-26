import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class AvroToPojoToJsonConsumer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-pojo-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("your-avro-topic"));

        // Configure Jackson for logical types
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        try {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        // Deserialize directly to POJO
                        AccountTransactionEntity transaction = deserializeAvro(record.value());
                        
                        // Convert to JSON
                        String json = objectMapper.writeValueAsString(transaction);
                        System.out.println("Converted JSON: " + json);
                    } catch (Exception e) {
                        System.err.println("Error processing record: " + e.getMessage());
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }

    private static AccountTransactionEntity deserializeAvro(byte[] data) throws Exception {
        // Using SpecificDatumReader with your POJO class
        SpecificDatumReader<AccountTransactionEntity> reader = new SpecificDatumReader<>(AccountTransactionEntity.class);
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }
}
