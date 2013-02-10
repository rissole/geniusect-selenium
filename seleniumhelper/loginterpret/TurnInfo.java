package seleniumhelper.loginterpret;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import seleniumhelper.loginterpret.events.TIEvent;

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
		List<String> strEvents = Arrays.asList(turnHTML.replaceAll("(</h2>|</div>)", "$1\n").split("\n"));
		Iterator<String> itr = strEvents.iterator();
		while (itr.hasNext()) {
			TIEvent event = TIEvent.create(itr);
			if (event != null) {
				events.add(event);
			}
		}
	}
	
	public ArrayList<TIEvent> getEvents() {
		return events;
	}
	
}
