package org.ziggrid.generator.baseball;

import java.util.Iterator;

import org.ziggrid.generator.main.Timer;
import org.zinutils.csv.CSVLine;
import org.zinutils.csv.CSVReader;
import org.zinutils.exceptions.UtilException;

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
			if (nextLine.get(0).equals("id")) {
				pending = new Game(this);
				return true;
			}
			usedLine();
		}
	}

	@Override
	public Game next() {
		if (!hasNext() || pending == null)
			throw new IndexOutOfBoundsException("There are no more games");
		Game ret = pending;
		pending = null;
		return ret;
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