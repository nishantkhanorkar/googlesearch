import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DecoderFactory

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration
import java.util.Properties

import java.io.ByteArrayInputStream

// -- Load Avro schema from file --
String schemaString = new File('/path/to/your-schema.avsc').text
Schema schema = new Schema.Parser().parse(schemaString)

// -- Kafka setup --
Properties props = new Properties()
props.put("bootstrap.servers", "localhost:9092")
props.put("group.id", "jmeter-group")
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")  // note: ByteArrayDeserializer

KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)
consumer.subscribe(Arrays.asList("my_topic"))

// -- Poll --
long timeoutMillis = 30000
long start = System.currentTimeMillis()
boolean found = false

while ((System.currentTimeMillis() - start) < timeoutMillis) {
    ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100))
    if (!records.isEmpty()) {
        for (record in records) {
            byte[] value = record.value()
            // Deserialize Avro
            DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema)
            def decoder = DecoderFactory.get().binaryDecoder(value, null)
            GenericRecord avroRecord = reader.read(null, decoder)

            log.info("Decoded Avro: ${avroRecord}")

            if (avroRecord.get("id").toString() == vars.get("rowId")) {
                found = true
                break
            }
        }
    }
    if (found) break
}

consumer.close()
