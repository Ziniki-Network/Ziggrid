package org.ziggrid.generator.out.ws;

import java.util.List;

import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.ZigGenerator;
import org.ziggrid.generator.out.AnalyticStore;
import org.ziggrid.generator.provider.Factory;

public class WebSocketSender implements AnalyticStore {
	private AtmosphereResponse response;
	private AsyncIOWriter writer;
	private ZigGenerator gen;

	public WebSocketSender(AtmosphereResponse response) {
		this.response = response;
		this.writer = response.getAsyncIOWriter();
	}

	@Override
	public void open(Factory f) {
	}

	@Override
	public void push(List<AnalyticItem> toSave) {
		if (writer == null)
			return;
		for (AnalyticItem item : toSave) {
			try {
				writer.write(response, item.asJson());
			} catch (Exception notAgain) {
				notAgain.printStackTrace();
				writer = null;
				return;
			}
		}
	}

	@Override
	public void close() {
		if (gen != null) {
			gen.setEndTime(0); // will end RIGHT NOW!
			gen = null;
		}
	}

	@Override
	public void setGenerator(ZigGenerator gen) {
		this.gen = gen;
	}
}
