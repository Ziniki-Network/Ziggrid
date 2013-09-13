package org.ziggrid.generator.out.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.ZigGenerator;
import org.ziggrid.generator.out.AnalyticStore;
import org.ziggrid.generator.provider.Factory;
import org.ziggrid.utils.exceptions.UtilException;

public class FileOutput implements AnalyticStore {
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
	public void open(Factory f) {
		try {
			writer.append("[");
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	@Override
	public void push(List<AnalyticItem> toSave) {
		for (AnalyticItem ai : toSave) {
			try {
				if (havePrev) writer.write(",");
				writer.append('\n');
				writer.append(ai.asJson());
				havePrev = true;
			} catch (Exception ex) {
				ZigGenerator.logger.severe(ex.getMessage());
			}
		}
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
	public void setGenerator(ZigGenerator gen) {
		// TODO Auto-generated method stub
		
	}

}
