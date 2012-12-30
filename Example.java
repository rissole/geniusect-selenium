import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;

public class Example  {
    public static void main(String[] args) {
    	WebDriver driver = new FirefoxDriver();
        
        ShowdownHelper showdown = new ShowdownHelper(driver);
        showdown.open();
        showdown.login();
        /*showdown.findBattle("Random Battle", "");
        
        //waitforbattlestart
        
        System.out.println("Our team:");
        String[] team = showdown.getTeam("geniusecttest");
        for (int i = 0; i < team.length; ++i) {
        	System.out.println(team[i]);
        }*/
    }
}