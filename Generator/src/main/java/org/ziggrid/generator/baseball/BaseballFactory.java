package org.ziggrid.generator.baseball;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.api.TickUpdate;
import org.ziggrid.generator.main.Timer;
import org.ziggrid.generator.provider.Factory;
import org.ziggrid.model.Model;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.JsonReader;
import org.zinutils.collections.ListMap;
import org.zinutils.csv.CSVReader;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.DateUtils;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StreamProvider;
import org.zinutils.xml.XML;

public class BaseballFactory implements Factory {
	final static Logger logger = LoggerFactory.getLogger("BaseballFactory");
	private final StorageEngine store;
	private final Config config;
	private final Model model;
	private int processingErrors = 0;
	private int nextId = 0;
	private ListMap<String, StreamProvider> seasonFiles = new ListMap<String, StreamProvider>();
	private Iterator<String> seasonIterator;
	private final List<GameIterator> games = new ArrayList<GameIterator>();
	private int lastDayReported = -1;
	private short unique;
	private int outOf = 1;
	private int mod = 0;
	private int logical = 0;

	public BaseballFactory(StorageEngine store, XML config, Model model) {
		this.store = store;
		this.config = config.populate(Config.class, this);
		this.model = model;
		for (UseFile x : this.config.files)
			x.storeFiles(seasonFiles);
		for (UseDirectory x : this.config.directories)
			x.storeFiles(seasonFiles);
		for (UseResource x : this.config.resources)
			x.storeFiles(seasonFiles);
		seasonIterator = new TreeSet<String>(seasonFiles.keySet()).iterator();
		logger.info("Processing files: " + seasonFiles.keySet());
	}

	@Override
	public void setPosition(short unique, int outOf, int mod) {
		this.unique = unique;
		this.outOf = outOf;
		this.mod = mod;
	}

	@Override
	public int getId() {
		return mod;
	}

	@Override
	public int getCurrentPosition() {
		return logical/outOf;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public int endAt() {
		return 30000000;
	}

	@Override
	public String nextId() {
		++nextId;
		if (outOf == 1)
			return unique + "_" + Integer.toString(nextId);
		return unique + "_" + mod + "_" + nextId;
	}

	@Override
	public void prepareRun() {
	}

	@Override
	public TickUpdate doTick(Timer timer) {
		TickUpdate ret = new TickUpdate();
		while (true) {
			while (games.isEmpty()) {
				if (!seasonIterator.hasNext())
					return null;
				String nextSeason = seasonIterator.next();
				for (StreamProvider f : seasonFiles.get(nextSeason))
					games.add(games(timer, f));
				logger.info("Processing Season " + nextSeason + (outOf != -1 ? " for mod " + mod + "/" + outOf : "") + " with " + games.size() + " home team files");
			}

			while (!games.isEmpty()) {
				GameIterator next = findNextLogicalGame();
				if (next == null) {
					continue;
				}
				Game game = next.next();
				StoreableObject gameRecord = new StoreableObject("gameRecord", "gameRecord_" + game.gameid);
				if (store.has(gameRecord.id()))
					continue;
				ret.updates.put(gameRecord.id(), gameRecord);
				if (lastDayReported != game.gameDate) {
					lastDayReported = game.gameDate;
					game.reportDate();
				}
				logger.info("Processing game: " + game.visteam + "@" + game.hometeam + "[" + game.season + ":" + game.gameDate + "]");
				for (StoreableObject o : game.players()) {
					try {
						ret.updates.put(o.id(), o);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
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
				ret.items.addAll(game.events());
				return ret;
			}
		}
	}

	private GameIterator findNextLogicalGame() {
		GameIterator ret = null;
		while (!games.isEmpty()) {
			List<GameIterator> done = new ArrayList<GameIterator>();
			for (GameIterator gi : games) {
				if (!gi.hasNext()) {
					gi.close();
					done.add(gi);
					continue;
				}
				if (ret == null)
					ret = gi;
				else if (gi.dayOfYear() < ret.dayOfYear())
					ret = gi;
			}
			games.removeAll(done);
			if (ret != null) {
				if (outOf == 1 || logical++%outOf == mod)
					return ret;
				// otherwise, we need to "pretend" we used it
				ret.next();
				ret = null;
			}
		}
		return null;
	}

	private GameIterator games(Timer timer, StreamProvider f) {
		CSVReader r = new CSVReader(f.read());
		return new GameIterator(this, timer, r);
	}

	@Override
	public void close() {
		logger.error("Total processing errors = " + processingErrors);
	}
	
	public static void main(String[] args) {
		if (args.length != 3)
			throw new UtilException("Usage: BaseballFactory <model dir> <model bucket> <xml>");
		File xmlf = new File(args[0], args[1]);
		if (!xmlf.isDirectory())
			throw new UtilException("The path " + xmlf + " is not a directory");
		ErrorHandler eh = new ErrorHandler();
		Model m = new Model();
		for (File df : xmlf.listFiles()) {
			logger.info("Found document directory " + df.getName());
			for (File mf : df.listFiles()) {
				if (mf.getName().endsWith(".json")) {
					logger.info("Reading " + mf.getPath());
					JsonReader jr = new JsonReader();
					jr.readModel(eh, m, df.getName(), FileUtils.readFile(mf));
				}
			}
		}
		m.validate(eh);
        if (eh.hasErrors()) {
        	eh.displayErrors();
        	System.exit(1);
        }

        Date d = new Date();
		BaseballFactory factory = new BaseballFactory(null, XML.fromFile(new File(args[2])), m);
		Iterator<String> sit = factory.seasonFiles.iterator();
		while (sit.hasNext()) {
			String seasonName = sit.next();
			for (StreamProvider f : factory.seasonFiles.get(seasonName))
				factory.games.add(factory.games(null, f));
			System.out.println("Season " + seasonName + factory.games);
			GameIterator game;
			while ((game = factory.findNextLogicalGame()) != null) {
				Game g = game.next();
				System.out.println(g);
			}
		}
		System.out.println("Elapsed = " + DateUtils.elapsedTime(d, new Date(), DateUtils.Format.sss3));
	}
}
