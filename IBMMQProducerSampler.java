import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.JMSException;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.AbstractJavaSamplerClient;
import org.apache.jmeter.samplers.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class IBMMQProducerSampler extends AbstractJavaSamplerClient {

    private ExecutorService executorService;
    private int numberOfMessages;
    private int threads;
    private String messageContent;
    private String queueManager;
    private String hostname;
    private int port;
    private String channel;
    private String requestQueue;
    
    @Override
    public void setupTest(JavaSamplerContext context) {
        // Retrieve parameters from JMeter GUI
        queueManager = context.getParameter("queueManager");
        hostname = context.getParameter("hostname");
        port = Integer.parseInt(context.getParameter("port"));
        channel = context.getParameter("channel");
        requestQueue = context.getParameter("requestQueue");
        numberOfMessages = Integer.parseInt(context.getParameter("numberOfMessages"));
        threads = Integer.parseInt(context.getParameter("threads"));
        messageContent = context.getParameter("message");

        executorService = Executors.newFixedThreadPool(threads);
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
        args.addArgument("message", "{\"message\": \"This is message\", \"clientReferenceId\": \"uniqueClientReferenceId\"}");
        return args;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();  // Start timing

        try {
            CountDownLatch latch = new CountDownLatch(numberOfMessages);
            MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
            factory.setHostName(hostname);
            factory.setPort(port);
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setTransportType(1);  // Set TCP/IP transport type

            for (int i = 0; i < numberOfMessages; i++) {
                final String clientReferenceId = "clientRef_" + i;
                String message = messageContent.replace("uniqueClientReferenceId", clientReferenceId);

                executorService.submit(() -> {
                    try (Connection connection = factory.createQueueConnection();
                         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

                        Queue queue = session.createQueue(requestQueue);
                        MessageProducer producer = session.createProducer(queue);

                        TextMessage textMessage = session.createTextMessage(message);
                        producer.send(textMessage);

                    } catch (JMSException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();  // Wait for all threads to finish sending messages
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
