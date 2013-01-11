import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;
import seleniumhelper.ShowdownHelper.TurnEndStatus;

public class Example  {
    public static void main(String[] args) throws Exception {
    	//System.setProperty("webdriver.firefox.profile", "default");
    	FirefoxDriver driver = new FirefoxDriver();
    	// wait up to 10 seconds for elements to load
    	driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        ShowdownHelper showdown = new ShowdownHelper(driver, "http://play.pokemonshowdown.com/~~rissole-showdown.herokuapp.com:80");
        showdown.open();
        //String[] userPass = loadUserPass();
        //showdown.login("geniusecttest"+(new Random()).nextInt(100000), "");
        showdown.sleep(60000);
        showdown.login("geniusecttest"+(new Random()).nextInt(100000), "");
        showdown.findBattle("Ubers (suspect test)", "Untitled 18");
        
        // WAIT FOR BATTLE START
        TurnEndStatus startStatus = showdown.waitForBattleStart();
        if (startStatus == TurnEndStatus.SWITCH) {
        	showdown.switchTo(0);
        	showdown.waitForNextTurn(0);
        }
        try {
	        String SELF = showdown.getUserName();
	        String OPP = showdown.getOpponentName();
	        
	        System.out.println("My name is " + SELF + ", and I just started a battle.");
	        System.out.println("This is my team. There is none like it-");
	        List<String> ourTeam = showdown.getTeam(SELF);
	        printlist(ourTeam);
	        System.out.println();
	        System.out.println("My hapless opponent is " + OPP + ", and this is his team; or what I know of it:");
	        List<String> team = showdown.getTeam(OPP);
	        printlist(team);
	        System.out.println();
	        
	        System.out.println("Current turn: " + showdown.getCurrentTurn());
	        System.out.println("-Current turn---------------");
	        System.out.println(showdown.getCurrentTurnText());
	        System.out.println("-Last turn----------");
	        System.out.println(showdown.getLastTurnText());
	        System.out.println("----------------");
	        System.out.println("Opponent's Pokemon: "+showdown.getCurrentPokemon(OPP, false));
	             
	        System.out.println("Moves:");
	        printlist(showdown.getMoves(showdown.getCurrentPokemon(true)));
	        for (String move : showdown.getMoves()) {
	        	System.out.println(move + ": " + showdown.getMoveRemainingPP(move) + " PP");
	        }
	        
	        System.out.println("Format: "+showdown.getFormat()+"\nClauses: ");
	        printlist(showdown.getClauses());
	        
	        TurnEndStatus s = TurnEndStatus.UNKNOWN;
	        while (s != TurnEndStatus.WON && s != TurnEndStatus.LOST) {
	        	if (showdown.getBattleLogText().contains("gsquit")) {
	        		break;
	        	}
	        	ourTeam = showdown.getSwitchableTeam();
	        	String switchingTo = "No one";
	        	if (ourTeam.size() > 0) {
	        		switchingTo = ourTeam.get((new Random()).nextInt(ourTeam.size()));
	        		System.out.println("Switching to " + switchingTo);
			        showdown.switchTo(switchingTo,false);
	        	}
	        	else {
	        		showdown.doMove(showdown.getUsableMoves().get(0));
	        	}
		        
		        s = showdown.waitForNextTurn(10);
		        System.err.println(s);
		        
		        System.out.println("Current Pokemon now (should be "+switchingTo+"): "+showdown.getCurrentPokemon(false));
	        }
        }
        finally {
        	dumplogfile(showdown);
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
    
    public static void dumplogfile(ShowdownHelper showdown) {
    	try {
    		String url = showdown.getDriver().getCurrentUrl();
    		String battleTitle = url.substring(url.lastIndexOf("/")+1);
    		File out = new File(battleTitle+".log");
    		out.createNewFile();
			PrintWriter w = new PrintWriter(out);
			w.write(showdown.getBattleLogText());
			w.close();
			System.out.println("Dumped log to " + battleTitle+".log");
		}
    	catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
}