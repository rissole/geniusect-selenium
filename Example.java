import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;
import seleniumhelper.ShowdownHelper.TurnEndStatus;
import seleniumhelper.loginterpret.*;
import seleniumhelper.loginterpret.events.TIEvent;

public class Example  {
    public static void main(String[] args) throws Exception {
    	//testBattleLogFile();
    	testBattle();
    	//benchmark();
    }
    
    public static void testBattle() throws Exception {
    	//System.setProperty("webdriver.firefox.profile", "default");
    	FirefoxDriver driver = new FirefoxDriver();
    	// wait up to 10 seconds for elements to load
    	//driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        ShowdownHelper showdown = new ShowdownHelper(driver, "http://play.pokemonshowdown.com/~~rissole-showdown.herokuapp.com:80");
        showdown.open();
        //String[] userPass = loadUserPass();
        //showdown.sleep(60000);
        showdown.login("geniusecttest"+(new Random()).nextInt(100000), "");
        showdown.createTeam("Shuckle @ Rocky Helmet\nTrait: Sturdy\nEVs: 252 SDef / 252 HP / 4 Atk\nCareful Nature\n- Acupressure\n- Power Split\n- Rest\n- Rollout", "t");
        showdown.findBattle("Ubers", "t");
        
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
	        
	        System.out.println("Current turn: " + showdown.getBattleLog().getCurrentTurn());
	        System.out.println("-Current turn---------------");
	        System.out.println(showdown.getBattleLog().getCurrentTurnText());
	        System.out.println("-Last turn----------");
	        System.out.println(showdown.getBattleLog().getLastTurnText());
	        System.out.println("----------------");
	        System.out.println("Opponent's Pokemon: "+showdown.getCurrentPokemon(OPP, false));
	             
	        System.out.println("Moves:");
	        String poke = showdown.getCurrentPokemon(true);
	        printlist(showdown.getMoves());
	        for (String move : showdown.getMoves()) {
	        	System.out.println(move + ": " + showdown.getMoveRemainingPP(move) + " PP");
	        }
	        System.out.println("Gender: '"+showdown.getGender(poke, SELF)+"'");
	        System.out.println("Ability: "+showdown.getAbility(poke, SELF));
	        System.out.println("Item: "+showdown.getItem(poke, SELF));
	        System.out.println("Their Ability: "+showdown.getAbility(showdown.getCurrentPokemon(OPP, true), OPP));
	        
	        System.out.println("Format: "+showdown.getBattleLog().getFormat()+"\nClauses: ");
	        printlist(showdown.getBattleLog().getClauses());
	        
	        TurnEndStatus s = TurnEndStatus.UNKNOWN;
	        while (s != TurnEndStatus.WON && s != TurnEndStatus.LOST) {
	        	if (showdown.getBattleLog().contains("gsquit", false)) {
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
			w.write(showdown.getBattleLog().getLogText());
			w.close();
			System.out.println("Dumped log to " + battleTitle+".log");
		}
    	catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    
    public static void testBattleLogFile() {
    	String text = "";
		try {
			Scanner r = new Scanner(new File("battlesample_html.log"));
			while (r.hasNextLine()) {
				text += r.nextLine() + "\n";
			}
		}
		catch (Exception e) {
			return;
		}
		BattleLog bl = new BattleLog(text);
		System.out.println(bl.getCurrentPokemonAtTurn("RODAN", 5, false));
		System.out.println(bl.getCurrentPokemonAtTurn("RODAN", 6, false));
		System.out.println(bl.getCurrentPokemonAtTurn("Cloak", 6, true));
		System.out.println(bl.getCurrentPokemonAtTurn("Cloak", 6, false));
		System.out.println(bl.getCurrentTurn());
		System.out.println("-------------------");
		TIContext tic = new TIContext();
		tic.foeCurrentPokemon = bl.getCurrentPokemonAtTurn("Cloak", 5, true);
		tic.myCurrentPokemon = bl.getCurrentPokemonAtTurn("RODAN", 5, true);
		TurnInfo ti = new TurnInfo(bl.getTurnHTML(5), tic);
		for (TIEvent event : ti.getEvents()) {
			System.err.println(event);
			System.err.println("-------");
		}
    }
    
    public static void benchmark() throws Exception {
    	FirefoxDriver driver = new FirefoxDriver();
		ShowdownHelper showdown = new ShowdownHelper(driver, "http://play.pokemonshowdown.com/~~rissole-showdown.herokuapp.com:80");
		showdown.open();
		String userName = "geniusecttest"+(new Random()).nextInt(100000);
		showdown.login(userName, "");
        showdown.createTeam("Hard To Please (Ninetales) (F) @ Leftovers\nTrait: Drought\nEVs: 252 HP / 252 SAtk / 4 SDef\nModest Nature\nIVs: 30 SAtk / 30 SDef\n- Sunny Day\n- SolarBeam\n- Overheat\n- Power Swap\n\nThe Jungle (Tangrowth) (M) @ Leftovers\nTrait: Chlorophyll\nEVs: 252 HP / 252 Spd / 4 Atk\nNaive Nature\n- Growth\n- Power Whip\n- Hidden Power [Ice]\n- Earthquake\n\nDisease (Dugtrio) (M) @ Focus Sash\nTrait: Arena Trap\nShiny: Yes\nEVs: 252 Spd / 4 Def / 252 Atk\nJolly Nature\n- Earthquake\n- Sucker Punch\n- Stone Edge\n- Reversal\n\nSerpentine (Heatran) (M) @ Choice Scarf\nTrait: Flash Fire\nShiny: Yes\nEVs: 252 Spd / 252 SAtk / 4 HP\nModest Nature\n- Overheat\n- SolarBeam\n- Earth Power\n- Hidden Power [Ice]\n\nWorse Everyday (Dragonite) (M) @ Lum Berry\nTrait: Multiscale\nEVs: 252 Spd / 4 HP / 252 Atk\nAdamant Nature\n- Dragon Dance\n- Fire Punch\n- ExtremeSpeed\n- Outrage\n\nAnimal (Donphan) (M) @ Leftovers\nTrait: Sturdy\nEVs: 252 SDef / 28 HP / 228 Def\nCareful Nature\n- Rapid Spin\n- Toxic\n- Stealth Rock\n- Earthquake", "t");
        showdown.findBattle("Ubers", "t");
        
        TurnEndStatus startStatus = showdown.waitForBattleStart();
        if (startStatus == TurnEndStatus.SWITCH) {
        	showdown.switchTo(0);
        	showdown.waitForNextTurn(0);
        }
        long startTime;
        long endTime;
        startTime = System.nanoTime();
    	List<String> ourPokes= showdown.getTeam(userName);
    	endTime = System.nanoTime();
		System.out.println("Loaded Pokemon LIST in "+((endTime-startTime)/1000000)+"ms");
    	for(int n = 0; n < ourPokes.size(); n++) {
    		startTime = System.nanoTime();
    		
    		showdown.getMoves(n, true);
    		
    		showdown.getPokemonAttributes(n, userName);
    		
    		endTime = System.nanoTime();
    		System.out.println("Loaded Pokemon "+n+" in "+((endTime-startTime)/1000000)+"ms");
    	}
    }
}