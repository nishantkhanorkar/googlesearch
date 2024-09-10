import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.JMSException;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.AbstractJavaSamplerClient;
import org.apache.jmeter.samplers.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.concurrent.ConcurrentHashMap;

public class IBMMQConsumerSampler extends AbstractJavaSamplerClient {

    private String queueManager;
    private String hostname;
    private int port;
    private String channel;
    private String responseQueue;

    @Override
    public void setupTest(JavaSamplerContext context) {
        // Retrieve parameters from JMeter GUI
        queueManager = context.getParameter("queueManager");
        hostname = context.getParameter("hostname");
        port = Integer.parseInt(context.getParameter("port"));
        channel = context.getParameter("channel");
        responseQueue = context.getParameter("responseQueue");
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments();
        args.addArgument("queueManager", "QMGR");
        args.addArgument("hostname", "localhost");
        args.addArgument("port", "1414");
        args.addArgument("channel", "CHANNEL");
        args.addArgument("responseQueue", "RESPONSE.QUEUE");
        return args;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();  // Start timing

        try {
            MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
            factory.setHostName(hostname);
            factory.setPort(port);
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setTransportType(1);  // Set TCP/IP transport type

            Connection connection = null;
            Session session = null;
            try {
                connection = factory.createQueueConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue(responseQueue);
                MessageConsumer consumer = session.createConsumer(queue);

                TextMessage receivedMessage = (TextMessage) consumer.receive(1000);  // Timeout in milliseconds

                if (receivedMessage != null) {
                    String messageText = receivedMessage.getText();  // Extract message text
                    String clientReferenceId = extractClientReferenceId(messageText);  // Extract clientReferenceId from message

                    // Get the time when the corresponding message was sent
                    Long sentTime = IBMMQProducerSampler.getMessageSentTimeMap().get(clientReferenceId);

                    if (sentTime != null) {
                        long receivedTime = System.currentTimeMillis();
                        long timeDifference = receivedTime - sentTime;
                        result.setResponseMessage("Message with clientReferenceId: " + clientReferenceId + " took " + timeDifference + " ms to receive.");
                    } else {
                        result.setResponseMessage("No corresponding sent message found for clientReferenceId: " + clientReferenceId);
                    }
                }

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

    private String extractClientReferenceId(String messageText) {
        // Simple extraction logic (modify this based on actual message format)
        String clientRefPrefix = "\"clientReferenceId\": \"";
        int start = messageText.indexOf(clientRefPrefix) + clientRefPrefix.length();
        int end = messageText.indexOf("\"", start);
        return messageText.substring(start, end);
    }

