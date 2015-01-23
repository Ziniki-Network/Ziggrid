package org.ziggrid.generator.provider;

import org.ziggrid.api.TickUpdate;
import org.ziggrid.generator.main.Timer;
import org.ziggrid.model.Model;

public interface Factory {

	public abstract String nextId();

	public abstract int endAt();

	public abstract void prepareRun();

	public abstract TickUpdate doTick(Timer timer);

	public abstract void close();

	public abstract Model getModel();
	
	public void setPosition(short unique, int outOf, int mod);

	public abstract int getId();

	public abstract int getCurrentPosition();

}
