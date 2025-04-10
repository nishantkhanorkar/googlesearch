import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TransactionConverter {

    static {
        // Register all required conversions once at startup
        SpecificData.get().addLogicalTypeConversion(new Conversions.DecimalConversion());
        SpecificData.get().addLogicalTypeConversion(new Conversions.TimestampMicrosConversion());
    }

    public static Transaction convertRecord(GenericRecord genericRecord) {
        try {
            // Create a working copy of the record
            GenericRecord recordCopy = new GenericData.Record(genericRecord.getSchema());
            
            // Copy all fields with type conversions
            for (org.apache.avro.Schema.Field field : genericRecord.getSchema().getFields()) {
                Object value = genericRecord.get(field.name());
                
                // Handle timestamp field (convert Instant to microseconds if needed)
                if ("transactionTime".equals(field.name()) && value != null) {
                    if (value instanceof Instant) {
                        value = ((Instant) value).toEpochMilli() * 1000; // Convert to microseconds
                    }
                }
                // Handle amount field
                else if ("amount".equals(field.name()) && value != null) {
                    value = convertAmountRecord((GenericRecord) value);
                }
                
                recordCopy.put(field.name(), value);
            }

            // Perform the final conversion
            return (Transaction) SpecificData.get().deepCopy(
                Transaction.getClassSchema(),
                recordCopy
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert transaction record", e);
        }
    }

    private static Object convertAmountRecord(GenericRecord amountRecord) {
        try {
            // Create a copy of the amount record
            GenericRecord amountCopy = new GenericData.Record(amountRecord.getSchema());
            
            // Convert decimal value if needed
            Object value = amountRecord.get("value");
            if (value instanceof ByteBuffer) {
                value = new Conversions.DecimalConversion().fromBytes(
                    (ByteBuffer) value,
                    amountRecord.getSchema().getField("value").schema(),
                    LogicalTypes.decimal(10, 2)
                );
            }
            
            // Copy all amount fields
            amountCopy.put("value", value);
            amountCopy.put("currency", amountRecord.get("currency"));
            
            return amountCopy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert amount record", e);
        }
    }

    // Helper method to convert timestamp long to readable format
    public static LocalDateTime convertTransactionTime(Transaction transaction) {
        long micros = transaction.getTransactionTime();
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(micros / 1000), 
            ZoneId.systemDefault()
        );
    }
}
