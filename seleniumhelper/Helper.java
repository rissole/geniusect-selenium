package seleniumhelper;
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
	
	public void waitForElementPresent(final By by) {
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return isElementPresent(by);
            }
        });
	}
	
	public void clickAt(By by) {
		driver.findElement(by).click();
	}
	
	public void dropdownSelect(By dropdown, String option) {
		WebElement dd = driver.findElement(dropdown);
		dd.click();
		sleep(500);
		(new Select(dd)).selectByVisibleText(option);
	}
	
	public void dropdownSelect(By dropdown, int option) {
		WebElement dd = driver.findElement(dropdown);
		dd.click();
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
}
