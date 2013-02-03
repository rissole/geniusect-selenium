package seleniumhelper.loginterpret;

import java.util.ArrayList;

public class TurnInfo {
	
	private String turnHTML;
	private TIContext context;
	private ArrayList<TIEvent> events;
	
	public TurnInfo(String turnHTML, TIContext context) {
		this.turnHTML = turnHTML;
		this.context = context;
		this.events = new ArrayList<TIEvent>();
		interpret();
	}
	
	public TIContext getContext() {
		return context;
	}
	
	/**
	 * Gets the HTML that derived this TurnInfo
	 * @return String - HTML passed into this TurnInfo.
	 */
	public String getTurnHTML() {
		return turnHTML;
	}
	
	private void interpret() {
		String strEvents[] = turnHTML.replaceAll("(</h2>|</div>)", "$1\n").split("\n");
		for (int i = 0; i < strEvents.length;) {
			String strEvent = strEvents[i];
			TIEvent event = new TIEvent(strEvent);
			if (event.requires() == TIEvent.REQUIRES_SKIPPING) {
				++i;
				continue;
			}
			else if (event.requires() == TIEvent.REQUIRES_UNTIL_SPACER) {
				i += append_until_spacer(event, strEvents, i);
			}
			else {
				int j = 1; 
				for (; j < strEvents.length-i && j < event.requires(); ++j) {
					event.appendToEvent(strEvents[i+j]);
				}
				i += j;
			}
			events.add(event);
		}
	}
	
	private int append_until_spacer(TIEvent event, String[] strEvents, int currentLine) {
		int j = 1;
		for (; j < strEvents.length-currentLine; ++j) {
			if (!strEvents[currentLine+j].matches("^<div class=\"spacer\">.*?</div>$") &&
				!strEvents[currentLine+j].matches("^<h2>.*?</h2>$")) {
				event.appendToEvent(strEvents[currentLine+j]);
			}
			else {
				++j; //ignore the actual "spacer"/h2 line
				break;
			}
		}
		return j;
	}
	
	public ArrayList<TIEvent> getEvents() {
		return events;
	}
	
}
