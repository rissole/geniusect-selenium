package seleniumhelper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import org.openqa.selenium.*;
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
	 */
	public void login(String userName, String password) {
		By loginButtonBy = By.xpath("//button[contains(text(),'Choose name')]");
		waitForElementPresent(loginButtonBy, 20);
		
	    driver.findElement(loginButtonBy).click();
	    driver.findElement(By.id("overlay_name")).clear();
	    driver.findElement(By.id("overlay_name")).sendKeys(userName);
	    driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
	    
	    waitForElementPresent(By.id("overlay_password"));
	    
	    driver.findElement(By.id("overlay_password")).clear();
	    driver.findElement(By.id("overlay_password")).sendKeys(password);
	    driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
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
	
	/**
	 * Waits for the next turn to begin or the battle to end, kicking inactive players.
	 * @param kickAfterSeconds Clicks 'Kick inactive player' after this number of seconds. 
	 * Set to 0 to never kick
	 * @return Returns true if we are at the next turn, or false if the game ended (because we kicked them)
	 */
	public boolean waitForNextTurn(int kickAfterSeconds) {
		int waited = 0;
		boolean haveWon = getBattleLogText().contains(getUserName() + " won the battle!");
		while (!isElementPresent(By.cssSelector("div.moveselect")) && !haveWon) {
			if (kickAfterSeconds != 0 && waited >= kickAfterSeconds) {
				kickInactivePlayer();
			}
			sleep(1000);
			waited += 1;
		}
		return (!haveWon);
	}
	
	/**
	 * Chooses the specified move to attack with.
	 * @param moveName The move to use.
	 */
	public void doMove(String moveName) {
		clickAt(By.xpath("//button[text()='"+moveName+"']"));
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
			moves.add(e.getText());
		}
		return moves;
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
		WebElement battleLog = driver.findElement(By.cssSelector("div.battle-log"));
		return battleLog.getText();
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
		String battleText = getBattleLogText();
		int currentTurnIdx = battleText.lastIndexOf("Turn");
		int lastTurnIdx = battleText.lastIndexOf("Turn", currentTurnIdx-4);
		if (lastTurnIdx == -1) {
			return "";
		}
		else {
			return battleText.substring(lastTurnIdx, currentTurnIdx);
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
}
