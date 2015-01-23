package org.ziggrid.generator.baseball;

import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class InningState {
	public enum RunnerAdvances {
		Safely, Erased
	}

	@SuppressWarnings("serial")
	public class StateException extends RuntimeException {
		public StateException(String string) {
			super(string);
		}
	}

	public int processingErrors = 0;
	public int inning = 1;
	public boolean half = false;
	public int outs = 0;
	public int rbi = 0;
	public int runs = 0;
	public boolean[] bases = new boolean[4];
	public boolean[] newBases = null;
	private int runnerOuts;
	public int errorOuts;

	public void moveUp(String action) {
		// Don't automatically move up on outs ...
		if (invalidAction(action))
			return;
		
		int quant = figureQuant(action);
		if (quant != -1) { // if walk, placeBatter will do the job ...
			for (int b=3;b>0;b--) {
				if (bases[b] && b+quant > 3) {
					rbi++;
					runs++;
				} else if (bases[b])
					bases[b+quant] = true;
				bases[b] = false;
			}
			while (--quant > 0)
				bases[quant] = false;
		}
		placeBatter(action);
	}

	public void placeBatter(String action) {
		if (invalidAction(action))
			return;
		int quant = figureQuant(action);
		if (quant == 0)
			return;
		else if (quant == -1) { // walk; fill first available base ...
			for (int i=1;i<4;i++) {
				if (i == 4) {
					rbi++;
					runs++;
				} else if (!bases[i]) {
					bases[i] = true;
					break;
				}
			}
		} else if (quant == 4) {
			rbi++;
			runs++;
		} else
			bases[quant] = true;
	}

	public boolean invalidAction(String action) {
		return (action.length() == 1 && "FGKLP".contains(action)) || action.equals("SB") || action.equals("SF") || action.equals("DP");
	}
	
	public int figureQuant(String action) {
		if (action.equals("BB") || action.equals("HBP") || action.equals("CI") || action.equals("E"))
			return -1;
		else if (action.equals("FC"))
			return 1;
		else if (action.equals("1B"))
			return 1;
		else if (action.equals("2B"))
			return 2;
		else if (action.equals("3B"))
			return 3;
		else if (action.equals("HR"))
			return 4;
		else
			throw new UtilException("Cannot move up " + action);
	}

	public void dotAdvance() {
		newBases = new boolean[4];
	}

	public void moveRunner(int from, int to, RunnerAdvances reachedSafely, boolean creditRbis) {
		if (from != 0 && !bases[from])
			throw new StateException("Cannot move runner from base " + from + " because there is no-one there");
		bases[from] = false;
		if (reachedSafely == RunnerAdvances.Safely) {
			if (to == 4) {
				if (creditRbis)
					rbi++;
				runs++;
			} else if (newBases != null)
				newBases[to] = true;
			else
				bases[to] = true;
		} else if (reachedSafely == RunnerAdvances.Erased)
			runnerOuts++;
	}

	public void doneDotAdvance() {
		if (newBases != null)
			for (int i=0;i<4;i++) bases[i] |= newBases[i];
		newBases = null;
	}

	public void advanceGame(String action) {
		if (action == null || CollectionUtils.listOf("BB", "1B", "2B", "3B", "HR", "HBP", "E", "FC", "CS", "CI").contains(action)) {
			outs+=runnerOuts;
		}
		else if (action.equals("DP"))
			outs+=2;
		else if ((action.length() == 1 && "FGKLP".contains(action)) || action.equals("SB") || action.equals("SF")) {
			if (errorOuts == 0)
				outs += runnerOuts + 1;
		} else 
			throw new UtilException("Cannot advance by " + action);
		if (outs == 3) {
			if (!half)
				half = true;
			else {
				half = false;
				inning++;
			}
			outs = 0;
			bases[1] = bases[2] = bases[3] = false;
		} else if (outs > 3)
			throw new UtilException("Outs went up to " + outs);
		runnerOuts = 0;
		errorOuts = 0;
	}
	
	public void skipTo(int inning, boolean half) {
		this.inning = inning;
		this.half = half;
		outs = 0;
		bases[1] = bases[2] = bases[3] = false;
		reset();
		processingErrors++;
	}
	
	public void reset() {
		rbi = 0;
		runs = 0;
		errorOuts = 0;
		runnerOuts = 0;
	}

	@Override
	public String toString() {
		return inning + (half?"H":"V") + outs + (bases[3]?"<":".") + (bases[2]?"^":".") + (bases[1]?">":".") + (rbi%10) + (runs%10);
	}
}
