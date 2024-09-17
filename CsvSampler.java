import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvSampler extends AbstractJavaSamplerClient {

    private List<String[]> csvData = new ArrayList<>();
    private int currentIndex = 0;

    // Called once per thread, before the runTest method
    @Override
    public void setupTest(JavaSamplerContext context) {
        String csvFile = context.getParameter("csvFile");  // Get the file path from JMeter parameter
        loadCsvData(csvFile);
    }

    // Loads the CSV data and stores it in a list
    private void loadCsvData(String csvFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                csvData.add(line.split(","));  // Split the row by commas and store it
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called for every sample execution
    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();

        if (currentIndex < csvData.size()) {
            String[] row = csvData.get(currentIndex++);
            String sampleId = row[0];
            long startTime = Long.parseLong(row[1]);  // Parse the start time

            result.setSampleLabel("Sample-" + sampleId);
            result.sampleStart(startTime);  // Set the start time for the sample

            // Check if endTime is present and valid
            if (row.length > 2 && !row[2].isEmpty()) {
                try {
                    long endTime = Long.parseLong(row[2]);  // Parse the end time
                    result.sampleEnd(endTime);  // Set the end time for the sample
                    result.setSuccessful(true);  // Mark the sample as successful
                    result.setResponseMessage("Processed sample " + sampleId);
                    result.setResponseData("Start time: " + startTime + ", End time: " + endTime, "UTF-8");
                } catch (NumberFormatException e) {
                    result.sampleEnd();  // End the sample now if endTime is invalid
                    result.setSuccessful(false);  // Mark the sample as failed
                    result.setResponseMessage("Invalid endTime for sample " + sampleId);
                    result.setResponseData("Start time: " + startTime + ", Invalid endTime", "UTF-8");
                }
            } else {
                result.sampleEnd();  // End the sample now if endTime is missing
                result.setSuccessful(false);  // Mark the sample as failed
                result.setResponseMessage("Missing endTime for sample " + sampleId);
                result.setResponseData("Start time: " + startTime + ", Missing endTime", "UTF-8");
            }
        } else {
            result.setSampleLabel("End of CSV");
            result.setSuccessful(false);
            result.setResponseMessage("No more rows to process");
        }

        return result;
    }

    // Called once per thread, after the runTest method
    @Override
    public void teardownTest(JavaSamplerContext context) {
        // Cleanup if necessary (optional)
    }
}
