import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.specific.SpecificData;

import java.io.File;
import java.io.IOException;

public class AvroToPojoConverter {

    public static void main(String[] args) {
        // 1. Define the input schema file and output directory
        String schemaFilePath = "src/main/avro/user.avsc";
        String outputDir = "src/main/java";
        
        // 2. Create output directory if it doesn't exist
        new File(outputDir).mkdirs();
        
        try {
            // 3. Convert Avro schema to POJO
            convertAvroToPojo(schemaFilePath, outputDir);
            System.out.println("Successfully generated POJO classes in: " + outputDir);
        } catch (IOException e) {
            System.err.println("Error generating POJO classes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void convertAvroToPojo(String schemaFilePath, String outputDir) throws IOException {
        // 1. Parse the Avro schema file
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(new File(schemaFilePath));

        // 2. Configure Avro specific data with logical type conversions
        SpecificData specificData = SpecificData.get();
        specificData.addLogicalTypeConversion(new Conversions.DecimalConversion());
        specificData.addLogicalTypeConversion(new TimeConversions.DateConversion());
        specificData.addLogicalTypeConversion(new TimeConversions.TimeConversion());
        specificData.addLogicalTypeConversion(new TimeConversions.TimestampConversion());

        // 3. Create and configure the compiler
        SpecificCompiler compiler = new SpecificCompiler(schema);
        compiler.setOutputCharacterEncoding("UTF-8");
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.PUBLIC);
        compiler.setTemplateDir("/path/to/custom/templates"); // Optional: for custom templates

        // 4. Generate the Java classes
        compiler.compileToDestination(null, new File(outputDir));
    }
}
