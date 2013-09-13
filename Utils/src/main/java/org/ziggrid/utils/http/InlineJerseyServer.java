package org.ziggrid.utils.http;

public class InlineJerseyServer extends InlineServer {

	public InlineJerseyServer(int port, String appClz) {
		super(port, "com.sun.jersey.spi.container.servlet.ServletContainer");
		initParam("javax.ws.rs.Application", appClz);
	}

	public InlineJerseyServer(String amqpUri, String appClz) {
		super(amqpUri, "com.sun.jersey.spi.container.servlet.ServletContainer");
		initParam("javax.ws.rs.Application", appClz);
	}
}
