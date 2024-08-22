import javax.jms.*;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.WMQConstants;

public class MQSendAndReceive {
    public static void main(String[] args) {
        String hostname = "192.168.1.1";
        int port = 1212;
        String queueManager = "TESTHUB";
        String channel = "ASVUT_NOSSL.TESTHUB";
        String sendToQueue = "AST.QP.ALL.ACCSERV.VALUE.REQ";
        String replyToQueue = "AST.QP.ALL.PYMT.VALUE.RES";

        MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();
        QueueConnection connection = null;
        QueueSession session = null;
        QueueSender sender = null;
        QueueReceiver receiver = null;

        try {
            // Set the MQ connection parameters
            connectionFactory.setHostName(hostname);
            connectionFactory.setPort(port);
            connectionFactory.setQueueManager(queueManager);
            connectionFactory.setChannel(channel);
            connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);

            // Create a connection and start it
            connection = connectionFactory.createQueueConnection();
            connection.start();

            // Create a session and specify the acknowledgement mode
            session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create a queue object for the sendToQueue
            Queue requestQueue = session.createQueue("queue:///" + sendToQueue);
            Queue responseQueue = session.createQueue("queue:///" + replyToQueue);

            // Create a sender for the request queue
            sender = session.createSender(requestQueue);

            // Create a text message to send
            TextMessage requestMessage = session.createTextMessage("Your message content here");

            // Set the JMSReplyTo field to specify where the response should be sent
            requestMessage.setJMSReplyTo(responseQueue);

            // Send the message
            sender.send(requestMessage);
            System.out.println("Message sent to the queue: " + sendToQueue);

            // Create a receiver for the response queue
            receiver = session.createReceiver(responseQueue);

            // Wait for a response (timeout can be set if desired)
            Message responseMessage = receiver.receive(30000); // 30 seconds timeout

            if (responseMessage instanceof TextMessage) {
                TextMessage textResponse = (TextMessage) responseMessage;
                System.out.println("Received response: " + textResponse.getText());
            } else {
                System.out.println("Received non-text response");
            }

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (sender != null) sender.close();
                if (receiver != null) receiver.close();
                if (session != null) session.close();
                if (connection != null) connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
