package seleniumhelper.loginterpret.events;

import java.util.regex.Pattern;

/**
 * Class for mapping Regex patterns to TIEvent subclasses.
 */
public class TIEventData {
	/**
	 * Requires all lines until a spacer div, h2 or end of turn; whichever occurs first.
	 */
	public static final int REQUIRES_UNTIL_SPACER = -1;
	
	/**
	 * Pattern in the log that means this event.
	 */
	public Pattern regex;
	/**
	 * Class for this event.
	 */
	public Class<? extends TIEvent> tiEventClass;
	
	/** The number of additional lines required from the battle log
	 * to create this TIEvent object. Special
	 * <code>TIEventData.REQUIRES_UNTIL_SPACER</code> indicates lines required
	 * are up to the next spacer div, h2 tag, or turn end (which is often the case
	 * with Showdown).
	 * Otherwise it is an integer specifying how many lines from the log should
	 * be read for the event, <b>including initial line</b>.
	 */
	public int linesRequired;
	
	/**
	 * Class for mapping Regex patterns to TIEvent subclasses.
	 * @param pattern Pattern in the log that means this event.
	 * @param event Class for this event (subclass of TIEvent).
	 * @param linesRequired (see below).
	 * @see TIEventData#linesRequired
	 */
	public TIEventData(String pattern, Class<? extends TIEvent> event, int linesRequired) {
		if (pattern == null) {
			regex = null;
		}
		else {
			regex = Pattern.compile(pattern);
		}
		tiEventClass = event;
		this.linesRequired = linesRequired;
	}
}
