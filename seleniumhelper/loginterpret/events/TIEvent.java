package seleniumhelper.loginterpret.events;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * Superclass that represents one "event" from Showdown.
 * Subclasses contain interpreted event data in an easy to use format.
 * Generally speaking, each move is its own event, and 
 * each switch is its own event.
 * @author burse
 *
 */
public abstract class TIEvent {
	
	/**
	 * Map of regexs to TIEvent subclass objects
	 * Simply add a new entry in this map to create a new event.
	 * First parameter is the regex applied to the line in the battle log- if this
	 * matches, a new TIEvent instance is created, specifically an instance of
	 * the class in the second parameter.<p>
	 * 
	 * The second entry must have regex=null and will be selected if no regex match is found.
	 * class=null indicates to skip that line upon a match.<p>
	 *
	 * The number of additional lines required from the battle log
	 * to create this TIEvent object. Special
	 * <code>TIEventData.REQUIRES_UNTIL_SPACER</code> indicates lines required
	 * are up to the next spacer div, h2 tag, or turn end (which is often the case
	 * with Showdown).
	 * Otherwise it is an integer specifying how many lines from the log should
	 * be read for the event, <b>including initial line</b>.
	 */
	public static final TIEventData[] EVENTMAP = {
		new TIEventData("^<h2>Turn|^<div class=\"spacer\">", null, 0),
		new TIEventData("^<div>.+? called .+? back!</div>$", TIChangeEvent.class, 3),
		new TIEventData(null, TIUnknownEvent.class, TIEventData.REQUIRES_UNTIL_SPACER),
	};
	
	protected String eventText;
	
	/**
	 * DO NOT CALL
	 * @see TIEvent#create(Iterator)
	 */
	public TIEvent(String eventText) {
		this.eventText = eventText;
	}
	
	/**
	 * Creates a new Turn Info event based on the current line of the battle
	 * log HTML we are looking at.
	 * @param itr String iterator of the battle log HTML.<br>
	 * This is always incremented at least once.
	 * @return TIEvent subclass or null if this line should be skipped.
	 */
	public static TIEvent create(Iterator<String> itr) {
		// Determine what kind of event this line is.
		String strEvent = itr.next();
		TIEventData eventData = TIEvent.findEventByMatch(strEvent);
		
		// eventData.tiEventClass being null means we skip this line
		if (eventData.tiEventClass == null) {
			return null;
		}
		
		// GET EVENT TEXT
		// init with our first line
		String eventText = strEvent;
		
		// Get extra lines required: case 1: until next spacer, h2, or end of stream.
		if (eventData.linesRequired == TIEventData.REQUIRES_UNTIL_SPACER) {
			while (itr.hasNext()) {
				String line = itr.next();
				if (!line.matches("^<div class=\"spacer\">.*?</div>$") &&
					!line.matches("^<h2>.*?</h2>$")) {
					eventText += "\n" + line;
				}
				else {
					break;
				}
			}
		}
		// case 2: int - just number of lines required
		else {
			// linesRequired-1 because we have already appended the first line
			for (int j = 0; itr.hasNext() && j < eventData.linesRequired-1; ++j) {
				eventText += itr.next();
			}
		}
		
		// INITIALISE EVENT OBJECT
		TIEvent event = null;
		try {
			event = (TIEvent) eventData.tiEventClass.getConstructors()[0].newInstance(eventText);
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		} catch (SecurityException e) {
			return null;
		}
		return event;
	}
 
	/**
	 * Find an event class for our text line
	 * @param firstLine Line we've read from the log
	 * @return Matching TIEventData from EVENT_MAP
	 */
	private static TIEventData findEventByMatch(String firstLine) {
		for (TIEventData ted : EVENTMAP) {
			// regex should be null if we've traverse the entire list
			if (ted.regex == null || ted.regex.matcher(firstLine).find()) {
				return ted;
			}
		}
		return null;
	}
	
	public String getEventText() {
		return eventText;
	}
	
	public String toString() {
		return getClass().getSimpleName()+"|"+getEventText();
	}
}
