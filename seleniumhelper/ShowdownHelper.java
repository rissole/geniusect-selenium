package seleniumhelper;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.json.*;

/**
 * Selenium helper functions specifically for Pokemon Showdown.
 * @author burse
 */
public class ShowdownHelper extends Helper {
	
	// Base URL of Pokemon Showdown server.
	private String rootURL;
	private String currentUser;
	
	/**
	 * Creates an instance of helper functions for Pokemon Showdown automation
	 * @param driver The WebDriver that will be used for automation
	 * @param rootURL The base URL of Pokemon Showdown server, can be changed to alter what server the
	 * automation will run on. No trailing forward slash eg. http://play.pokemonshowdown.com
	 */
	public ShowdownHelper(WebDriver driver, String rootURL) {
		super(driver);
		this.rootURL = rootURL;
		this.currentUser = "";
	}
	
	/**
	 * Creates an instance of helper functions for Pokemon Showdown automation at http://play.pokemonshowdown.com
	 * @param driver The WebDriver that will be used for automation
	 */
	public ShowdownHelper(WebDriver driver) {
		super(driver);
		rootURL = "http://play.pokemonshowdown.com";
		this.currentUser = "";
	}
	
	/**
	 * Opens the showdown homepage.
	 */
	public void open() {
		driver.get(rootURL);
	}
	
	//// Lobby functions
	
	/**
	 * Logs in to Showdown with the supplied information.
	 * Make password empty string if you wish to use an unregistered account.
	 */
	public void login(String userName, String password) {
		waitForElementPresent(By.xpath("//button[contains(text(),'Choose name')]"), 20);
		
	    driver.findElement(By.xpath("//button[contains(text(),'Choose name')]")).click();
	    driver.findElement(By.id("overlay_name")).clear();
	    driver.findElement(By.id("overlay_name")).sendKeys(userName);
	    driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
	    
	    if (password.length() > 0) {
		    waitForElementPresent(By.id("overlay_password"));
		    
		    driver.findElement(By.id("overlay_password")).clear();
		    driver.findElement(By.id("overlay_password")).sendKeys(password);
		    driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
	    }
	    currentUser = userName;
	}
	
	/**
	 * Attempts to find a battle of the specified format using the specified team.
	 * @param format The format you want to search for.
	 * @param team The team you are using. Ignored for random battle.
	 */
	public void findBattle(String format, String team) {
		dropdownSelect(By.id("lobby-format"), format);
		sleep(500);
		if (isElementPresent(By.id("lobby-team"))) {
			dropdownSelect(By.id("lobby-team"), team);
			sleep(500);
		}
		clickAt(By.id("lobby-gobutton"));
	}
	
	/**
	 * Waits for a battle to begin. Times out after two minutes.
	 */
	public void waitForBattleStart() {
		waitForElementPresent(By.cssSelector("div.whatdo"), 120);
		sleep(500);
	}
	
	/**
	 * Gets the current player's name.
	 * @return String - our user name,  or empty string if not logged in.
	 */
	public String getUserName() {
		return currentUser;
	}
	
	//// Battle functions
	
	/**
	 * Gets the opponent's user name.
	 * @return Opponent's user name, or empty string on failure.
	 */
	public String getOpponentName() {
		List<WebElement> allElements = driver.findElements(By.cssSelector("div.trainer"));
		for (WebElement e : allElements) {
			if (!e.getText().equals(getUserName())) {
				return e.getText();
			}
		}
		return "";
	}
	
	/**
	 * Leaves the current battle (click the close room button)
	 */
	public void leaveBattle() {
		WebElement currentTab = driver.findElement(By.xpath("//a[contains(@class, 'tab battletab cur')]"));
		currentTab.findElement(By.xpath("//span[contains(@class, 'close')]")).click();
		sleep(500);
		if (isElementPresent(By.cssSelector("button[type=\"submit\"]"))) {
			clickAt(By.cssSelector("button[type=\"submit\"]"));
		}
	}
	
	/**
	 * Surrenders the current battle.
	 */
	public void surrender() {
		sendMessage("/surrender");
		waitForBattleLogContains(getOpponentName() + " won the battle!");
	}
	
	/**
	 * Presses "Kick Inactive Player" button
	 */
	public void kickInactivePlayer() {
		if (isElementPresent(By.cssSelector("div.replay-controls > button"))) {
			clickAt(By.cssSelector("div.replay-controls > button"));
		}
	}
	
	/**
	 * Sends a message in a battle and logs it in the console.
	 * @param message The message to send
	 */
	public void sendMessage(String message) {
		sendMessage(message, true);
	}
	
	/**
	 * Sends a message in a battle
	 * @param message The message to send
	 * @param logToConsole If true, the message will also be written to the console.
	 */
	public void sendMessage(String message, boolean logToConsole) {
		WebElement chatbox = driver.findElement(By.xpath("(//textarea[@type='text'])[2]"));
		chatbox.clear();
		chatbox.sendKeys(message);
		chatbox.sendKeys(Keys.RETURN);
		if (logToConsole) {
			System.out.println("[BATTLE LOG] " + getUserName() + ": " + message);
		}
	}
	
	public enum TurnEndStatus {
		UNKNOWN,
		ATTACK,
		SWITCH,
		WON,
		LOST
	}
	
	/**
	 * Waits for the next turn to begin or the battle to end, kicking inactive players.
	 * @param kickAfterSeconds Clicks 'Kick inactive player' after this number of seconds. 
	 * Set to 0 to never kick
	 * @return TurnEndStatus indicator.
	 */
	public TurnEndStatus waitForNextTurn(int kickAfterSeconds) {
		int waited = 0;
		boolean gameOver = false;
		while (!isElementPresent(By.cssSelector("div.whatdo")) && !gameOver) {
			if (kickAfterSeconds != 0 && waited >= kickAfterSeconds*1000) {
				kickInactivePlayer();
				kickAfterSeconds = 0;
			}
			sleep(500);
			waited += 500;
			gameOver = ((Long)javascript("return curRoom.battle.done;") > 0);
		}
		if (gameOver) {
			if (battleLogContains(getUserName() + " won the battle!")) {
				return TurnEndStatus.WON;
			}
			else {
				return TurnEndStatus.LOST;
			}
		}
		else {
			String whatdoText = driver.findElement(By.cssSelector("div.whatdo")).getText();
			if (whatdoText.contains("Switch " + getCurrentPokemon(false) + " to:")) {
				return TurnEndStatus.SWITCH;
			}
			else if (whatdoText.contains("What will " + getCurrentPokemon(false) + " do?")) {
				return TurnEndStatus.ATTACK;
			}
		}
		return TurnEndStatus.UNKNOWN;
	}
	
	/**
	 * Chooses the specified move to attack with.
	 * @param moveName The move to use.
	 * @throws NoSuchChoiceException if the specified move can't be found 
	 */
	public void doMove(String moveName) throws NoSuchChoiceException {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		try {
			moveMenu.findElement(By.xpath("//button[text()='"+moveName+"']")).click();
		}
		catch (NoSuchElementException e) {
			throw new NoSuchChoiceException("You do not have the move '"+moveName+"'");
		}
	}
	
	/**
	 * Gets the moves we currently have.
	 * @return String List - names of the moves we have
	 */
	public List<String> getMoves() {
		return getMoves(getCurrentPokemon(true));
	}
	
	/**
	 * Gets the name of usable moves we currently have.
	 * @return String List - names of the moves
	 */
	public List<String> getUsableMoves() {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		List<WebElement> moveButtons = moveMenu.findElements(By.tagName("button"));
		List<String> moves = new ArrayList<String>(4);
		for (WebElement e : moveButtons) {
			if (e.getAttribute("disabled") == null) {
				moves.add(substringToFirst(e.getText(), 0, "\n"));
			}
		}
		return moves;
	}
	
	/**
	 * Returns whether the specified move on the active Pokemon is usable.
	 * @param move The full move name.
	 * @return Boolean
	 * @throws NoSuchChoiceException If the active Pokemon does not have the specified move.
	 */
	public boolean isMoveUsable(String move) throws NoSuchChoiceException {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		try {
			return (moveMenu.findElement(By.xpath("//button[text()='"+move+"']")).getAttribute("disabled") == null);
		}
		catch (NoSuchElementException e) {
			throw new NoSuchChoiceException("You do not have the move '"+move+"'");
		}
	}
	
	/**
	 * Gets the moves the specified Pokemon [species], on our team, currently has.
	 * @param pokemon The Pokemon whose moves we want to retrieve
	 * @param getShortNames If true, shortnames will be returned. (ie "leechseed" not "Leech Seed")
	 * @return String List - names of the moves it has
	 */
	public List<String> getMoves(String pokemon, boolean getShortNames) {
		ArrayList<String> moves = new ArrayList<String>(6);
		int slot = getSlotForSpecies(pokemon);
		if (slot == -1)
			return moves;
		String pokeObj = "curRoom.battle.mySide.pokemon["+slot+"]";
		long numberMoves = javascript("return "+pokeObj+".moves.length");
		for (int i = 0; i < numberMoves; ++i) {
			if (getShortNames == false) {
				String moveName = (String)javascript("return Tools.getMove("+pokeObj+".moves[arguments[0]]).name",i);
				if (moveName.contains("Hidden Power")) {
					moveName = "Hidden Power";
				}
				moves.add(moveName);
			}
			else {
				moves.add((String)javascript("return "+pokeObj+".moves[arguments[0]]",i));
			}
		}
		return moves;		
	}
	
	/**
	 * Gets the moves the specified Pokemon [species], on our team, currently has.
	 * @param pokemon The Pokemon whose moves we want to retrieve
	 * @return String List - names of the moves it has
	 */
	public List<String> getMoves(String pokemon) {
		return getMoves(pokemon, false);
	}
	
	/**
	 * Gets the PP remaining for the move specified.
	 * @return PP remaining for specified move, throws exception if we don't have that move.
	 * @throws NoSuchChoiceException if the specified move can't be found
	 */
	public int getMoveRemainingPP(String move) throws NoSuchChoiceException {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		try {
			WebElement moveButton = moveMenu.findElement(By.xpath("//button[contains(text(),'"+move+"')]"));
			String[] moveInfo = moveButton.getText().split("\n");
			// moveInfo = [move name, type, pp/maxpp]
			return Integer.parseInt(substringToFirst(moveInfo[2], 0, "/"));
		}
		catch (Exception e) {
			throw new NoSuchChoiceException("You do not have the move '"+move+"'");
		}
	}
	
	/**
	 * Switches to the specified Pokemon.
	 * @param pokemon The name of the Pokemon we want to switch to.
	 * @param byNickname Set to false to switch by species name.
	 * @throws NoSuchChoiceException if the specified Pokemon can't be found
	 * @throws IsTrappedException if we are trapped
	 * @throws UnusableException if we can't switch to the specified Pokemon (it is fainted, for example)
	 */
	public void switchTo(String pokemon, boolean byNickname) 
			throws NoSuchChoiceException, IsTrappedException, UnusableException {
		WebElement switchToDiv = driver.findElement(By.cssSelector("div.switchmenu"));
		if (isTrapped()) {
			throw new IsTrappedException();
		}
		try {
			if (byNickname) {
				WebElement pokeButton = switchToDiv.findElement(By.xpath("//button[contains(text(),'"+pokemon+"')]"));
				if (pokeButton.getAttribute("disabled") != null) {
					throw new UnusableException("Pokemon "+pokemon+" cannot be switched to!");
				}
				pokeButton.click();
			}
			else {
				// try to identify which button has the species specified. oh god lol nvm it's ez
				switchTo(getSlotForSpecies(pokemon));
			}
		}
		catch (NoSuchElementException e) {
			throw new NoSuchChoiceException("You do not have the Pokemon '"+pokemon+"'");
		}
	}
	
	/**
	 * Switches to the Pokemon in slot specified
	 * @param slot It's the slot (0-5)
	 * @throws NoSuchChoiceException if the specified slot is invalid
	 * @throws IsTrappedException if we are trapped
	 * @throws UnusableException if we can't switch to the specified Pokemon (it is fainted, for example)
	 */
	public void switchTo(int slot) 
			throws NoSuchChoiceException, IsTrappedException, UnusableException {
		if (isTrapped()) {
			throw new IsTrappedException();
		}
		WebElement switchToDiv = driver.findElement(By.cssSelector("div.switchmenu"));
		List<WebElement> buttons = switchToDiv.findElements(By.tagName("button"));
		if (slot < 0 || slot >= buttons.size()) {
			throw new NoSuchChoiceException("Cannot swap to slot " + slot);
		}
		if (buttons.get(slot).getAttribute("disabled") != null) {
			throw new UnusableException("Pokemon in slot "+slot+" cannot be switched to!");
		}
		buttons.get(slot).click();
	}
	
	/**
	 * Returns whether the current Pokemon is trapped.
	 * @return True if cannot switch, false otherwise.
	 */
	public boolean isTrapped() {
		WebElement switchToDiv = driver.findElement(By.cssSelector("div.switchmenu"));
		return switchToDiv.getText().equals("You are trapped and cannot switch!");
	}
	
	/**
	 * Returns the slot on owner's team in which the Pokemon with the specified name is in.
	 * @param pokemon Name of Pokemon species
	 * @param owner Which team we are investigating
	 * @return Integer - 0-5, slot. -1 on error.
	 */
	private int getSlotForSpecies(String pokemon, String owner) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		for (int i = 0; i < 6; ++i) {
			String species = javascript("var p = curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return p.species;", side, i);
			if (pokemon.equals(species)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Gets the slot for the specified Pokemon species on our team.
	 * @param pokemon Name of Pokemon species
	 * @return Integer - 0-5, slot. -1 on error.
	 */
	private int getSlotForSpecies(String pokemon) {
		return getSlotForSpecies(pokemon, getUserName());
	}
	
	/**
	 * Gets a Pokemon's attribute via javascript.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @param attribute Which javascript attribute we are trying to get
	 * @return Object - returned Javascript object (may be null)
	 */
	private Object getPokemonAttribute(String pokemon, String owner, String attribute) {
		int slot = getSlotForSpecies(pokemon, owner);
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		return javascript("var p=curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return p[arguments[2]];", side, slot, attribute);
	}
	
	/**
	 * Returns the current status of the Pokemon.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return String - Pokemon status. 3 character abbreviation, lower case.
	 * Can return: <b>'tox', 'psn', 'frz', 'par', 'brn'.</b>
	 */
	public String getStatus(String pokemon, String owner) {
		return (String)getPokemonAttribute(pokemon, owner, "status");
	}
	
	/**
	 * Returns whether the Pokemon specified has the given volatile. A volatile is an effect which
	 * is usually removed on switch.
	 * @param pokemon The species name of the Pokemon
	 * @param owner  Which team the Pokemon is on
	 * @param _volatile String - volatile name.<br/>
	 * Known values: <b>formechange, leechseed, protect, magiccoat, yawn, confusion,
	 * airballoon, transform, substitute, taunt, encore, torment, stockpile{1,2,3}, perish{1,2,3}</b>
	 * @return
	 */
	public boolean hasVolatile(String pokemon, String owner, String _volatile) {
		int slot = getSlotForSpecies(pokemon, owner);
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		return (Boolean)javascript("var p=curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return p.hasVolatile(arguments[2]);", side, slot, _volatile);
	}
	
	/**
	 * Returns whether the specified Pokemon has fainted or not.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return True if fainted, false otherwise.
	 */
	public boolean isFainted(String pokemon, String owner) {
		return (Boolean)getPokemonAttribute(pokemon,owner,"fainted");
	}
	
	/**
	 * Returns the specified Pokemon's HP.
	 * <b>NOTE: This returns a percentage (0-100) if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.</b>
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return Integer - returns a percentage (0-100) if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.
	 */
	public int getHP(String pokemon, String owner) {
		return ((Long)getPokemonAttribute(pokemon,owner,"hp")).intValue();
	}
	
	/**
	 * Returns the specified Pokemon's Max HP.
	 * <b>NOTE: This returns 100 if the <code>owner</code> is the opponent; otherwise
	 * the exact max HP value.</b>
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return Integer - returns 100 if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.
	 */
	public int getMaxHP(String pokemon, String owner) {
		return ((Long)getPokemonAttribute(pokemon,owner,"maxhp")).intValue();
	}
	
	/**
	 * Returns the boosts of the Pokemon currently on <code>owner</code>'s side of the field.
	 * @param owner Which team the Pokemon is on
	 * @return Map - <code>(stat,boost)</code> pairs, <code>stat</code> being one of <b>atk, def, spa, spd, spe</b> and
	 * -6 <= <code>boost</code> <= 6.
	 */
	public Map<String,Integer> getBoosts(String owner) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		Map<String,Integer> boosts = new HashMap<String,Integer>();
		String jsonBoosts = (String)javascript("return JSON.stringify(curRoom.battle[arguments[0]].active[0].boosts);", side);
		try {
			JSONObject jo = new JSONObject(jsonBoosts);
			Iterator<String> itr = jo.keys();
			while (itr.hasNext()) {
				String k = itr.next();
				boosts.put(k, jo.getInt(k));
			}
		}
		catch (JSONException e) {
			return boosts;
		}
		return boosts;
	}
	
	//// Battle log functions
	
	/**
	 * Gets the substring, starting at startIndex, up to the first instance of 'stop'.
	 * @param s String to search
	 * @param startIndex Index to start from
	 * @param stop String to stop at
	 * @return The whole string if the 'stop' could not be found
	 */
	private String substringToFirst(String s, int startIndex, String stop) {
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
	public String getBattleLogText() {
		WebElement battleLog = driver.findElement(By.cssSelector("div.battle-log"));
		return battleLog.getText();
	}
	
	/**
	 * Returns true if the battle log contains the specified string.
	 * @param s The String to check for.
	 * @return True if and only if the battle log contains the string specified.
	 */
	public boolean battleLogContains(String s) {
		return (getBattleLogText().lastIndexOf(s) != -1);
	}
	
	/**
	 * Gets the text of the current turn we are in.
	 * @return String - current turn text, or empty string if a turn hasn't started yet.
	 */
	public String getCurrentTurnText() {
		String battleText = getBattleLogText();
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
		
		String battleText = getBattleLogText();
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
	
	private WebElement getTrainerDiv(String owner) {
		List<WebElement> allElements = driver.findElements(By.cssSelector("div.trainer"));
		for (WebElement e : allElements) {
			if (e.getText().equals(owner)) {
				return e;
			}
		}
		return null;
	}
	
	/**
	 * Returns species names of Pokemon on the specified team.
	 * @param owner Name of team's owner
	 * @return String List - Pokemon species names, or empty list on failure.
	 */
	public List<String> getTeam(String owner) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		List<String> team = new ArrayList<String>(6);
		for (int i = 0; i < 6; ++i) {
			String poke = javascript("var p = curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return p.species;", side, i);
			if (poke != null) {
				team.add(poke);
			}
		}
		return team;
	}
	
	/**
	 * Returns species names of non-fainted Pokemon on the specified team.
	 * @param owner Name of team's owner
	 * @return String List - Pokemon species names, or empty list on failure.
	 */
	public List<String> getAliveTeam(String owner) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		List<String> team = new ArrayList<String>(6);
		for (int i = 0; i < 6; ++i) {
			String poke = javascript("var p = curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null && !p.fainted) return p.species;", side, i);
			if (poke != null) {
				team.add(poke);
			}
		}
		return team;
	}
	
	/**
	 * Returns species names of Pokemon we can switch to.
	 * @param owner Name of team's owner
	 * @return String List - Pokemon species names, or empty list on failure.
	 */
	public List<String> getSwitchableTeam() {
		List<String> team = getAliveTeam(getUserName());
		team.remove(getCurrentPokemon(true));
		return team;
	}
	
	/**
	 * Waits until the battle log contains the specified text (up to 5 minutes)
	 * @param message String to wait for.
	 */
	public void waitForBattleLogContains(final String message) {
		(new WebDriverWait(driver, 300)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return battleLogContains(message);
            }
        });
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
	 * Returns what Pokemon was last sent out on owner's side of the field.
	 * <b>NOTE: THIS HAS PROBLEMS WITH ZOROARK</b> (as does Showdown itself)
	 * @param owner Whose side of the field we are checking
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentPokemon(String owner, boolean resolveNickname) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		String pokeField = "name";
		if (resolveNickname) {
			pokeField = "species";
		}
		
		// this is probably bad
		String command = 
				("if (curRoom.battle.%side.active[0] != null)\n" +
				"	return curRoom.battle.%side.active[0].%field;\n" +
				"else if (curRoom.battle.%side.lastPokemon != null)\n" + 
				"	return curRoom.battle.%side.lastPokemon.%field;\n"+
				"return '';")
				.replace("%side",side).replace("%field",pokeField);
		String result = javascript(command);
		return result;
	}
	
	/**
	 * Returns what Pokemon was last sent out on our side of the field.
	 * <b>NOTE: THIS HAS PROBLEMS WITH ZOROARK</b> (as does Showdown itself)
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentPokemon(boolean resolveNickname) {
		return getCurrentPokemon(getUserName(), resolveNickname);
	}
	
	/**
	 * Returns what Pokemon was last sent out on the opponent's side of the field.
	 * <b>NOTE: THIS HAS PROBLEMS WITH ZOROARK</b> (as does Showdown itself)
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentOpponentPokemon(boolean resolveNickname) {
		return getCurrentPokemon(getOpponentName(), resolveNickname);
	}
	
	/**
	 * Gets the format of the game we are currently in.
	 * @return String - Format, eg "OU (current)"
	 */
	public String getFormat() {
		String blt = getBattleLogText();
		return (substringToFirst(blt, blt.indexOf("Format: ")+8, "\n"));
	}
	
	/**
	 * Gets the clauses that are active in the game we are currently in.
	 * @return String List - Clauses, eg "Sleep Clause"
	 */
	public List<String> getClauses() {
		String blt = getTurnText(0);
		ArrayList<String> clauses = new ArrayList<String>();
		int idx = 0;
		while ((idx=blt.indexOf("Rule: ",idx)) != -1) {
			clauses.add(substringToFirst(blt,idx+6,"\n"));
			idx += 6;
		}
		return clauses;
	}
}
