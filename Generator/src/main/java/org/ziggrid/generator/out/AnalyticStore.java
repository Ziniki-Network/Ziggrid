package org.ziggrid.generator.out;

import java.util.List;

import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.ZigGenerator;
import org.ziggrid.generator.provider.Factory;

public interface AnalyticStore {

	void open(Factory factory);
	void push(List<AnalyticItem> toSave);
	void close();
	void setGenerator(ZigGenerator gen);
	
}
