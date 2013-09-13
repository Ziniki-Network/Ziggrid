package org.ziggrid.generator.out.json;

import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.ZigGenerator;
import org.ziggrid.generator.out.AnalyticStore;
import org.ziggrid.generator.provider.Factory;

public class ConsoleDisplay implements AnalyticStore {

	@Override
	public void open(Factory f) {
	}

	@Override
	public void push(List<AnalyticItem> toSave) {
		for (AnalyticItem ai : toSave) {
			try {
				System.out.println(ai.asJson());
			} catch (JSONException ex) {
				ZigGenerator.logger.severe(ex.getMessage());
			}
		}
	}

	@Override
	public void close() {
	}

	@Override
	public void setGenerator(ZigGenerator gen) {
	}
}
