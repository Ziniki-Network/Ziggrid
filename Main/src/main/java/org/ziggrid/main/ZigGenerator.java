package org.ziggrid.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.TickUpdate;
import org.ziggrid.generator.main.Timer;
import org.ziggrid.generator.provider.Factory;
import org.ziggrid.model.Model;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.SyncUtils;
import org.zinutils.xml.XML;

import com.codahale.metrics.Timer.Context;

public class ZigGenerator {
	final Factory factory;
	private final StorageEngine analyticStore;
	private Timer timer;
	public static Logger logger = LoggerFactory.getLogger("ZigGenerator");
	private int endTime;
	public int delay;
	public final int limit;
	private int count = 0;
	private final com.codahale.metrics.Timer tickTimer;
	private Context tickTimerContext;

	public ZigGenerator(boolean realtime, int delay, int limit, XML configXML, StorageEngine store, Model model) {
		this.delay = delay;
		this.limit = limit;
		String factoryClass = configXML.top().get("factory");
		factory = Reflection.create(getClass().getClassLoader(), factoryClass, store, configXML, model);
		timer = new Timer(realtime, 0);
		endTime = factory.endAt();
		this.analyticStore = store;
		tickTimer = CodaHaleMetrics.metrics.timer("GeneratorTickTimer-" + factory.getId());
	}

	public ZigGenerator setEndTime(int endTime) {
		this.endTime = endTime;
		return this;
	}
	
	public void resetCounter() {
		count = 0;
	}

	public boolean timeUp() {
		return (endTime != -1 && !timer.lessThan(endTime)) || (limit >= 0 && count > limit);
	}

	public void init() {
		factory.prepareRun();
	}

	public boolean advanceOneTick() {
		tickTimerContext = tickTimer.time();
		try {
			logger.debug("Generating events at " + timer + "; end = " + endTime);
			TickUpdate toSave = factory.doTick(timer);
			if (toSave == null)
				return false;
			analyticStore.syncTo(factory.getId(), factory.getCurrentPosition());
			if (!analyticStore.push(toSave))
				return false;
			count += toSave.items.size();
			if (limit > 0 && count > limit)
				return false;
			timer.next();
			if (delay > 0)
				SyncUtils.sleep(delay);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		finally {
			tickTimerContext.stop();
		}
	}
	
	public void close() {
		logger.info("Closing down ...");
		factory.close();
	}

	public String currentTick() {
		return timer.toString();
	}
}
