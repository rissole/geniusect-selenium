package seleniumhelper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pokemon Showdown battle log interpreter. Provides functions to assist in analysing
 * and reading a battle log.<br/>
 * Initialise it with a String - the battle log text.<br/>
 * Update it as the battle updates with <code>setLogText</code><br/>
 * Note this can also be used on arbitrary strings, for example, a log saved in a text file.
 * @author burse
 */
public class BattleLog {
	
	private String battleLogText;
	
	public BattleLog(String logText) {
		setLogText(logText);
	}
	
	public void setLogText(String logText) {
		battleLogText = logText;
	}
	
	/**
	 * Gets the substring, starting at startIndex, up to the first instance of 'stop'.
	 * @param s String to search
	 * @param startIndex Index to start from
	 * @param stop String to stop at
	 * @return The whole string if the 'stop' could not be found
	 */
	public static String substringToFirst(String s, int startIndex, String stop) {
		int idx = s.indexOf(stop, startIndex);
		if (idx == -1) {
			return s;
		}
		return s.substring(startIndex, idx);
	}
	
	/**
	 * Gets the latest turn in the current battle.
	 * @return Integer - the turn, or 0 if a turn has not been started.
	 */
	public int getCurrentTurn() {
		String cTT = getCurrentTurnText();
		if (cTT == "") {
			return 0;
		}
		
		String turnString = "";
		if (Pattern.matches("^Turn (\\d+)$", cTT)) {
			turnString = cTT.substring(5);
		}
		else {
			turnString = substringToFirst(cTT, 5, "\n");
		}
		
		try {
			return Integer.parseInt(turnString);
		}
		catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * Gets the whole text in the Battle Log.
	 * @return String - battle log text, including new lines.
	 */
	public String getLogText() {
		return battleLogText;
	}
	
	/**
	 * Returns true if the battle log contains the specified string.
	 * @param s The String to check for.
	 * @return True if and only if the battle log contains the string specified.
	 */
	public boolean contains(String s) {
		return (getLogText().lastIndexOf(s) != -1);
	}
	
	/**
	 * Gets the text of the current turn we are in.
	 * @return String - current turn text, or empty string if a turn hasn't started yet.
	 */
	public String getCurrentTurnText() {
		String battleText = getLogText();
		int currentTurnIdx = battleText.lastIndexOf("Turn ");
		if (currentTurnIdx == -1) {
			return "";
		}
		else {
			return battleText.substring(currentTurnIdx);
		}
	}
	
	/**
	 * Gets the text of the last turn we were in.
	 * Turn 0 is considered to be the initial announcement of team, format, etc.
	 * @return String - last turn text, or empty string if a turn hasn't been completed yet.
	 */
	public String getLastTurnText() {
		return getTurnText(getCurrentTurn()-1);
	}
	
	/**
	 * Gets the text of the specified turn.
	 * @param turn The turn. Turn 0 is considered to be the initial announcement of team, format, etc.
	 * @return String - the text from that turn, including "Turn (turn number)" heading
	 */
	public String getTurnText(int turn) {
		if (turn == getCurrentTurn()) {
			return getCurrentTurnText();
		}
		
		String battleText = getLogText();
		int turnIdx;
		if (turn == 0) {
			turnIdx = battleText.indexOf("Turn 1");
			return battleText.substring(0, turnIdx);
		}
		String turnStr = "Turn " + turn;
		turnIdx = battleText.indexOf(turnStr);
		int nextTurnIdx = battleText.indexOf("Turn", turnIdx+turnStr.length());
		if (turnIdx == -1 || nextTurnIdx == -1) {
			return "";
		}
		else {
			return battleText.substring(turnIdx, nextTurnIdx);
		}
	}
	
	/**
	 * Takes an ambiguous name string and returns the Pokemon name.
	 * @param fullname either "Pokemon name" or "nickname (Pokemon name)"
	 * @return The original string if it doesn't contain brackets, else what is inside the brackets.
	 */
	private String getNameFromPossibleNickname(String fullname) {
		if (!fullname.contains("(")) {
			return fullname;
		}
		else {
			int idx = fullname.indexOf("(");
			return substringToFirst(fullname,idx+1,")");
		}
	}
	
	/**
	 * Returns what Pokemon was on owner's side of the field at the start of turn.
	 * @param owner Whose side of the field we are checking
	 * @param turn Which turn we are interested in
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentPokemonAtTurn(String owner, int turn, boolean resolveNickname) {
		String sentOutStr = owner + " sent out ";
		if (turn == 0) {
			turn = 1;
		}
		while (turn >= 0) {
			--turn;
			String text = getTurnText(turn);
			int idx = text.lastIndexOf(sentOutStr);
			String nameWithPossibleNickname = substringToFirst(text,idx+sentOutStr.length(),"!\n");
			if (idx != -1 && resolveNickname) {
				return getNameFromPossibleNickname(nameWithPossibleNickname);
			}
			else if (idx != -1) {
				if (nameWithPossibleNickname.contains("(")) {
					return substringToFirst(nameWithPossibleNickname, 0, " (");
				}
				return substringToFirst(nameWithPossibleNickname, 0, "!\n");
			}
		}
		return "";
	}
	
	/**
	 * Gets the format of the game we are currently in.
	 * @return String - Format, eg "OU (current)"
	 */
	public String getFormat() {
		String blt = getLogText();
		return (substringToFirst(blt, blt.indexOf("Format:\n")+8, "\n"));
	}
	
	/**
	 * Gets the clauses that are active in the game we are currently in.
	 * @return String List - Clauses, eg "Sleep Clause"
	 */
	public List<String> getClauses() {
		String blt = getTurnText(0);
		ArrayList<String> clauses = new ArrayList<String>();
		String[] lines = blt.split("\n");
		for (int i = 0; i < lines.length; ++i) {
			if (lines[i].lastIndexOf("Clause") != -1) {
				clauses.add(lines[i]);
			}
			else if (clauses.size() != 0) {
				break;
			}
		}
		return clauses;
	}
}
