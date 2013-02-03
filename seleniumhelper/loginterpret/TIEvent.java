package seleniumhelper.loginterpret;

public class TIEvent {
	
	/**
	 * Requires all lines until a spacer div, h2 or end of turn; whichever occurs first.
	 */
	public static final int REQUIRES_UNTIL_SPACER = -1;
	/**
	 * requires() returns this if this line needs to be skipped.
	 */
	public static final int REQUIRES_SKIPPING = -2;
	
	private int extraLinesRequired;
	private String eventText;
	
	public TIEvent(String firstLine) {
		eventText = firstLine;
		extraLinesRequired = 0;
		initialInterpret();
	}
	
	private void initialInterpret() {
		determineExtraLinesRequired();
	}
	
	private void determineExtraLinesRequired() {
		if (eventText.startsWith("<h2>Turn") || eventText.startsWith("<div class=\"spacer\">")) {
			extraLinesRequired = TIEvent.REQUIRES_SKIPPING;
		}
		else {
			extraLinesRequired = TIEvent.REQUIRES_UNTIL_SPACER;
		}	
	}
	
	/**
	 * Returns the number of extra lines required for the event.
	 * @return int - number of lines required, or:<br>
	 * <code>REQUIRES_UNTIL_SPACER<br>
	 * REQUIRES_SKIPPING</code>
	 */
	public int requires() {
		return extraLinesRequired;
	}
	
	public void appendToEvent(String text) {
		eventText += "\n"+text;
	}
	
	public String getEventText() {
		return eventText;
	}
	
	public String toString() {
		return getEventText();
	}
}
