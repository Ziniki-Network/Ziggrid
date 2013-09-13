package org.ziggrid.utils.serialization;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.serialization.Endpoint;
import org.ziggrid.utils.serialization.SerializedControllerConnection;

/** Listen for requests, and start processes.
 * If and when a connection drops, kill all associated processes.
 *
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class SerializedController extends Thread {
	private final String loggerName;
	protected final Logger logger;
	private final Class<?>[] acceptableRequests;
	private final Endpoint ep;
	private final ServerSocket socket;
	protected boolean initialized = false;

	public SerializedController(String loggerName, int port, Class<?>[] acceptableRequests) {
		this.loggerName = loggerName;
		this.logger = LoggerFactory.getLogger(loggerName);
		this.acceptableRequests = acceptableRequests;
		try {
			socket = new ServerSocket(port);
			ep = new Endpoint(socket);
			System.out.println("Listening on " + ep);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public Endpoint getEndpoint() {
		return ep;
	}

	@Override
	public void run() {
		synchronized (this)
		{
			while (!initialized)
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		logger.info("Listening for connections");
		while (true)
		{
			try
			{
				Socket conn = socket.accept();
				new SerializedControllerConnection(loggerName, this, conn, acceptableRequests).start();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	public void closingConnection(SerializedControllerConnection serializedControllerConnection) {
		
	}
}
