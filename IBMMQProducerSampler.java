import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.AbstractJavaSamplerClient;
import org.apache.jmeter.samplers.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class IBMMQProducerSampler extends AbstractJavaSamplerClient {

    private ConcurrentHashMap<String, Long> messageTracker = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private MQSender mqSender;
    private int numberOfMessages;
    private int threads;
    private String messageContent;

    @Override
    public void setupTest(JavaSamplerContext context) {
        // Set up parameters from JMeter GUI
        String queueManager = context.getParameter("queueManager");
        String hostname = context.getParameter("hostname");
        int port = Integer.parseInt(context.getParameter("port"));
        String channel = context.getParameter("channel");
        String requestQueue = context.getParameter("requestQueue");
        numberOfMessages = Integer.parseInt(context.getParameter("numberOfMessages"));
        threads = Integer.parseInt(context.getParameter("threads"));
        messageContent = context.getParameter("message");  // Retrieve the message content

        mqSender = new MQSender(queueManager, hostname, port, channel, requestQueue);
        executorService = Executors.newFixedThreadPool(threads);  // Configurable threads
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments();
        args.addArgument("queueManager", "QMGR");
        args.addArgument("hostname", "localhost");
        args.addArgument("port", "1414");
        args.addArgument("channel", "CHANNEL");
        args.addArgument("requestQueue", "REQUEST.QUEUE");
        args.addArgument("numberOfMessages", "80000");
        args.addArgument("threads", "10");
        args.addArgument("message", "{\"message\": \"This is a message\", \"clientReferenceId\": \"uniqueClientRefId\"}");
        return args;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();  // Start timing

        try {
            CountDownLatch latch = new CountDownLatch(numberOfMessages);

            for (int i = 0; i < numberOfMessages; i++) {
                final String clientReferenceId = "clientRef_" + i;
                String message = messageContent.replace("uniqueClientRefId", clientReferenceId);  // Replace with unique clientReferenceId
                messageTracker.put(clientReferenceId, System.currentTimeMillis());

                executorService.submit(() -> {
                    try {
                        mqSender.sendMessage(message);  // Send message with clientReferenceId
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();  // Wait until all messages are sent
            result.setSuccessful(true);
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseMessage("Exception: " + e.getMessage());
        } finally {
            result.sampleEnd();  // End timing
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        executorService.shutdown();
    }
}
