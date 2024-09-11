package com.example.jmeter.mqplugin;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import javax.jms.*;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import java.util.ArrayList;
import java.util.List;

public class MQProducerConsumerSampler extends AbstractJavaSamplerClient {

    // MQ Connection Details
    private String queueManager;
    private String hostname;
    private int port;
    private String channel;
    private String requestQueue;
    private String responseQueue;
    private String clientReferenceId;
    private int numberOfMessages;

    // JMS objects
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private MessageConsumer consumer;

    @Override
    public void setupTest(JavaSamplerContext context) {
        // Initialize parameters
        queueManager = context.getParameter("queueManager");
        hostname = context.getParameter("hostname");
        port = Integer.parseInt(context.getParameter("port"));
        channel = context.getParameter("channel");
        requestQueue = context.getParameter("requestQueue");
        responseQueue = context.getParameter("responseQueue");
        clientReferenceId = context.getParameter("clientReferenceId");
        numberOfMessages = Integer.parseInt(context.getParameter("numberOfMessages", "1")); // Default to 1

        try {
            // Set up the connection factory for IBM MQ
            MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
            factory.setQueueManager(queueManager);
            factory.setHostName(hostname);
            factory.setPort(port);
            factory.setChannel(channel);
            factory.setTransportType(1);  // Bind to IBM MQ transport

            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Set up the producer and consumer
            Queue requestQ = session.createQueue(requestQueue);
            producer = session.createProducer(requestQ);

            Queue responseQ = session.createQueue(responseQueue);
            consumer = session.createConsumer(responseQ);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.setSampleLabel("IBM MQ Producer-Consumer Test");

        List<Long> responseTimes = new ArrayList<>();

        try {
            connection.start();  // Start connection for consumer
            result.sampleStart(); // Start tracking total time for all messages

            for (int i = 0; i < numberOfMessages; i++) {
                // Start individual sample timing
                long messageStartTime = System.currentTimeMillis();

                // Create and send the message
                TextMessage message = session.createTextMessage();
                message.setText(createMessage(i));
                message.setJMSCorrelationID(clientReferenceId + "-" + i);  // Use unique correlation ID for each message
                producer.send(message);

                // Receive the correlated message (wait for 5 seconds)
                Message responseMessage = consumer.receive(5000);

                if (responseMessage != null && responseMessage instanceof TextMessage) {
                    String responseText = ((TextMessage) responseMessage).getText();
                    long messageEndTime = System.currentTimeMillis();
                    long responseTime = messageEndTime - messageStartTime; // Calculate time difference
                    responseTimes.add(responseTime); // Store response time for each message
                } else {
                    result.setResponseMessage("No response received for message " + i);
                    result.setSuccessful(false);
                    return result;
                }
            }

            result.setSuccessful(true);
            result.sampleEnd(); // End tracking total time

            // Calculate average response time
            long totalResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum();
            long averageResponseTime = totalResponseTime / numberOfMessages;

            // Set response data to show number of messages and average response time
            result.setResponseData("Number of messages: " + numberOfMessages +
                                   ", Average response time (ms): " + averageResponseTime, "UTF-8");

        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseMessage("Exception: " + e.getMessage());
        }

        return result;
    }

    private String createMessage(int index) {
        // Create the custom JSON message with the clientReferenceId for correlation
        return "{\"clientReferenceId\": \"" + clientReferenceId + "-" + index + "\", \"field1\": \"value1\", \"field2\": \"value2\"}";
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            if (producer != null) producer.close();
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
