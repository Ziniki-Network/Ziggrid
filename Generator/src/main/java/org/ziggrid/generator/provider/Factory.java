package org.ziggrid.generator.provider;

import java.util.List;

import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.Timer;

public interface Factory {

	public abstract int nextId();

	public abstract int endAt();

	public abstract String getBucket();

	public abstract String couchUrl();

	public abstract void prepareRun();

	public abstract List<AnalyticItem> doTick(Timer timer);

	public abstract void close();

}
