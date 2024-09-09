import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.AbstractJavaSamplerClient;
import org.apache.jmeter.samplers.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.concurrent.ConcurrentHashMap;

public class IBMMQConsumerSampler extends AbstractJavaSamplerClient {

    private ConcurrentHashMap<String, Long> messageTracker;
    private int numberOfMessages;
    private MQReceiver mqReceiver;
    private long[] timeDifferences;

    @Override
    public void setupTest(JavaSamplerContext context) {
        // Set up parameters from JMeter GUI
        String queueManager = context.getParameter("queueManager");
        String hostname = context.getParameter("hostname");
        int port = Integer.parseInt(context.getParameter("port"));
        String channel = context.getParameter("channel");
        String responseQueue = context.getParameter("responseQueue");
        numberOfMessages = Integer.parseInt(context.getParameter("numberOfMessages"));

        mqReceiver = new MQReceiver(queueManager, hostname, port, channel, responseQueue);
        messageTracker = new ConcurrentHashMap<>();
        timeDifferences = new long[numberOfMessages];
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments();
        args.addArgument("queueManager", "QMGR");
        args.addArgument("hostname", "localhost");
        args.addArgument("port", "1414");
        args.addArgument("channel", "CHANNEL");
        args.addArgument("responseQueue", "RESPONSE.QUEUE");
        args.addArgument("numberOfMessages", "80000");
        return args;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();  // Start timing

        try {
            int receivedMessages = 0;

            while (receivedMessages < numberOfMessages) {
                String responseMessage = mqReceiver.receiveMessage();  // Receive response message
                
                // Parse response JSON to extract clientReferenceId
                String clientReferenceId = extractClientReferenceId(responseMessage);

                if (clientReferenceId != null) {
                    Long sentTime = messageTracker.remove(clientReferenceId);
                    if (sentTime != null) {
                        long receivedTime = System.currentTimeMillis();
                        timeDifferences[receivedMessages] = receivedTime - sentTime;
                        receivedMessages++;
                        System.out.println("Message: " + clientReferenceId + " received, Time Difference: " + timeDifferences[receivedMessages - 1] + "ms");
                    }
                }
            }

            result.setSuccessful(true);
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseMessage("Exception: " + e.getMessage());
        } finally {
            result.sampleEnd();  // End timing
        }

        return result;
    }

    private String extractClientReferenceId(String jsonMessage) {
        // Assume the message is in JSON format and parse clientReferenceId from it
        // Use a JSON library (like org.json or Jackson) to extract clientReferenceId
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonMessage);
            return jsonObject.getString("clientReferenceId");
        } catch (org.json.JSONException e) {
            return null;
        }
    }
}
