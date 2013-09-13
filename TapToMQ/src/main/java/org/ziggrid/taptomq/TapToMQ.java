package org.ziggrid.taptomq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapToMQ {
	static List<TapObserver> observers = new ArrayList<TapObserver>();
	
	private static final Logger logger = LoggerFactory.getLogger("TapToMq");
	
	public static void main(String[] args) throws IOException{

		if (args.length < 3) {
			System.err.println("Usage: TapToMQ <couchUrl> <rabbitUrl> <bucket> [--maxItems <max items count>]");
			System.exit(1);
		}

		String couchUrl = args[0];
		String bucket = args[1];
		String rabbitUrl = args[2];
		
		int maxItems = -1;
		if (args.length > 3) {
			maxItems = Integer.parseInt(args[4]);
		}
		
		logger.info("Starting TapToMQ:");
		logger.info("Couch URL: " + couchUrl);
		logger.info("Couch bucket: " + bucket);
		logger.info("RabbitMQ URL: " + rabbitUrl);
		
		List<Thread> threads = new ArrayList<Thread>();
		TapObserver observer = new TapObserver(couchUrl, bucket, rabbitUrl, maxItems);
		observers.add(observer);
		Thread obsThread = new Thread(observer);
		obsThread.start();
		threads.add(obsThread);
	}
}
