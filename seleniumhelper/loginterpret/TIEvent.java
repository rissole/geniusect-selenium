package seleniumhelper.loginterpret;

import java.util.Iterator;

public class TIEvent {
	
	/**
	 * Map of regexs to TIEvent subclass objects
	 */
	public static final TIEventData[] EVENTMAP = {
		new TIEventData("^<h2>Turn|^<div class=\"spacer\">", null),
		new TIEventData("^<div>.+? called .+? back!</div>$", TIChangeEvent.class),
		new TIEventData(null, TIEvent.class)
	};
	
	protected int linesRequired;
	protected String eventText;
	
	protected TIEvent() {
		linesRequired = TIEventData.REQUIRES_UNTIL_SPACER;
		this.eventText = "";
	}
	
	/**
	 * Creates a new Turn Info event.
	 * @param itr String iterator of the battle log HTML.<br>
	 * This is always incremented at least once.
	 * @return TIEvent or subclass.
	 */
	public static TIEvent create(Iterator<String> itr) {
		String strEvent = itr.next();
		TIEvent event = TIEvent.findEventByMatch(strEvent);
		
		// skip.
		if (event == null) {
			return null;
		}
		
		// initialise event text
		event.eventText = strEvent;
		
		// until next spacer, h2, or end of stream.
		if (event.requires() == TIEventData.REQUIRES_UNTIL_SPACER) {
			while (itr.hasNext()) {
				String line = itr.next();
				if (!line.matches("^<div class=\"spacer\">.*?</div>$") &&
					!line.matches("^<h2>.*?</h2>$")) {
					event.appendToEvent(line);
				}
				else {
					break;
				}
			}
		}
		// int - just this number of lines.
		else {
			int j = 0; 
			// -1 because we count the line we've already eaten
			for (; itr.hasNext() && j < event.requires()-1; ++j) {
				event.appendToEvent(itr.next());
			}
		}
		return event;
	}
 
	/**
	 * Find an event class for our text line
	 * @param firstLine
	 * @return
	 */
	private static TIEvent findEventByMatch(String firstLine) {
		for (TIEventData ted : EVENTMAP) {
			// if we have traversed the list entirely...
			if (ted.regex == null) {
				try {
					return (TIEvent)ted.tiEventClass.newInstance();
				} catch (InstantiationException e) {
					return null;
				} catch (IllegalAccessException e) {
					return null;
				}
			}
			// if it matches
			else if (ted.regex.matcher(firstLine).find()) {
				try {
					return (ted.tiEventClass != null ?
							(TIEvent)ted.tiEventClass.newInstance() : null);
				} catch (InstantiationException e) {
					return null;
				} catch (IllegalAccessException e) {
					return null;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the number of extra lines required for the event.
	 * @return int - number of lines required, or:<br>
	 * <code>REQUIRES_UNTIL_SPACER<br>
	 * REQUIRES_SKIPPING</code>
	 */
	public int requires() {
		return linesRequired;
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
