package seleniumhelper.loginterpret;

/**
 * Class to provide additional information to TurnInfo log interpret functionality.
 */
public class TIContext {
	/**
	 * Species name of Pokemon on Player 1's side of the field.<br>
	 * <code>getCurrentPokemon(getUserName(), true)</code>
	 */
	public String myCurrentPokemon;
	
	/**
	 * Species name of Pokemon on Player 2's side of the field.<br>
	 * <code>getCurrentPokemon(getOpponentName(), true)</code>
	 */
	public String foeCurrentPokemon;
}
