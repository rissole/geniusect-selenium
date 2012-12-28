package seleniumhelper;
import org.openqa.selenium.*;

/**
 * Selenium helper functions specifically for Pokemon Showdown.
 * @author burse
 */
public class ShowdownHelper extends Helper {
	
	// Base URL of Pokemon Showdown server.
	private String rootURL;
	
	/**
	 * Creates an instance of helper functions for Pokemon Showdown automation
	 * @param driver The WebDriver that will be used for automation
	 * @param rootURL The base URL of Pokemon Showdown server, can be changed to alter what server the
	 * automation will run on. No trailing forward slash eg. http://play.pokemonshowdown.com
	 */
	public ShowdownHelper(WebDriver driver, String rootURL) {
		super(driver);
		this.rootURL = rootURL;
	}
	
	/**
	 * Creates an instance of helper functions for Pokemon Showdown automation at http://play.pokemonshowdown.com
	 * @param driver The WebDriver that will be used for automation
	 */
	public ShowdownHelper(WebDriver driver) {
		super(driver);
		rootURL = "http://play.pokemonshowdown.com";
	}
	
	/**
	 * Opens the showdown homepage.
	 */
	public void open() {
		driver.get(rootURL);
	}
	
	//// Lobby functions
	
	/**
	 * Logs in with default test account details.
	 */
	public void login() {
		login("geniusecttest", "test123");
	}
	
	/**
	 * Logs in to Showdown with the supplied information.
	 */
	public void login(String username, String password) {
		By loginButtonBy = By.xpath("(//button[@onclick=\"return rooms['lobby'].formRename()\"])[595]");
		waitForElementPresent(loginButtonBy);
		
	    driver.findElement(loginButtonBy).click();
	    driver.findElement(By.id("overlay_name")).clear();
	    driver.findElement(By.id("overlay_name")).sendKeys(username);
	    driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
	    
	    waitForElementPresent(By.id("overlay_password"));
	    
	    driver.findElement(By.id("overlay_password")).clear();
	    driver.findElement(By.id("overlay_password")).sendKeys(password);
	    driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
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
	
	//// Battle functions
	
	/**
	 * Leaves the current battle
	 */
	public void leaveBattle() {
		clickAt(By.cssSelector("span.close.close0"));
	}
	
	/**
	 * Alias for leaveBattle
	 */
	public void surrender() {
		leaveBattle();
	}
	
	/**
	 * Presses "Kick Inactive Player" button
	 */
	public void kickInactivePlayer() {
		clickAt(By.cssSelector("div.replay-controls > button"));
	}
	
	//// Battle log functions
	
	public boolean battleLogContains(String text) {
		return true;
	}
	
}
