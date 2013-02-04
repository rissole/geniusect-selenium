package seleniumhelper.loginterpret;

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
	public Class<?> tiEventClass;
	
	/**
	 * Class for mapping Regex patterns to TIEvent subclasses.
	 * @param pattern Pattern in the log that means this event.
	 * @param event Class for this event.
	 */
	public TIEventData(String pattern, Class<?> event) {
		if (pattern == null) {
			regex = null;
		}
		else {
			regex = Pattern.compile(pattern);
		}
		tiEventClass = event;
	}
}
