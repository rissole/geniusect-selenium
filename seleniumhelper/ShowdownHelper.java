package seleniumhelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import seleniumhelper.loginterpret.BattleLog;

/**
 * Selenium helper functions specifically for Pokemon Showdown.
 * @author burse
 */
public class ShowdownHelper extends Helper {
	
	// Base URL of Pokemon Showdown server.
	private String rootURL;
	
	// Currently logged in user
	private String currentUser;
	
	private BattleLog battlelog;
	
	// Have we pressed the Kick Inactive Player button?
	private boolean battleTimerOn;
	
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
		this.battlelog = null;
		this.battleTimerOn = false;
	}
	
	/**
	 * Creates an instance of helper functions for Pokemon Showdown automation at http://play.pokemonshowdown.com
	 * @param driver The WebDriver that will be used for automation
	 */
	public ShowdownHelper(WebDriver driver) {
		super(driver);
		rootURL = "http://play.pokemonshowdown.com";
		this.currentUser = "";
		this.battlelog = null;
		this.battleTimerOn = false;
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
	 * @return TurnEndStatus.SWITCH if we are prompted for a lead Pokemon, TurnEndStatus.ATTACK otherwise.
	 * @throws InvalidTeamException If Showdown rejects your team for any reason.
	 */
	public TurnEndStatus waitForBattleStart() throws InvalidTeamException {
		(new WebDriverWait(driver, 120)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return isElementPresent(By.cssSelector("div.whatdo")) || isElementPresent(By.id("messagebox"));
            }
        });
		sleep(500);
		if (isElementPresent(By.id("messagebox"))) {
			throw new InvalidTeamException(driver.findElement(By.cssSelector("#messagebox > div")).getText());
		}
		
		initBattleLog();
		battleTimerOn = false;
		
		if (driver.findElement(By.cssSelector("div.whatdo")).getText().contains("How will you start the battle?")) {
			return TurnEndStatus.SWITCH;
		}
		return TurnEndStatus.ATTACK;
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
		waitForBattleLogContains(getOpponentName() + " won the battle!", true);
	}
	
	/**
	 * Presses "Kick Inactive Player" button
	 */
	public void kickInactivePlayer() {
		if (isElementPresent(By.cssSelector("div.replay-controls > button"))) {
			clickAt(By.cssSelector("div.replay-controls > button"));
			battleTimerOn = !battleTimerOn;
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
	
	/**
	 * Creates a team in the teambuilder from an importable.
	 * @param importable The full importable in Pokemon Showdown format.
	 * @param teamName The name you want the team to have. Recommended to be unique.
	 */
	public void createTeam(String importable, String teamName) {
		driver.findElement(By.id("tabtab-teambuilder")).click();
		sleep(500);
		int attempts = 0;
		while (!isElementVisible(By.xpath("//button[text()=' New team']"))) {
			clickAt(By.xpath("//button[i[@class='icon-chevron-left']]"));
			sleep(500);
			++attempts;
			if (attempts > 20) {
				throw new NoSuchElementException("Could not find 'New Team' button");
			}
		}
	    driver.findElement(By.xpath("//button[text()=' New team']")).click();
	    sleep(500);
	    driver.findElement(By.xpath("//button[text()=' Import/Export']")).click();
	    sleep(500);
	    driver.findElement(By.cssSelector("textarea.teamedit")).clear();
	    driver.findElement(By.cssSelector("textarea.teamedit")).sendKeys(importable);
	    driver.findElement(By.cssSelector("input.textbox.teamnameedit")).clear();
	    driver.findElement(By.cssSelector("input.textbox.teamnameedit")).sendKeys(teamName);
	    driver.findElement(By.cssSelector("button.savebutton")).click();
	    sleep(500);
	    driver.findElement(By.id("tabtab-lobby")).click();
	    sleep(500);
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
			if (!battleTimerOn && kickAfterSeconds != 0 && waited >= kickAfterSeconds*1000) {
				kickInactivePlayer();
				kickAfterSeconds = 0;
			}
			sleep(500);
			waited += 500;
			gameOver = ((Long)javascript("return curRoom.battle.done;") > 0);
		}
		updateBattleLog();
		if (gameOver) {
			if (battlelog.contains(getUserName() + " won the battle!", true)) {
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
	 * Finds the WebElement of the button with the specified move name.
	 * @param moveName Move name (case insensitive)
	 * @return WebElement - button, null on failure
	 */
	private WebElement findMoveButton(String moveName) {
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		for (WebElement e : moveMenu.findElements(By.tagName("button"))) {
			if (substringToFirst(e.getText(), 0, "\n").equalsIgnoreCase(moveName)) {
				return e;
			}
		}
		return null;
	}
	
	/**
	 * Chooses the specified move to attack with.
	 * @param moveName The move to use.
	 * @throws NoSuchChoiceException if the specified move can't be found 
	 */
	public void doMove(String moveName) throws NoSuchChoiceException {
		WebElement moveButton = findMoveButton(moveName);
		if (moveButton != null) {
			moveButton.click();
		}
		else {
			throw new NoSuchChoiceException("You do not have the move '"+moveName+"'");
		}
	}
	
	/**
	 * Gets the moves we currently have.
	 * @return String List - names of the moves we have
	 */
	public List<String> getMoves() {
		ArrayList<String> moves = new ArrayList<String>(4);
		WebElement moveMenu = driver.findElement(By.cssSelector("div.movemenu"));
		for (WebElement e : moveMenu.findElements(By.tagName("button"))) {
			moves.add(substringToFirst(e.getText(), 0, "\n"));
		}
		return moves;
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
		WebElement moveButton = findMoveButton(move);
		if (moveButton != null) {
			return (moveButton.getAttribute("disabled") == null);
		}
		else {
			throw new NoSuchChoiceException("You do not have the move '"+move+"'");
		}
	}
	
	/**
	 * Gets the moves the specified Pokemon, on our team, currently has.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The Pokemon whose moves we want to retrieve
	 * @param getShortNames If true, shortnames will be returned. (ie "leechseed" not "Leech Seed")
	 * @return String List - names of the moves it has
	 */
	public List<String> getMoves(String pokemon, boolean getShortNames) {
		return getMoves(getSlotForSpecies(pokemon), getShortNames);
	}
	
	/**
	 * Gets the moves the specified Pokemon, on our team, currently has.
	 * @param slot The slot that the Pokemon is in. [0-5]
	 * @param getShortNames If true, shortnames will be returned. (ie "leechseed" not "Leech Seed")
	 * @return String List - names of the moves it has
	 */
	public List<String> getMoves(int slot, boolean getShortNames) {
		if (slot < 0 ||  slot > 5)
			return new ArrayList<String>(0);
		@SuppressWarnings("unchecked")
		List<String> moves = (List<String>)javascript(
			"var pokeObj = curRoom.battle.mySide.pokemon[arguments[0]]; var moves = [];" + 
			"for (var i = 0; i < pokeObj.moves.length; ++i) {" + 
			"	var moveName = pokeObj.moves[i];" + 
			"	if (arguments[1] == false) {" + 
			"		moveName = Tools.getMove(moveName).name;" + 
			"		if (moveName.contains(\"Hidden Power\")) {" + 
			"			moveName = \"Hidden Power\";" + 
			"		}" + 
			"	}" + 
			"	moves.push(moveName);" + 
			"}" + 
			"return moves;"
		, slot, getShortNames);
		return moves;
	}
	
	/**
	 * Gets the PP remaining for the move specified.
	 * @return PP remaining for specified move, throws exception if we don't have that move.
	 * @throws NoSuchChoiceException if the specified move can't be found
	 */
	public int getMoveRemainingPP(String move) throws NoSuchChoiceException {
		WebElement moveButton = findMoveButton(move);
		if (moveButton != null) {
			String[] moveInfo = moveButton.getText().split("\n");
			// moveInfo = [move name, type, pp/maxpp]
			return Integer.parseInt(substringToFirst(moveInfo[2], 0, "/"));
		}
		else {
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
	 * @param slot It's the slot [0-5]
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
	public int getSlotForSpecies(String pokemon, String owner) {
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
	public int getSlotForSpecies(String pokemon) {
		return getSlotForSpecies(pokemon, getUserName());
	}
	
	/**
	 * Gets a Pokemon's attribute via javascript.
	 * @param slot The slot of the Pokemon to check. [0-5]
	 * @param owner Which team the Pokemon is on
	 * @param attribute Which javascript attribute we are trying to get
	 * @param json True if you want to return a JSON stringified object.
	 * @return Object - returned Javascript object (may be null)
	 */
	private Object getPokemonAttribute(int slot, String owner, String attribute, boolean json) {
		if (slot < 0 || slot > 5) {
			return null;
		}
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		if (!json) {
			return javascript("var p=curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return p[arguments[2]];", side, slot, attribute);
		}
		else {
			return javascript("var p=curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return JSON.stringify(p[arguments[2]]);", side, slot, attribute);
		}
	}
	
	/**
	 * Returns the attributes of the specified Pokemon. Note that it is generally faster to gather all the
	 * information of a Pokemon in one call like this if you wish to retrieve more than one field.
	 * @param slot The slot that the Pokemon is in. [0-5]
	 * @param owner Which team the Pokemon is on
	 * @return A Map containing all the attributes:<br>
	 * <b>'status'</b>: see getStatus<br>
	 * <b>'hp'</b>: see getHP<br>
	 * <b>'maxhp'</b>: see getMaxHP<br>
	 * <b>'gender'</b>: see getGender<br>
	 * <b>'level'</b>: see getLevel<br>
	 * <b>'ability'</b>: see getAbility<br>
	 * <b>'item'</b>: see getItem<br>
	 */
	public Map<String, Object> getPokemonAttributes(int slot, String owner) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		String info = (String)javascript(
				"var p=curRoom.battle[arguments[0]].pokemon[arguments[1]];" +
				"var ability = p.ability;" +
				"if (ability == \"\")" +
				"	ability = (p.abilities.length == 1) ? p.abilities[0] : null;" +
				"if (ability != null)" +
				"	ability = Tools.getAbility(ability).name;" +
				
				"var item = p.item;" +
				"if (item == null || item == \"\")" +
				"	item = null;" +
				"if (item != null)" +
				"	item = Tools.getItem(item).name;" +
				
				"if (p == null) return '';" +
				"return JSON.stringify({" +
				"'status':p.status, 'hp':p.hp, 'maxhp':p.maxhp, 'gender':p.gender, 'level':p.level," +
				"'ability':ability, 'item':item" +
				"});", side, slot
		);
		
		Map<String, Object> mapInfo = new HashMap<String, Object>();
		try {
			JSONObject jo = new JSONObject(info);
			mapInfo.put("status", jo.getString("status"));
			mapInfo.put("hp", jo.getInt("hp"));
			mapInfo.put("maxhp", jo.getInt("maxhp"));
			mapInfo.put("gender", jo.getString("gender"));
			mapInfo.put("level", jo.getInt("level"));
			mapInfo.put("ability", jo.getString("ability"));
			mapInfo.put("item", jo.getString("item"));
		}
		catch (JSONException e) {
			return mapInfo;
		}
		return mapInfo;
	}
	
	/**
	 * Returns the current status of the Pokemon.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return String - Pokemon status. 3 character abbreviation, lower case.
	 * Can return: <b>'tox', 'psn', 'frz', 'par', 'brn', or null if no status.</b>
	 */
	public String getStatus(String pokemon, String owner) {
		return getStatus(getSlotForSpecies(pokemon), owner);
	}
	
	/**
	 * Returns the current status of the Pokemon.
	 * @param slot The slot that the Pokemon is in. [0-5]
	 * @param owner Which team the Pokemon is on
	 * @return String - Pokemon status. 3 character abbreviation, lower case.
	 * Can return: <b>'tox', 'psn', 'frz', 'par', 'brn', or null if no status.</b>
	 */
	public String getStatus(int slot, String owner) {
		return (String)getPokemonAttribute(slot, owner, "status", false);
	}
	
	/**
	 * Returns whether the Pokemon specified has the given volatile. A volatile is an effect which
	 * is usually removed on switch.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner  Which team the Pokemon is on
	 * @param _volatile String - volatile name.<br/>
	 * Known values: <b>formechange, leechseed, protect, magiccoat, yawn, confusion,
	 * airballoon, transform, substitute, taunt, encore, torment, stockpile{1,2,3}, perish{1,2,3}</b>
	 * @return true if and only if the specified volatile is present.
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
	 * Returns whether the Pokemon specified has the given volatile. A volatile is an effect which
	 * is usually removed on switch.
	 * @param slot The slot the Pokemon is in.
	 * @param owner  Which team the Pokemon is on
	 * @param _volatile String - volatile name.<br/>
	 * Known values: <b>formechange, leechseed, protect, magiccoat, yawn, confusion,
	 * airballoon, transform, substitute, taunt, encore, torment, stockpile{1,2,3}, perish{1,2,3}</b>
	 * @return true if and only if the specified volatile is present.
	 */
	public boolean hasVolatile(int slot, String owner, String _volatile) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		return (Boolean)javascript("var p=curRoom.battle[arguments[0]].pokemon[arguments[1]]; if (p!=null) return p.hasVolatile(arguments[2]);", side, slot, _volatile);
	}
	
	/**
	 * Returns whether the specified Pokemon has fainted or not.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return True if fainted, false otherwise.
	 */
	public boolean isFainted(String pokemon, String owner) {
		return (Boolean)getPokemonAttribute(getSlotForSpecies(pokemon, owner),owner,"fainted",false);
	}
	
	/**
	 * Returns whether the specified Pokemon has fainted or not.
	 * @param slot The slot the Pokemon is in.
	 * @param owner Which team the Pokemon is on
	 * @return True if fainted, false otherwise.
	 */
	public boolean isFainted(int slot, String owner) {
		return (Boolean)getPokemonAttribute(slot,owner,"fainted",false);
	}
	
	/**
	 * Returns the specified Pokemon's HP.
	 * <b>NOTE: This returns a percentage (0-100) if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.</b><br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return Integer - returns a percentage (0-100) if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.
	 */
	public int getHP(String pokemon, String owner) {
		return ((Long)getPokemonAttribute(getSlotForSpecies(pokemon,owner),owner,"hp",false)).intValue();
	}
	
	/**
	 * Returns the specified Pokemon's HP.
	 * <b>NOTE: This returns a percentage (0-100) if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.</b>
	 * @param slot The slot the Pokemon is in.
	 * @param owner Which team the Pokemon is on
	 * @return Integer - returns a percentage (0-100) if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.
	 */
	public int getHP(int slot, String owner) {
		return ((Long)getPokemonAttribute(slot,owner,"hp",false)).intValue();
	}
	
	/**
	 * Returns the specified Pokemon's Max HP.
	 * <b>NOTE: This returns 100 if the <code>owner</code> is the opponent; otherwise
	 * the exact max HP value.</b><br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return Integer - returns 100 if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.
	 */
	public int getMaxHP(String pokemon, String owner) {
		return ((Long)getPokemonAttribute(getSlotForSpecies(pokemon,owner),owner,"maxhp",false)).intValue();
	}
	
	/**
	 * Returns the specified Pokemon's Max HP.
	 * <b>NOTE: This returns 100 if the <code>owner</code> is the opponent; otherwise
	 * the exact max HP value.</b>
	 * @param slot The slot the Pokemon is in.
	 * @param owner Which team the Pokemon is on
	 * @return Integer - returns 100 if the <code>owner</code> is the opponent; otherwise
	 * the exact HP value.
	 */
	public int getMaxHP(int slot, String owner) {
		return ((Long)getPokemonAttribute(slot,owner,"maxhp",false)).intValue();
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
			@SuppressWarnings("unchecked")
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
	
	/**
	 * Returns the full name of the ability of the specified Pokemon.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return Ability name, or null if no ability found.<br/>
	 * Note that in most circumstances, this will be null when <code>owner</code> is the opponent.
	 * It only returns correctly if the target Pokemon has <b>only one possible ability.</b>
	 */
	public String getAbility(String pokemon, String owner) {
		return getAbility(pokemon, owner, false);
	}
	
	/**
	 * Returns the full name of the ability of the specified Pokemon.
	 * @param slot The slot of the Pokemon [0-5]
	 * @param owner Which team the Pokemon is on
	 * @return Ability name, or null if no ability found.<br/>
	 * Note that in most circumstances, this will be null when <code>owner</code> is the opponent.
	 * It only returns correctly if the target Pokemon has <b>only one possible ability.</b>
	 */
	public String getAbility(int slot, String owner) {
		return getAbility(slot, owner, false);
	}
	
	/**
	 * Returns the name of the ability of the specified Pokemon.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @param getShortName If true, shortnames will be returned. (ie "shedskin" not "Shed Skin")
	 * @return Ability name, or null if no ability found.<br/>
	 * Note that in most circumstances, this will be null when <code>owner</code> is the opponent.
	 * It only returns correctly if the target Pokemon has <b>only one possible ability.</b>
	 */
	public String getAbility(String pokemon, String owner, boolean getShortName) {
		return getAbility(getSlotForSpecies(pokemon), owner, getShortName);
	}
	
	/**
	 * Returns the name of the ability of the specified Pokemon.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @param getShortName If true, shortnames will be returned. (ie "shedskin" not "Shed Skin")
	 * @return Ability name, or null if no ability found.<br/>
	 * Note that in most circumstances, this will be null when <code>owner</code> is the opponent.
	 * It only returns correctly if the target Pokemon has <b>only one possible ability.</b>
	 */
	public String getAbility(int slot, String owner, boolean getShortName) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		return (String)javascript(
			"var pokeObj = curRoom.battle[arguments[0]].pokemon[arguments[1]];" +
			"var ability = pokeObj.ability;" +
			"if (ability == \"\")" +
			"	ability = (pokeObj.abilities.length == 1) ? pokeObj.abilities[0] : null;" +
			"if (arguments[2])" +
			"	return ability;" +
			"else" +
			"	return Tools.getAbility(ability).name;"
		, side, slot, getShortName);
	}
	
	/**
	 * Returns the full name of the item held by the specified Pokemon on our team.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return Item name, or empty string if no item.
	 */
	public String getItem(String pokemon, String owner) {
		return getItem(pokemon, owner, false);
	}
	
	/**
	 * Returns the full name of the item held by the specified Pokemon on our team.
	 * @param slot The slot that the Pokemon is in. [0-5]
	 * @param owner Which team the Pokemon is on
	 * @return Item name, or empty string if no item.
	 */
	public String getItem(int slot, String owner) {
		return getItem(slot, owner, false);
	}
	
	/**
	 * Returns the name of the item held by the specified Pokemon on our team.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @param getShortName If true, shortnames will be returned. (ie "griseousorb" not "Griseous Orb")
	 * @return Item name, or null if no item.
	 */
	public String getItem(String pokemon, String owner, boolean getShortName) {
		return getItem(getSlotForSpecies(pokemon), owner, getShortName);
	}
	
	/**
	 * Returns the name of the item held by the specified Pokemon on our team.
	 * @param slot The slot that the Pokemon is in. [0-5]
	 * @param owner Which team the Pokemon is on
	 * @param getShortName If true, shortnames will be returned. (ie "griseousorb" not "Griseous Orb")
	 * @return Item name, or null if no item.
	 */
	public String getItem(int slot, String owner, boolean getShortName) {
		String side = "mySide";
		if (owner.equals(getOpponentName())) {
			side = "yourSide";
		}
		return (String)javascript(
			"var pokeObj = curRoom.battle[arguments[0]].pokemon[arguments[1]];" +
			"var item = pokeObj.item;" +
			"if (item == null || item == \"\")" +
			"	return null;" +
			"if (arguments[2])" +
			"	return item;" +
			"else" +
			"	return Tools.getItem(item).name;"
		, side, slot, getShortName);
	}
	
	/**
	 * Gets the specified Pokemon's gender.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return "M" for male, "F" for female, or empty string if genderless.
	 */
	public String getGender(String pokemon, String owner) {
		return (String)getPokemonAttribute(getSlotForSpecies(pokemon,owner), owner, "gender", false);
	}
	
	/**
	 * Gets the specified Pokemon's gender.
	 * @param slot The slot the Pokemon is in.
	 * @param owner Which team the Pokemon is on
	 * @return "M" for male, "F" for female, or empty string if genderless.
	 */
	public String getGender(int slot, String owner) {
		return (String)getPokemonAttribute(slot, owner, "gender", false);
	}
	
	/**
	 * Gets the specified Pokemon's level.<br>
	 * <b>NOTE</b> It is much more efficient to use the <code>slot</code> parameterised version of this
	 * function if you can.
	 * @param pokemon The species name of the Pokemon
	 * @param owner Which team the Pokemon is on
	 * @return int - Pokemon's level.
	 */
	public int getLevel(String pokemon, String owner) {
		return ((Long)getPokemonAttribute(getSlotForSpecies(pokemon,owner), owner, "level", false)).intValue();
	}
	
	/**
	 * Gets the specified Pokemon's level.
	 * @param slot The slot the Pokemon is in.
	 * @param owner Which team the Pokemon is on
	 * @return int - Pokemon's level.
	 */
	public int getLevel(int slot, String owner) {
		return ((Long)getPokemonAttribute(slot, owner, "level", false)).intValue();
	}
	
	/**
	 * Waits until the battle log contains the specified text (up to 5 minutes)
	 * @param message String to wait for.
	 */
	public void waitForBattleLogContains(final String message, final boolean ignoreChats) {
		(new WebDriverWait(driver, 300)).until(new ExpectedCondition<Boolean>() {
           public Boolean apply(WebDriver d) {
               return battlelog.contains(message, ignoreChats);
           }
       });
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
	
	// Why? Answer: lazy.
	public static String substringToFirst(String s, int startIndex, String stop) {
		return BattleLog.substringToFirst(s, startIndex, stop);
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
		@SuppressWarnings("unchecked")
		List<String> team = (List<String>)javascript(
			"var pokes = []; var pokeObjs = curRoom.battle[arguments[0]].pokemon; " +
			"for (var i = 0; i < pokeObjs.length; ++i) {" +
			"	pokes.push(pokeObjs[i].species);" +
			"}" +
			"return pokes;"
		, side);
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
		@SuppressWarnings("unchecked")
		List<String> team = (List<String>)javascript(
			"var pokes = []; var pokeObjs = curRoom.battle[arguments[0]].pokemon; " +
			"for (var i = 0; i < pokeObjs.length; ++i) {" +
			"	if (!pokeObjs[i].fainted)" +
			"		pokes.push(pokeObjs[i].species);" +
			"}" +
			"return pokes;"
		, side);
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
	
	public void initBattleLog() {
		battlelog = new BattleLog(driver.findElement(By.cssSelector("div.battle-log > div.inner")).getAttribute("innerHTML"));
	}
	
	public void updateBattleLog() {
		battlelog.setLogText(driver.findElement(By.cssSelector("div.battle-log > div.inner")).getAttribute("innerHTML"));
	}
	
	public BattleLog getBattleLog() {
		return battlelog;
	}
}
