package seleniumhelper.loginterpret;

public class TurnInfo {
	
	private String turnHTML;
	
	public TurnInfo(String turnHTML) {
		this.turnHTML = turnHTML;
		interpret();
	}
	
	/**
	 * Gets the HTML that derived this TurnInfo
	 * @return String - HTML passed into this TurnInfo.
	 */
	public String getTurnHTML() {
		return turnHTML;
	}
	
	private void interpret() {
	}
	
}
