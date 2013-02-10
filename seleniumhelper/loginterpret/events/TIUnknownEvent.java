package seleniumhelper.loginterpret.events;

public class TIUnknownEvent extends TIEvent {

	/**
	 * DO NOT CALL
	 * @see TIEvent#create
	 */
	public TIUnknownEvent(String eventText) {
		super(eventText);
		//System.out.println("Unknown event discovered: " + this.toString());
	}

}
