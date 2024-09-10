import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.JMSException;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.AbstractJavaSamplerClient;
import org.apache.jmeter.samplers.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
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
    private String replyToQueue;
    private String clientReferenceId;
    private static ConcurrentHashMap<String, Long> messageSentTimeMap = new ConcurrentHashMap<String, Long>();  // Stores clientReferenceId and the time message was sent

    @Override
    public void setupTest(JavaSamplerContext context) {
        // Retrieve parameters from JMeter GUI
        queueManager = context.getParameter("queueManager");
        hostname = context.getParameter("hostname");
        port = Integer.parseInt(context.getParameter("port"));
        channel = context.getParameter("channel");
        requestQueue = context.getParameter("requestQueue");
        replyToQueue = context.getParameter("replyToQueue");
        numberOfMessages = Integer.parseInt(context.getParameter("numberOfMessages"));
        threads = Integer.parseInt(context.getParameter("threads"));
        messageContent = context.getParameter("message");
        clientReferenceId = context.getParameter("clientReferenceId");

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
        args.addArgument("replyToQueue", "REPLY.QUEUE");
        args.addArgument("numberOfMessages", "80000");
        args.addArgument("threads", "10");
        args.addArgument("message", "{\"message\": \"This is message\", \"clientReferenceId\": \"uniqueClientReferenceId\"}");
        args.addArgument("clientReferenceId", "clientRefId");  // Example default clientReferenceId
        return args;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();  // Start timing

        try {
            CountDownLatch latch = new CountDownLatch(numberOfMessages);
            final MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
            factory.setHostName(hostname);
            factory.setPort(port);
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setTransportType(1);  // Set TCP/IP transport type

            for (int i = 0; i < numberOfMessages; i++) {
                final String uniqueClientReferenceId = clientReferenceId + "_" + i;
                final String message = messageContent.replace("uniqueClientReferenceId", uniqueClientReferenceId);

                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        Connection connection = null;
                        Session session = null;
                        try {
                            connection = factory.createQueueConnection();
                            connection.start();
                            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                            Queue queue = session.createQueue(requestQueue);
                            MessageProducer producer = session.createProducer(queue);

                            TextMessage textMessage = session.createTextMessage(message);

                            // Set ReplyTo queue
                            Queue replyQueue = session.createQueue(replyToQueue);
                            textMessage.setJMSReplyTo(replyQueue);

                            // Store the time message is sent in ConcurrentHashMap
                            messageSentTimeMap.put(uniqueClientReferenceId, System.currentTimeMillis());

                            producer.send(textMessage);

                        } catch (JMSException e) {
                            e.printStackTrace();
                        } finally {
                            if (session != null) {
                                try {
                                    session.close();
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (connection != null) {
                                try {
                                    connection.close();
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }
                            }
                            latch.countDown();
                        }
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

    public static ConcurrentHashMap<String, Long> getMessageSentTimeMap() {
        return messageSentTimeMap;
    }
}
