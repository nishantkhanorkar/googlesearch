public class AvroConsumer {
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-consumer-v2");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("your-avro-topic"));

            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        AccountTransactionEntity transaction = safeDeserialize(record.value());
                        String json = objectMapper.writeValueAsString(transaction);
                        System.out.println("Success: " + json);
                    } catch (Exception e) {
                        System.err.printf("Error processing record at offset %d: %s%n",
                            record.offset(), e.getMessage());
                        // Add hex dump of problematic record
                        System.err.println("Record data (hex): " + bytesToHex(record.value()));
                    }
                }
            }
        }
    }

    private static AccountTransactionEntity safeDeserialize(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Empty data payload");
        }

        try {
            // Try standard Avro first
            return deserializeAvro(data);
        } catch (Exception e1) {
            try {
                // Fallback to Confluent format if standard fails
                return deserializeConfluentAvro(data);
            } catch (Exception e2) {
                throw new IllegalArgumentException(
                    String.format("Failed to deserialize as either standard Avro (%s) or Confluent Avro (%s)",
                        e1.getMessage(), e2.getMessage()));
            }
        }
    }

    // Utility method for debugging
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }
}
