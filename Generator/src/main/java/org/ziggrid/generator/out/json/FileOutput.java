package org.ziggrid.generator.out.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.AnalyticItem;
import org.ziggrid.api.ExistingObjectProvider;
import org.ziggrid.api.IInterestEngine;
import org.ziggrid.api.IModel;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.api.TickUpdate;
import org.ziggrid.config.StorageConfig;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class FileOutput implements StorageEngine {
	public static Logger logger = LoggerFactory.getLogger("ZigGenerator");
	private FileWriter writer;
	private boolean havePrev = false;

	public FileOutput() {
		String toFile = System.getProperty("fileOutput.storeIn");
		if (toFile == null || toFile.length() == 0)
			throw new UtilException("Using FileOutput requires -DfileOutput.storeIn");
		try {
			writer = new FileWriter(new File(toFile));
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void open(IInterestEngine engine, IModel model, StorageConfig storage) {
		try {
			writer.append("[");
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public short unique() {
		return 0;
	}

	@Override
	public StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean push(TickUpdate toSave) {
		for (AnalyticItem ai : toSave.items) {
			try {
				if (havePrev) writer.write(",");
				writer.append('\n');
				writer.append(ai.asJsonString());
				havePrev = true;
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
		return true;
	}

	@Override
	public void close() {
		try {
			writer.append('\n');
			writer.write("]");
			writer.close();
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void recordServer(String string, JSONObject obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void syncTo(int id, int currentPosition) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean has(String gameId) {
		// TODO Auto-generated method stub
		return false;
	}
}
