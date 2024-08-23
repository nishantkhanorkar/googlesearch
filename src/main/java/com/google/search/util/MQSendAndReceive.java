import javax.jms.*;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.JMSC;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.samplers.SampleResult;

public class MQSendAndReceive implements JavaSamplerClient {
    private String hostname;
    private int port;
    private String queueManager;
    private String channel;
    private String sendToQueue;
    private String replyToQueue;
    private String messageContent;

    @Override
    public void setupTest(JavaSamplerContext context) {
        hostname = context.getParameter("hostname");
        port = Integer.parseInt(context.getParameter("port"));
        queueManager = context.getParameter("queueManager");
        channel = context.getParameter("channel");
        sendToQueue = context.getParameter("sendToQueue");
        replyToQueue = context.getParameter("replyToQueue");
        messageContent = context.getParameter("messageContent");
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart(); // Start timing

        MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();
        try {
            connectionFactory.setHostName(hostname);
            connectionFactory.setPort(port);
            connectionFactory.setQueueManager(queueManager);
            connectionFactory.setChannel(channel);
            connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

            QueueConnection connection = connectionFactory.createQueueConnection();
            connection.start();

            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue sendQueue = session.createQueue(sendToQueue);
            Queue replyQueue = session.createQueue(replyToQueue);

            QueueSender sender = session.createSender(sendQueue);
            TextMessage message = session.createTextMessage(messageContent);
            message.setJMSReplyTo(replyQueue);

            sender.send(message);

            QueueReceiver receiver = session.createReceiver(replyQueue);
            Message responseMessage = receiver.receive(30000); // Timeout after 30 seconds

            if (responseMessage != null && responseMessage instanceof TextMessage) {
                String responseText = ((TextMessage) responseMessage).getText();
                result.sampleEnd(); // End timing
                result.setResponseData(responseText, "UTF-8");
                result.setSuccessful(true);
            } else {
                result.sampleEnd(); // End timing
                result.setResponseMessage("No response or wrong message type received");
                result.setSuccessful(false);
            }

            session.close();
            connection.close();
        } catch (Exception e) {
            result.sampleEnd(); // End timing
            result.setResponseMessage("Exception: " + e.getMessage());
            result.setSuccessful(false);
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        // Cleanup if necessary
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("hostname", "192.168.1.1");
        params.addArgument("port", "1212");
        params.addArgument("queueManager", "TESTHUB");
        params.addArgument("channel", "ASVUT_NOSSL.TESTHUB");
        params.addArgument("sendToQueue", "AST.QP.ALL.ACCSERV.VALUE.REQ");
        params.addArgument("replyToQueue", "AST.QP.ALL.PYMT.VALUE.RES");
        params.addArgument("messageContent", "Your message content here");
        return params;
    }
}
