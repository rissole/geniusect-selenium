package seleniumhelper.loginterpret;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.html.dom.HTMLDocumentImpl;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.InputSource;

/**
 * Pokemon Showdown battle log interpreter. Provides functions to assist in analysing
 * and reading a battle log.<br/>
 * Initialise it with a String - the battle log HTML (div.battle-log > div.inner).<br/>
 * Update it as the battle updates with <code>setLogText</code><br/>
 * Note this can also be used on arbitrary strings, for example, a log saved in a text file.
 * @author burse
 */
public class BattleLog {
	
	// Battle log text stripped of HTML and chat messages
	private String battleLogText;
	
	// unstripped HTML
	private String battleLogHTML; 
	
	/**
	 * Creates a new battle log interpreter.
	 * @param logHTML HTML of the battle log element (css=div.battle-log > div.inner).
	 */
	public BattleLog(String logHTML) {
		setLogText(logHTML);
	}
	
	public void setLogText(String logHTML) {
		battleLogHTML = logHTML;
		battleLogText = stripHTML(getLogHTML(true));
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
		Pattern p = Pattern.compile("^Turn (\\d+)$", Pattern.MULTILINE);
		Matcher m = p.matcher(cTT);
		if (m.find()) {
			turnString = m.group(1);
		}
		else {
			return 0;
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
	 * @return String - battle log text, including new lines, CHAT MESSAGES STRIPPED.
	 */
	public String getLogText() {
		return battleLogText;
	}
	
	/**
	 * Gets the HTML in the Battle Log.
	 * @param stripChats - if true, chat messages will be stripped from the returned HTML.
	 * @return String - battle log in raw HTML form.
	 */
	public String getLogHTML(boolean stripChats) {
		if (!stripChats) {
			return battleLogHTML;
		}
		else {
			return battleLogHTML.replaceAll("<div class=\"chat\">.*?</div>", "");
		}
	}
	
	/**
	 * Returns true if the battle log contains the specified string.
	 * @param s The String to check for.
	 * @param ignoreChats Set to true to ignore player chat messages.
	 * @return True if and only if the battle log contains the string specified.
	 */
	public boolean contains(String s, boolean ignoreChats) {
		if (!ignoreChats) {
			return (getLogText().lastIndexOf(s) != -1);
		}
		else {
			return (stripHTML(getLogHTML(true)).lastIndexOf(s) != -1);
		}
	}
	
	/**
	 * Gets the text of the current turn we are in.
	 * @return String - current turn text, IGNORING CHAT MESSAGES, or empty string if a turn hasn't started yet.
	 */
	public String getCurrentTurnText() {
		String battleText = getLogHTML(true);
		int currentTurnIdx = battleText.lastIndexOf("<h2>Turn ");
		if (currentTurnIdx == -1) {
			return "";
		}
		else {
			return stripHTML(battleText.substring(currentTurnIdx));
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
		
		String battleText = getLogHTML(true);
		int turnIdx;
		if (turn == 0) {
			turnIdx = battleText.indexOf("<h2>Turn 1</h2>");
			return stripHTML(battleText.substring(0, turnIdx));
		}
		String turnStr = "<h2>Turn " + turn + "</h2>";
		turnIdx = battleText.indexOf(turnStr);
		int nextTurnIdx = battleText.indexOf("<h2>Turn", turnIdx+turnStr.length());
		if (turnIdx == -1 || nextTurnIdx == -1) {
			return "";
		}
		else {
			return stripHTML(battleText.substring(turnIdx, nextTurnIdx));
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
			Pattern p = Pattern.compile("^"+sentOutStr+"(\\w+|[\\w ]+ \\(\\w+\\))!$", Pattern.MULTILINE | Pattern.DOTALL);
			Matcher m = p.matcher(text);
			if (m.find()) {
				String nameWithPossibleNickname = m.group(1);
				if (resolveNickname) {
					return getNameFromPossibleNickname(nameWithPossibleNickname);
				}
				else {
					if (nameWithPossibleNickname.contains("(")) {
						return substringToFirst(nameWithPossibleNickname, 0, " (");
					}
					return nameWithPossibleNickname;
				}
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
		return (substringToFirst(blt, blt.indexOf("Format: ")+8, "\n"));
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
			if (lines[i].startsWith("Battle between")) {
				break;
			}
			if (lines[i].indexOf("Clause") != -1) {
				clauses.add(substringToFirst(lines[i],0,":"));
			}
			else if (clauses.size() != 0) {
				break;
			}
		}
		return clauses;
	}
	
	/**
	 * Strips the HTML of the specified string, with new lines between each element.
	 * logHTML should be innerHTML of some battle log element.
	 */
	private String stripHTML(String logHTML) {
		HTMLDocument document = new HTMLDocumentImpl();
		DocumentFragment doc;
		DOMFragmentParser parser = new DOMFragmentParser();
		
		doc = document.createDocumentFragment();
		InputSource inputSource = new InputSource( new ByteArrayInputStream( logHTML.getBytes() ) );

		try {
			parser.parse(inputSource, doc);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		
		StringBuffer text = new StringBuffer();
		NodeList list = doc.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getTextContent().trim().length() > 0)
				text.append(node.getTextContent() + "\n");
		}
		return text.toString();
	}
	
	/**
	 * Gets the names of the players from the battle log.
	 * @return String Array - player's names, or null if the log doesn't contain the necessary line
	 * ("Battle between (player1) and (player2) started!")
	 */
	public String[] getPlayerNames() {
		Pattern p = Pattern.compile("^Battle between (.+?) and (.+?) started!$", Pattern.MULTILINE);
		Matcher m = p.matcher(getLogText());
		if (m.find()) {
			String[] names = new String[2];
			names[0] = m.group(1);
			names[1] = m.group(2);
			return names;
		}
		return null;
	}
}
