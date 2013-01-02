import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;

public class Example  {
    public static void main(String[] args) throws Exception {
    	WebDriver driver = new FirefoxDriver();
    	// wait up to 10 seconds for elements to load
    	driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        
        ShowdownHelper showdown = new ShowdownHelper(driver);
        showdown.open();
        String[] userPass = loadUserPass();
        showdown.login(userPass[0], userPass[1]);
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
        	System.out.print(team.get(i));
        	if (i != team.size()-1)
        		System.out.print(", ");
        }
        System.out.println();
        
        System.out.println("----------------");
        System.out.println(showdown.getCurrentTurnText());
        System.out.println("----------------");
        System.out.println(showdown.getLastTurnText());
        System.out.println("----------------");
        System.out.println(showdown.getCurrentPokemon("Opponent's Pokemon: "+showdown.getCurrentPokemon(showdown.getOpponentName())));
        
        //TODO: test waitForNextTurn, doMove.
        
        System.out.println("Moves:");
        printlist(showdown.getMoves());
        for (String move : showdown.getMoves()) {
        	System.out.println(move + ": " + showdown.getMoveRemainingPP(move) + " PP");
        }
        
        showdown.leaveBattle();
    }
    
    public static String[] loadUserPass() throws FileNotFoundException {
    	String[] ret = new String[2];
		Scanner s = new Scanner(new File("bin/account.txt"));
		ret[0] = s.nextLine();
		ret[1] = s.nextLine();
		return ret;    	
    }
    
    public static <T> void printlist(List<T> l) {
    	for (int i = 0; i < l.size(); ++i) {
        	System.out.print(l.get(i).toString());
        	if (i != l.size()-1)
        		System.out.print(", ");
        }
    	System.out.println();
    }
}