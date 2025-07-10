import java.sql.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration
import java.util.Properties
import java.util.Arrays

// === Input from CSV ===
String rowId = vars.get("rowId")

// === Setup DB ===
String url = "jdbc:db2://hostname:port/DBNAME"
String user = "youruser"
String password = "yourpassword"
Connection conn = DriverManager.getConnection(url, user, password)

// === Setup Kafka ===
Properties props = new Properties()
props.put("bootstrap.servers", "localhost:9092")
props.put("group.id", "jmeter-group")
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
props.put("auto.offset.reset", "latest") // or earliest based on your topic

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)
consumer.subscribe(Arrays.asList("my_topic"))

// === Start timing ===
SampleResult.setSampleLabel("Row ${rowId}: DB Update + Kafka Wait")
SampleResult.sampleStart()

// === Run update ===
Statement stmt = conn.createStatement()
int rows = stmt.executeUpdate("UPDATE my_table SET status = 'processed' WHERE id = " + rowId)

// === Poll Kafka ===
long timeoutMillis = 30000 // 30s timeout
long start = System.currentTimeMillis()
boolean messageReceived = false

while ((System.currentTimeMillis() - start) < timeoutMillis) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100))
    if (!records.isEmpty()) {
        for (record in records) {
            log.info("Kafka message: " + record.value())
            if (record.value().contains("\"id\":" + rowId)) {
                messageReceived = true
                break
            }
        }
    }
    if (messageReceived) break
}

consumer.close()
stmt.close()
conn.close()

SampleResult.sampleEnd()

if (messageReceived) {
    SampleResult.setSuccessful(true)
    SampleResult.setResponseMessage("Success for ID " + rowId)
} else {
    SampleResult.setSuccessful(false)
    SampleResult.setResponseMessage("No Kafka message for ID " + rowId)
}
