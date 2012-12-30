import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;

public class Example  {
    public static void main(String[] args) {
    	WebDriver driver = new FirefoxDriver();
    	// wait up to 10 seconds for elements to load
    	driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        
        ShowdownHelper showdown = new ShowdownHelper(driver);
        showdown.open();
        showdown.login();
        showdown.findBattle("Random Battle", "");
        
        showdown.waitForBattleStart();
        
        System.out.println("My name is " + showdown.getUserName() + ", and I just started a battle.");
        System.out.println("This is my team. There is none like it-");
        List<String> team = showdown.getTeam(showdown.getUserName());
        for (int i = 0; i < team.size(); ++i) {
        	System.out.print(team.get(i));
        	if (i != team.size()-1)
        		System.out.print(", ");
        }
        System.out.println();
        System.out.println("My hapless opponent is " + showdown.getOpponentName() + ", and this is his team; or what I know of it:");
        team = showdown.getTeam(showdown.getOpponentName());
        for (int i = 0; i < team.size(); ++i) {
        	System.out.print(team.get(i) + ", ");
        	if (i != team.size()-1)
        		System.out.print(", ");
        }
        System.out.println();
        showdown.sendMessage("Heh.");
    }
}