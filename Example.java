import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import seleniumhelper.ShowdownHelper;

public class Example  {
    public static void main(String[] args) {
    	ShowdownHelper showdown = new ShowdownHelper(null);
        System.out.println("Should be 11:" + showdown.getCurrentTurn());
        
        System.out.println("Should be Heatran:" + showdown.getPokemonForNickname("Guns n Roses", "Serpentine"));
        
        System.out.println("Guns n Roses's team:");
        String[] team = showdown.getTeam("Guns n Roses");
        for (int i = 0; i < team.length; ++i) {
        	System.out.println(team[i]);
        }
    }
}