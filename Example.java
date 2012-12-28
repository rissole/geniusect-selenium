import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;

public class Example  {
    public static void main(String[] args) {
        WebDriver driver = new FirefoxDriver();
        
        ShowdownHelper showdown = new ShowdownHelper(driver);
        showdown.open();
        showdown.login();
        showdown.findBattle("Random Battle", "");
    }
}