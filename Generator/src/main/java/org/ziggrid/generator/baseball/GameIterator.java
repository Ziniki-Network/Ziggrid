package org.ziggrid.generator.baseball;

import java.util.Iterator;

import org.ziggrid.generator.main.Timer;
import org.ziggrid.utils.csv.CSVLine;
import org.ziggrid.utils.csv.CSVReader;
import org.ziggrid.utils.exceptions.UtilException;

public class GameIterator implements Iterator<Game> {
	BaseballFactory root;
	Timer timer;
	private CSVReader csv;
	private CSVLine currentLine = null;
	private Game pending;

	public GameIterator(BaseballFactory root, Timer timer, CSVReader csv) {
		this.root = root;
		this.timer = timer;
		this.csv = csv;
	}

	@Override
	public boolean hasNext() {
		if (pending != null)
			return true;
		while (true) {
			CSVLine nextLine = nextLine();
			if (nextLine == null)
				return false;
//			BaseballFactory.logger.info(nextLine.get(0));
			if (nextLine.get(0).equals("id")) {
				pending = new Game(this, root.conn);
				return true;
			}
			usedLine();
		}
	}

	@Override
	public Game next() {
		if (pending != null) {
			Game p = pending;
			pending = null;
			return p;
		}
		if (!hasNext())
			throw new IndexOutOfBoundsException("There are no more games");
		return new Game(this, root.conn);
	}

	@Override
	public void remove() {
		throw new UtilException("Cannot remove from a Game Iterator");
	}
	
	CSVLine nextLine() {
		if (currentLine == null)
			currentLine = csv.readLine();
		return currentLine;
	}
	
	public void usedLine() {
		currentLine = null;
	}

	public void close() {
		csv.close();
	}

	public int dayOfYear() {
		if (!hasNext())
			throw new IndexOutOfBoundsException("There are no more games");
		return pending.gameDate;
	}
}