package seleniumhelper;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/**
 * General Selenium helper functions.
 * @author burse
 */
public class Helper {
	protected WebDriver driver;
	
	public Helper(WebDriver driver) {
		this.driver = driver;
	}

	public boolean isElementPresent(By by) {
		try {
			driver.findElement(by);
			return true;
		}
		catch (NoSuchElementException e) {
			return false;
		}
	}
	
	public boolean isElementVisible(By by) {
		try {
			return driver.findElement(by).isDisplayed();
		}
		catch (NoSuchElementException e) {
			return false;
		}
	}
	
	public void waitForElementPresent(final By by) {
		waitForElementPresent(by, 10);
	}
	
	public void waitForElementPresent(final By by, final int timeOutSeconds) {
		(new WebDriverWait(driver, timeOutSeconds)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return isElementPresent(by);
            }
        });
	}
	
	public void clickAt(By by) {
		driver.findElement(by).click();
	}
	
	public void dropdownSelect(By dropdown, String option) {
		Select dd = new Select(driver.findElement(dropdown));
		dd.selectByVisibleText(option);
	}
	
	public void dropdownSelect(By dropdown, int option) {
		WebElement dd = driver.findElement(dropdown);
		sleep(500);
		(new Select(dd)).selectByIndex(option);
	}
	
	public void sleep(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public List<WebElement> findElementsContainingText(By by, String text) {
		List<WebElement> allElements = driver.findElements(by);
		List<WebElement> elementsContainingText = new ArrayList<WebElement>();
		for (WebElement e : allElements) {
			if (e.getText().contains(text)) {
				elementsContainingText.add(e);
			}
		}
		return elementsContainingText;
	}
	
	public WebElement findElementContainingText(By by, String text) {
		List<WebElement> elements = findElementsContainingText(by, text);
		if (elements.size() < 1) {
			return null;
		}
		else {
			return elements.get(0);
		}
	}
	
	public WebDriver getDriver() {
		return driver;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T javascript(String script, Object...args) {
		return (T)((JavascriptExecutor)driver).executeScript(script, args);
	}
	
	/**
	 * Temporary hack to clear textboxes, since it seems broken.
	 * @param cssSelector
	 */
	public void clear(String cssSelector) {
		javascript("document.querySelector(arguments[0]).value = ''", cssSelector);
	}
	
	/**
	 * Temporary hack to set text values instantly, since sendKeys is slow now.
	 * @param cssSelector
	 */
	public void setValue(String cssSelector, String value) {
		javascript("document.querySelector(arguments[0]).value = arguments[1]", cssSelector, value);
	}
}
