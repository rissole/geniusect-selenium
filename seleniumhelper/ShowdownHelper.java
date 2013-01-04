package seleniumhelper;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

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
		By loginButtonBy = By.xpath("//button[contains(text(),'Choose name')]");
		waitForElementPresent(loginButtonBy, 20);
		
	    driver.findElement(loginButtonBy).click();
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
		waitForElementPresent(By.cssSelector("div.moveselect"), 120);
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
		clickAt(By.cssSelector("div.replay-controls > button"));
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
		KICKED,
		WON,
		LOST
	}
	
	/**
	 * Waits for the next turn to begin or the battle to end, kicking inactive players.
	 * @param kickAfterSeconds Clicks 'Kick inactive player' after this number of seconds. 
	 * Set to 0 to never kick
	 * @return Returns true if we are at the next turn, or false if the game ended (because we kicked them)
	 */
	public TurnEndStatus waitForNextTurn(int kickAfterSeconds) {
		int waited = 0;
		boolean haveWon = getBattleLogText().contains(getUserName() + " won the battle!");
		boolean haveLost = getBattleLogText().contains(getOpponentName() + " won the battle!");
		while (!isElementPresent(By.cssSelector("div.whatdo")) && !haveWon && !haveLost) {
			if (kickAfterSeconds != 0 && waited >= kickAfterSeconds) {
				kickInactivePlayer();
			}
			sleep(1000);
			waited += 1;
		}
		String whatdoText = driver.findElement(By.cssSelector("div.whatdo")).getText();
		if (haveWon) {
			if (getBattleLogText().contains(getOpponentName() + " lost because of their inactivity.")) {
				return TurnEndStatus.KICKED;
			}
			else {
				return TurnEndStatus.WON;
			}
		}
		else if (haveLost) {
			return TurnEndStatus.LOST;
		}
		else {
			if (whatdoText.equals("Switch " + getCurrentPokemon(false) + " to:")) {
				return TurnEndStatus.SWITCH;
			}
			else if (whatdoText.equals("What will " + getCurrentPokemon(false) + " do? ")) {
				return TurnEndStatus.ATTACK;
			}
		}
		return TurnEndStatus.UNKNOWN;
	}
	
	/**
	 * Chooses the specified move to attack with.
	 * @param moveName The move to use.
	 * @throws Exception if the specified move can't be found 
	 */
	public void doMove(String moveName) throws Exception {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		try {
			moveMenu.findElement(By.xpath("//button[text()='"+moveName+"']")).click();
		}
		catch (Exception e) {
			throw new Exception("You do not have the move '"+moveName+"'");
		}
	}
	
	/**
	 * Gets the moves we currently have.
	 * @return String List - names of the moves we have
	 */
	public List<String> getMoves() {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		List<WebElement> moveButtons = moveMenu.findElements(By.tagName("button"));
		List<String> moves = new ArrayList<String>(4);
		for (WebElement e : moveButtons) {
			moves.add(substringToFirst(e.getText(), 0, "\n"));
		}
		return moves;
	}
	
	/**
	 * Gets the PP remaining for the move specified.
	 * @return PP remaining for specified move, throws exception if we don't have that move.
	 * @throws Exception if the specified move can't be found
	 */
	public int getMoveRemainingPP(String move) throws Exception {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		try {
			WebElement moveButton = moveMenu.findElement(By.xpath("//button[contains(text(),'"+move+"')]"));
			String[] moveInfo = moveButton.getText().split("\n");
			// moveInfo = [move name, type, pp/maxpp]
			return Integer.parseInt(substringToFirst(moveInfo[2], 0, "/"));
		}
		catch (Exception e) {
			throw new Exception("You do not have the move '"+move+"'");
		}
	}
	
	/**
	 * Switches to the specified Pokemon.
	 * @param pokemon The name of the Pokemon we want to switch to.
	 * @param byNickname Set to false to switch by species name.
	 * @throws Exception if the specified Pokemon can't be found
	 */
	public void switchTo(String pokemon, boolean byNickname) throws Exception {
		WebElement switchToDiv = driver.findElement(By.cssSelector("div.switchmenu"));
		try {
			if (byNickname) {
				WebElement pokeButton = switchToDiv.findElement(By.xpath("//button[contains(text(),'"+pokemon+"')]"));
				pokeButton.click();
			}
			else {
				// try to identify which button has the species specified. oh god lol nvm it's ez
				switchTo(getSlotForSpecies(pokemon));
			}
		}
		catch (Exception e) {
			throw new Exception("You do not have the Pokemon '"+pokemon+"'");
		}
	}
	
	/**
	 * Switches to the Pokemon in slot specified
	 * @param slot It's the slot
	 */
	public void switchTo(int slot) {
		WebElement switchToDiv = driver.findElement(By.cssSelector("div.switchmenu"));
		List<WebElement> buttons = switchToDiv.findElements(By.tagName("button"));
		if (slot < 0 || slot >= buttons.size()) {
			return;
		}
		buttons.get(slot).click();
	}
	
	/**
	 * Returns the slot in which the Pokemon with the specified name is in.
	 * @param pokemon Name of Pokemon species
	 * @return Integer - 0-5, slot.
	 */
	private int getSlotForSpecies(String pokemon) {
		JavascriptExecutor je = (JavascriptExecutor) driver;
		for (int i = 0; i < 6; ++i) {
			String species = (String)je.executeScript("return curRoom.battle.mySide.pokemon["+i+"].species;");
			if (species.equals(pokemon)) {
				return i;
			}
		}
		return -1;
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
		String turnString = substringToFirst(cTT, 5, "\n");
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
		//WebElement battleLog = driver.findElement(By.cssSelector("div.battle-log"));
		//return battleLog.getText();
		String text = "";
		try {
		Scanner r = new Scanner(new File("battlesample.log"));
		while (r.hasNextLine()) {
		text += r.nextLine() + "\n";
		}
		return text;
		}
		catch (Exception e) {
		return "File not found";
		}
	}
	
	/**
	 * Gets the text of the current turn we are in.
	 * @return String - current turn text, or empty string if a turn hasn't started yet.
	 */
	public String getCurrentTurnText() {
		String battleText = getBattleLogText();
		int currentTurnIdx = battleText.lastIndexOf("Turn");
		if (currentTurnIdx == -1) {
			return "";
		}
		else {
			return battleText.substring(currentTurnIdx);
		}
	}
	
	/**
	 * Gets the text of the last turn we were in.
	 * @return String - last turn text, or empty string if a turn hasn't been completed yet.
	 */
	public String getLastTurnText() {
		return getTurnText(getCurrentTurn()-1);
	}
	
	/**
	 * Gets the text of the specified turn.
	 * @param turn The turn. Set this to 0 to get the text that appears before turn 1.
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
	 * Attempts to find the team of the specified owner.
	 * First it will check the announcement in the log of the team that you see in most formats.
	 * If that fails, it will scan the owner's team icons to try and find as many as it can that way.
	 * NOTE, in the second case, if nicknames are present, they are NOT resolved to actual names. It is
	 * up to you to ensure that the names retrieved are real names.
	 * @param owner Name of team's owner
	 * @return String List - Pokemon names, or empty list on failure.
	 */
	public List<String> getTeam(String owner) {
		Pattern p = Pattern.compile(owner + "'s team:\n(.+ / +?.*?)$", Pattern.MULTILINE);
		Matcher m = p.matcher(getBattleLogText());
		if (m.find()) {
			return Arrays.asList(m.group(1).split(" / "));
		}

		ArrayList<String> team = new ArrayList<String>(6);

		WebElement trainerDiv = getTrainerDiv(owner);
		if (trainerDiv == null) {
			return team;
		}
		
		List<WebElement> pokeIcons = trainerDiv.findElements(By.cssSelector("span.pokemonicon"));
		for (WebElement e : pokeIcons) {
			String name = e.getAttribute("title");
			if (name.equals("Not revealed")) {
				continue;
			}
			int bracketIdx = name.indexOf(" (");
			if (bracketIdx != -1) {
				name = name.substring(0, bracketIdx);
			}
			team.add(name);
		}
		return team;
	}
	
	/**
	 * Waits until the battle log contains the specified text (up to 5 minutes)
	 * @param message String to wait for.
	 */
	public void waitForBattleLogContains(final String message) {
		(new WebDriverWait(driver, 300)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return getBattleLogText().contains(message);
            }
        });
	}
	
	/**
	 * Takes an ambiguous name string and returns the pokemon name.
	 * @param fullname either "pokemon name" or "nickname (pokemon name)"
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
	 * * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentPokemonAtTurn(String owner, int turn, boolean resolveNickname) {
		// special case, find first released.
		String sentOutStr = owner + " sent out ";
		if (turn == 0) {
			String text = getBattleLogText();
			int idx = text.indexOf(sentOutStr);
			if (resolveNickname) {
				return getNameFromPossibleNickname(substringToFirst(text,idx+sentOutStr.length(),"!\n"));
			}
			else {
				return substringToFirst(substringToFirst(text, 0, " ("), 0, "!\n");
			}
		}
		while (turn >= 0) {
			--turn;
			String text = getTurnText(turn);
			int idx = text.lastIndexOf(sentOutStr);
			if (idx != -1 && resolveNickname) {
				return getNameFromPossibleNickname(substringToFirst(text,idx+sentOutStr.length(),"!\n"));
			}
			else if (idx != -1) {
				return substringToFirst(substringToFirst(text, 0, " ("), 0, "!\n");
			}
		}
		return "";
	}
	
	/**
	 * Returns the Pokemon currently on owner's side of the field.
	 * @param owner Whose side of the field we are checking
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentPokemon(String owner, boolean resolveNickname) {
		return getCurrentPokemonAtTurn(owner, getCurrentTurn(), resolveNickname);
	}
	
	/**
	 * Returns the Pokemon currently on our side of the field.
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentPokemon(boolean resolveNickname) {
		return getCurrentPokemonAtTurn(getUserName(), getCurrentTurn(), resolveNickname);
	}
	
	/**
	 * Returns the Pokemon currently on opponent's side of the field.
	 * @param resolveNickname Set to false to retrieve the nickname of the Pokemon rather than species
	 * @return String - Pokemon name, empty string on failure.
	 */
	public String getCurrentOpponentPokemon(boolean resolveNickname) {
		return getCurrentPokemonAtTurn(getOpponentName(), getCurrentTurn(), resolveNickname);
	}
}
