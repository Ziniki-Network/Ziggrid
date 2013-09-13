package org.ziggrid.generator.main;

import java.io.IOException;

import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.ziggrid.exceptions.ZiggridException;

public class ControlConnection {
	private final AsyncIOWriter writer;
	private final AtmosphereResponse response;
	GeneratorThread genThr;

	public ControlConnection(AtmosphereResponse response) {
		this.response = response;
		this.writer = response.getAsyncIOWriter();
	}

	public void send(String string) {
		try {
			ControlHandler.logger.info("Sending to client: " + string);
			writer.write(response, string);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
	}

	public void start() {
		if (genThr != null)
			throw new ZiggridException("Cannot start - already running");
		genThr = new GeneratorThread(this, ZigGenerator.instance);
		genThr.start();
	}

	public void stop() {
		if (genThr == null)
			throw new ZiggridException("Cannot stop - not running");
		genThr.pleaseDie();
	}

	public void delay(int d) {
		ZigGenerator.instance.delay = d;
	}
}
