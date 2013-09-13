package org.ziggrid.generator.baseball;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.CouchbaseAwareFactory;
import org.ziggrid.generator.main.Timer;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.csv.CSVReader;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.xml.XML;

import com.couchbase.client.CouchbaseClient;

public class BaseballFactory implements CouchbaseAwareFactory {
	final static Logger logger = LoggerFactory.getLogger("BaseballFactory");
	private final Config config;
	private int processingErrors = 0;
	private int nextId = 0;
	private ListMap<String, File> seasonFiles = new ListMap<String, File>();
	private Iterator<String> seasonIterator;
	private final List<GameIterator> games = new ArrayList<GameIterator>();
	private int lastDayReported = -1;
	CouchbaseClient conn;

	public BaseballFactory(XML config) {
		this.config = config.populate(Config.class, this);
		for (UseFile x : this.config.files)
			x.storeFiles(seasonFiles);
		for (UseDirectory x : this.config.directories)
			x.storeFiles(seasonFiles);
		seasonIterator = new TreeSet<String>(seasonFiles.keySet()).iterator();
		logger.info("Processing files: " + seasonFiles.keySet());
	}

	@Override
	public String couchUrl() {
		return config.couchUrl;
	}

	@Override
	public String getBucket() {
		return config.bucket;
	}
	
	@Override
	public void setConnection(CouchbaseClient conn) {
		this.conn = conn;
	}

	@Override
	public int endAt() {
		return 30000000;
	}

	@Override
	public int nextId() {
		return ++nextId;
	}

	@Override
	public void prepareRun() {
	}

	@Override
	public List<AnalyticItem> doTick(Timer timer) {
		while (true) {
			while (games.isEmpty()) {
				if (!seasonIterator.hasNext())
					return null;
				String nextSeason = seasonIterator.next();
				logger.info("Season " + nextSeason);
				for (File f : seasonFiles.get(nextSeason))
					games.add(games(timer, f));
				logger.info("Number of home teams: " + games.size());
			}

			while (!games.isEmpty()) {
				GameIterator next = findNextLogicalGame();
				if (next == null) {
					continue;
				}
				Game game = next.next();
				if (lastDayReported != game.gameDate) {
					lastDayReported = game.gameDate;
					game.reportDate();
				}
				logger.info("Processing game: " + game.visteam + "@" + game.hometeam + "[" + game.season + ":" + game.gameDate + "]");
				InningState state = new InningState();
				while (game.hasEvent()) {
					game.nextEvent(state);
					state.reset();
				}
				if (state.processingErrors != 0) {
					processingErrors  += state.processingErrors;
					logger.error("Saw " + state.processingErrors + " processing errors for a total of " + processingErrors);
				}
				game.addTeamEvents();
				return game.events();
			}
		}
	}

	private GameIterator findNextLogicalGame() {
		GameIterator ret = null;
		while (!games.isEmpty()) {
			List<GameIterator> done = new ArrayList<GameIterator>();
			for (GameIterator gi : games) {
				if (!gi.hasNext()) {
					done.add(gi);
					continue;
				}
				if (ret == null)
					ret = gi;
				else if (gi.dayOfYear() < ret.dayOfYear())
					ret = gi;
			}
			games.removeAll(done);
			if (ret != null)
				return ret;
		}
		return null;
	}

	private GameIterator games(Timer timer, File f) {
		try {
			CSVReader r = new CSVReader(new FileInputStream(f));
			return new GameIterator(this, timer, r);
		} catch (FileNotFoundException ex) {
			throw new UtilException("Could not find file " + f);
		}
	}

	@Override
	public void close() {
		logger.error("Total processing errors = " + processingErrors);
	}
}
