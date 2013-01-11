package seleniumhelper;

public class InvalidTeamException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidTeamException(String message) {
        super(message);
    }
	
	public InvalidTeamException() {
	}
}
