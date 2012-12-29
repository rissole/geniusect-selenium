import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;

public class Example  {
    public static void main(String[] args) {
    	ShowdownHelper showdown = new ShowdownHelper(null);
        System.out.println(showdown.getCurrentTurn());
    }
}