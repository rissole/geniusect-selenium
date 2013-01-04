package seleniumhelper;

public class NoSuchChoiceException extends Exception {
	private static final long serialVersionUID = 1L;

	public NoSuchChoiceException(String message) {
        super(message);
    }
	
	public NoSuchChoiceException() {
	}
}
