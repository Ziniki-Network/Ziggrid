package org.ziggrid.driver;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.taptomq.TapDataPacket;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class QueueMessageSource implements MessageSource {

	private static final Logger logger = LoggerFactory.getLogger("QueueMessageSource");

	private String serverUrl;
	private String queueName;

	private QueueingConsumer consumer;
	private Channel channel;
	private Connection connection;

	public QueueMessageSource(String serverUrl, String queueName) {
		this.serverUrl = serverUrl;
		this.queueName = queueName;
	}

	public QueueMessageSource(String couchUrl, String bucket, Object... args) {
		if (args == null || args.length != 2)
			throw new ZiggridException("Usage: QueueMessageSource serverUrl queueName");
		this.serverUrl = (String) args[0];
		this.queueName = (String) args[1];
	}

	@Override
	public void initialize() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(serverUrl);
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(queueName, false, false, false, null);
			consumer = new QueueingConsumer(channel);
			boolean autoAck = false;
			channel.basicConsume(queueName, autoAck, consumer);
			logger.error("Connected to rabbit server");
		} catch (IOException e) {
			logger.error("Unable to initialize message queue to " + serverUrl + " with name " + queueName, e);
		}
	}

	@Override
	public TapDataPacket getNextMessage() {
		QueueingConsumer.Delivery delivery;
		boolean seenNothing = false;
		try {
			for (;;) {
				delivery = consumer.nextDelivery(1000);
				if (delivery == null) {
					logger.info("No message received from Rabbit");
					seenNothing = true;
					continue;
				}
				if (seenNothing)
					logger.info("Received message from Rabbit");
				TapDataPacket dataPacket = TapDataPacket.deserialize(delivery.getBody());
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				return dataPacket;
			}
		} catch (InterruptedException e) {
			logger.error("Interupted while waiting for next message delivery", e);
		} catch (IOException e) {
			logger.error("Unable to acknowledge message delivery", e);
		}
		return null;

	}

	@Override
	public void startMessageFlow() throws Exception {
		
	}

	@Override
	public boolean hasMoreMessages() {
		//I'm not sure that this connection has any way to determine that no more messages are forthcoming...
		return true;
	}

	@Override
	public void shutdown() {
		try {
			if (connection != null)
				connection.close();
			if (channel != null)
				channel.close();
		} catch (IOException e) {
			logger.warn("Problems closing message queue connection/channel, e");
		}
	}

}
