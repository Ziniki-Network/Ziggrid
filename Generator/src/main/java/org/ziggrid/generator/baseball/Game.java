package org.ziggrid.generator.baseball;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.ziggrid.api.AnalyticItem;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.generator.baseball.InningState.RunnerAdvances;
import org.zinutils.csv.CSVLine;
import org.zinutils.exceptions.UtilException;

public class Game {
	String gameid;
	String visteam;
	String hometeam;
	private GameIterator it;
	String season;
	int gameDate;
	private String winPitcher;
	private String winningTeam;
	private int awayRuns;
	private int homeRuns;
	private List<AnalyticItem> allItems = new ArrayList<AnalyticItem>();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
	private List<StoreableObject> players = new ArrayList<StoreableObject>();

	// for unit tests
	Game() {
	}

	public Game(GameIterator it) {
		this.it = it;
		while (true) {
			CSVLine l = it.nextLine();
			if (l == null)
				return;
			String cmd = l.get(0);
			String opt = l.get(1);
			if (cmd.equals("id")) {
				gameid = opt;
				season = opt.substring(3, 7);
			} else if (cmd.equals("version")) {
				if (!opt.equals("2"))
					throw new UtilException("Version " + opt + " not supported");
			} else if (cmd.equals("info")) {
				if (opt.equals("visteam"))
					visteam = l.get(2);
				else if (opt.equals("hometeam"))
					hometeam = l.get(2);
				else if (opt.equals("wp"))
					winPitcher = l.get(2);
				else if (opt.equals("date")) {
					try {
						Date d = sdf.parse(l.get(2));
						Calendar cal = Calendar.getInstance();
						cal.setTime(d);
						gameDate = cal.get(Calendar.DAY_OF_YEAR);
					} catch (Exception ex) {
						throw UtilException.wrap(ex);
					}
				}
			} else if (cmd.equals("start")) {
				playerId(l.get(1), l.get(3));
				try {
					players.add(newProfile(season, l.get(1), l.get(2)));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else
				break;
			it.usedLine();
		}
	}

	private StoreableObject newProfile(String season, String evCode, String fullName) throws JSONException {
		AnalyticItem ret = new AnalyticItem("rosterPlayer", it.root.nextId(), it.timer.notch());
		ret.set("season", season);
		ret.set("player", evCode);
		ret.set("fullname", fullName);
		return ret;
	}

	private void playerId(String id, String team) {
		if (winPitcher == null)
			throw new UtilException("No winning pitcher has been set");
		if (id.equals(winPitcher))
			winningTeam = team;
	}

	public boolean hasEvent() {
		while (true) {
			CSVLine nextLine = it.nextLine();
			if (nextLine == null)
				return false;
			String cmd = nextLine.get(0);
			if (cmd.equals("id"))
				return false;
			else if (cmd.equals("play"))
				return true;
			else if (cmd.equals("sub"))
				playerId(nextLine.get(1),nextLine.get(3));
			it.usedLine();
		}
	}

	public void nextEvent(InningState state) {
		boolean recoveryMode = false;
		String currentInning = "1";
		String currentHalf = "0";
		boolean found = false;
		while (!found) {
			if (!hasEvent()) {
				return;
			}
			CSVLine play = it.nextLine();
			it.usedLine();
			if (recoveryMode) {
				if (currentInning.equals(play.get(1)) && currentHalf.equals(play.get(2))) {
					BaseballFactory.logger.info("Recovery ignoring " + play);
					continue;
				}
				state.skipTo(Integer.parseInt(play.get(1)), play.get(2).equals("1"));
				recoveryMode = false;
			}
			currentInning = play.get(1);
			currentHalf = play.get(2);
			try {
				if (!play.get(1).equals(Integer.toString(state.inning)))
					throw new UtilException("We lost track of the inning");
				else if (play.get(2).equals("1") != state.half)
					throw new UtilException("We lost track of the half-inning");
				String outcome = parseAction(play.get(6), state);
				if (outcome == null)
					continue;
				found = true;

				AnalyticItem ai = new AnalyticItem("plateAppearance", it.root.nextId(), it.timer.notch());
				ai.set("season", season);
				ai.set("dayOfYear", gameDate);
				ai.set("inning", play.get(1));
				ai.set("home", play.get(2).equals("1"));
				ai.set("team", team(play.get(2)));
				ai.set("outs", state.outs);
				ai.set("player", play.get(3));
				ai.set("bases", (state.bases[1]?"1":"")+(state.bases[2]?"2":"")+(state.bases[3]?"3":""));
				ai.set("awayScore", awayRuns);
				ai.set("homeScore", homeRuns);
				ai.set("action", outcome);
				ai.set("rbi", state.rbi);
				allItems.add(ai);
				if (currentHalf.equals("1"))
					homeRuns += state.runs;
				else
					awayRuns += state.runs;
			} catch (Exception ex) {
				BaseballFactory.logger.error(ex.getMessage());
				BaseballFactory.logger.info("Recovering after failure in parsing " + play);
				recoveryMode = true;
			}
		}
	}

	//	private static final String[] actions = new String[] { "BB", "1B", "2B", "HR", "P", "F", "K" }; 

	String parseAction(String outcome, InningState state) {
		if (outcome.equals("NP"))
			return null;
		
		outcome = outcome.replaceAll("[#!?]", "");
		String first = outcome;
		String modifiers = null;
		int paren = outcome.indexOf('(');
		int slash = outcome.indexOf('/');
		int dot = outcome.indexOf('.');
		if (dot != -1 && slash > dot)  // since runners can include modifiers, seeing a slash later than . is not a regular modifier
			slash = -1;
		else while (paren != -1 && slash > paren) {
			// make sure we find a slash after the close paren
			int close = outcome.indexOf(')', paren);
			paren = outcome.indexOf('(', close);
			slash = outcome.indexOf('/', close);
		}
		if (slash != -1) {
			first = outcome.substring(0, slash);
			if (dot != -1) {
				modifiers = outcome.substring(slash, dot);
			} else
				modifiers = outcome.substring(slash);
		} else if (dot != -1)
			first = outcome.substring(0, dot);
		
		String action = null;
		char c = first.charAt(0);
		int idx = 0;
		boolean returnNull = false;
		int requireBlanked = -1;
		boolean doAdvance = true;
		boolean conditionalAdvanceOnDotB = false;
		if (c < '1' || c > '9') {
			if (first.equals("HR")) { action = "HR"; idx = 2; }
			else if (first.equals("HP")) { action = "HBP"; idx = 2; }
			else if (first.equals("IW")) { action = "BB"; idx = 2; }
			else if (first.matches("DGR[0-9]?")) { action = "2B"; idx = 3; }
			else if (first.matches("FLE2")) { returnNull = true; idx = first.length(); }
			else if (first.matches("FC[0-9]?")) {
				idx = first.length();
				action = "FC";
			} else if (first.matches("SB[23]")) {
				if (slash != -1)
					throw new UtilException("Can't handle that");
				if (dot == -1) {
					int toBase = first.charAt(2)-'0';
					state.moveRunner(toBase-1, toBase, RunnerAdvances.Safely, true);
				}
				idx = first.length();
				returnNull = true;
			} else if (first.matches("SB3;SB2")) {
				if (slash != -1)
					throw new UtilException("Can't handle that");
				if (dot == -1) {
					state.moveRunner(2, 3, RunnerAdvances.Safely, true);
					state.moveRunner(1, 2, RunnerAdvances.Safely, true);
				}
				idx = first.length();
				returnNull = true;
			} else if (first.equals("DI")) {
				if (dot == -1)
					throw new UtilException("Need DOT portion for DI");
				if (slash != -1)
					throw new UtilException("DI cannot have modifiers");
				idx = first.length();
				returnNull = true;
			} else if (first.matches("(PO)?CS[23H](\\([0-9E]+\\))?")) {
				if (slash != -1)
					throw new UtilException("Can't handle that");
				int ch = 2;
				if (first.startsWith("PO"))
					ch = 4;
				int toBase = baseNo(first.charAt(ch));
				RunnerAdvances res = RunnerAdvances.Erased;
				if (first.contains("E"))
					res = RunnerAdvances.Safely;
				if (dot == -1) {
					state.moveRunner(toBase-1, toBase, res, false);
					state.advanceGame("CS");
				} else if (res == RunnerAdvances.Erased)
					requireBlanked = toBase-1;
				idx = first.length();
				returnNull = true;
			} else if (first.matches("PO[123](\\(E?[0-9]+(/TH)?\\))?")) {
				if (slash != -1)
					throw new UtilException("Can't handle that");
				if (dot == -1) {
					int toBase = first.charAt(2)-'0';
					state.moveRunner(toBase, -1, RunnerAdvances.Erased, false);
					state.advanceGame("CS");
				}
				idx = first.length();
				returnNull = true;
			} else if (first.matches("WP") || first.matches("BK") || first.matches("PB") || first.equals("OA")) {
				idx = first.length();
				returnNull = true;
			} else if (first.matches("E[0-9]*")) { action = "E"; idx=first.length(); }
			else {
				switch (c) {
				case 'S' : action = "1B"; break;
				case 'D' : action = "2B"; break;
				case 'T' : action = "3B"; break;
				case 'W' : action = "BB"; break;
				case 'K' : action = "K" ; break;
				case 'C' : action = "CI"; break;
				default: throw new UtilException("Cannot understand what happened: " + c + " in " + first);
				}
				idx++;
			}
		}
		while (idx < first.length()) {
			c = first.charAt(idx++);
			if (c == '+') {
				if (first.substring(idx).matches("SB[23]")) {
					if (slash != -1 || dot != -1)
						throw new UtilException("Can't handle that");
					int toBase = first.charAt(idx+2)-'0';
					state.moveRunner(toBase-1, toBase, RunnerAdvances.Safely, true);
					idx = first.length();
				} else if (first.substring(idx).matches("CS[23]\\([0-9]*\\)")) {
					action = "K";
					if (dot != -1)
						throw new UtilException("Can't handle that");
					int toBase = first.charAt(idx+2)-'0';
					state.moveRunner(toBase-1, toBase, RunnerAdvances.Erased, true);
					idx = first.length();
				} else if (first.substring(idx).matches("WP") || first.substring(idx).matches("PB")) {
					conditionalAdvanceOnDotB = true;
					idx = first.length();
				} else
					throw new UtilException("Can't handle + notation");
			} else if (c == '(') {
				int tmp = first.indexOf(')', idx);
				String ev = first.substring(idx, tmp);
				idx = tmp+1;
				if (!ev.matches("[B123]"))
					throw new UtilException("Cannot parse '" + ev +"' as a base to erase");
				if (!ev.equals("B")) {
					state.moveRunner(ev.charAt(0)-'0', -1, RunnerAdvances.Erased, false);
				}
			} else if (c == 'E')
				action = "E";
			else if (c < '1' || c > '9')
				throw new UtilException("Cannot understand first portion: " + first.substring(idx-1));
		}
		while (modifiers != null) {
			int next = modifiers.indexOf('/', 1);
			String me;
			if (next == -1) {
				me = modifiers.substring(1);
				modifiers = null;
			} else {
				me = modifiers.substring(1, next);
				modifiers = modifiers.substring(next);
			}
			if (me.equals("SH"))
				action = ifNull(action, "SB");
			else if (me.equals("SF"))
				action = ifNull(action, "SF");
			else if (me.equals("FO")) {
				if (action == null || action.equals("P") || action.equals("G") || action.equals("FC") || action.equals("SF"))
					action = "FC";
			} else if (me.equals("BG"))
				action = ifNull(action, "G");
			else if (me.equals("BF"))
				action = ifNull(action, "F");
			else if (me.matches("BP[1-9]?F?"))
				action = ifNull(action, "P");
			else if (me.matches("NDP"))
				; // not clear what this means
			else if (me.equals("BR"))
				; // runner hit by batted ball
			else if (me.matches("B?[FGL]?DP")) {
				if (action == null || action.equals("P") || action.equals("G") || action.equals("FC") || action.equals("SF"))
					action = "DP";
				else if (action.equals("K"))
					; // strike-em-out/throw-em-out
				else
					throw new UtilException("Possible inconsistency on DP");
			}
			else if (me.matches("[FGLP][0-9]*L?[MSD]?D?F?W?[-+]?"))
				action = ifNull(action, me.substring(0,1));
			else if (me.equals("FINT"))
				; // Fan Interference?
			else if (me.equals("INT"))
				; // Interference
			else if (me.matches("E[123]") && action != null && action.equals("CI"))
				; // Catcher Interference
			else if (me.matches("R[1-9]"))
				; // Relay throw
			else if (me.equals("REV"))
				; // Play was reviewed?
			else if (me.equals("OBS"))
				; // bunt single?
			else if (me.matches("[0-9]+"))
				;  // location
			else if (me.matches("TH[1-9]?"))
				;  // throw home (error)
			else if (me.equals("IF"))
				;  // infield fly rule in effect
			else
				throw new UtilException("Cannot understand modifier " + me);
		}
		
		if (action == null && !returnNull)
			throw new UtilException("unknown outcome " + outcome + " : " + first + " //// " + modifiers);
		
		boolean batterPlaced = false;
		if (dot != -1) {
			state.dotAdvance();
			String movers = outcome.substring(dot+1);
			while (movers.length() > 0) {
				if (movers.length() < 3)
					throw new UtilException("Cannot understand explicit movement: " + movers);
				RunnerAdvances advanced = RunnerAdvances.Safely;
				int from = getBase(movers, 0);
				if (from == 0 && conditionalAdvanceOnDotB)
					doAdvance = false;
				if (requireBlanked == from)
					requireBlanked = -1;
				if (movers.charAt(1) == 'X')
					advanced = RunnerAdvances.Erased;
				else
					checkChar(movers, 1, '-');
				int to = getBase(movers, 2);
				if (to == requireBlanked) {
					requireBlanked = 0; // a sort of hack ... we do still want outs++ but don't want to clear the bag that someone has just fetched up on
				}
				if (from == 0)
					batterPlaced = true;
				movers = movers.substring(3);
				boolean creditRbis = (action != null);
				while (movers.length() > 0) {
					char m = movers.charAt(0);
					if (m == ';') {
						movers = movers.substring(1);
						break;
					} else if (m == '(') {
						int mi = movers.indexOf(')');
						if (mi == -1)
							throw new UtilException("'(' without ')' in " + outcome.substring(dot+1));
						String parm = movers.substring(1, mi);
						if (parm.matches("[0-9]*E[0-9](/TH[123H]?)?")) {
							if (advanced == RunnerAdvances.Erased) {
								advanced = RunnerAdvances.Safely;
								state.errorOuts++;
							}
						} else if (parm.matches("[0-9]*")) {
							; // who cares
						} else if (parm.matches("T?UR")) {
							;
						} else if ("NR".equals(parm) || "NORBI".equals(parm))
							creditRbis = false;
						else if (parm.matches("TH[123H]?"))
							; // throw home allowing runners to advance
						else 
							throw new UtilException("Could not handle movement parameter: " + parm); 
						movers = movers.substring(mi+1);
					} else
						throw new UtilException("Unexpected char '" + m + "' when processing args for " + outcome.substring(dot+1));
				}
				state.moveRunner(from, to, advanced, creditRbis);
			}
			if (requireBlanked != -1) {
				state.bases[requireBlanked] = false;
				state.outs++;
			}
			state.doneDotAdvance();
			if (!batterPlaced && action != null)
				state.placeBatter(action);
		} else if (action != null) { // need default runners
			state.moveUp(action);
		}

		if (doAdvance)
			state.advanceGame(action);
		
		return action;
	}

	private int baseNo(char b) {
		if (b == 'H')
			return 4;
		else if (b == '3')
			return 3;
		else if (b == '2')
			return 2;
		else if (b == '1')
			return 1;
		else
			throw new UtilException("There is no base '" + b + "'");
	}

	private String ifNull(String action, String other) {
		return action == null ? other : action;
	}

	public int getBase(String movers, int pos) {
		char c = movers.charAt(pos);
		if (c >='1' && c <='3')
			return c-'0';
		else if (c == 'H')
			return 4;
		else if (c == 'B')
			return 0;
		else
			throw new UtilException("Could not interpret " + c + " as a base in " + movers);
	}

	private void checkChar(String str, int i, char c) {
		if (str.charAt(i) != c)
			throw new UtilException("Expected " + c + " but found " + str.charAt(i) + " in " + str);
	}

	private String team(String which) {
		if (which.equals("0"))
			return visteam;
		return hometeam;
	}

	public List<AnalyticItem> events() {
		if (winningTeam == null)
			throw new UtilException("No winning team was determined");
		for (AnalyticItem ai : allItems) {
			if (!ai.is("plateAppearance"))
				continue;
			if (((Boolean)ai.get("home") && winningTeam.equals("1")) ||
				(!(Boolean)ai.get("home") && winningTeam.equals("0")))
				ai.set("winning", true);
			else
				ai.set("winning", false);
		}
		return allItems;
	}
	
	public void reportDate() {
		AnalyticItem ai = new AnalyticItem("gameDate", it.root.nextId(), it.timer.notch());
		ai.set("season", season);
		ai.set("day", gameDate);
		allItems.add(ai);
	}

	public void addTeamEvents() {
		if (winningTeam == null)
			throw new UtilException("No winning team was determined");
		AnalyticItem ai = new AnalyticItem("gameResult", it.root.nextId(), it.timer.notch());
		ai.set("season", season);
		ai.set("day", gameDate);
		if (winningTeam.equals("1")) {
			ai.set("winteam", hometeam);
			ai.set("winruns", homeRuns);
			ai.set("loseteam", visteam);
			ai.set("loseruns", awayRuns);
		} else {
			ai.set("winteam", visteam);
			ai.set("winruns", awayRuns);
			ai.set("loseteam", hometeam);
			ai.set("loseruns", homeRuns);
		}
		allItems.add(ai);
	}

	public List<StoreableObject> players() {
		return players;
	}
	
	@Override
	public String toString() {
		return gameDate + " " + visteam + " " + hometeam;
	}
}
