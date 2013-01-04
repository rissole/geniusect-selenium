package seleniumhelper;

public class IsTrappedException extends Exception {
	private static final long serialVersionUID = 1L;

	public IsTrappedException(String message) {
        super(message);
    }

	public IsTrappedException() {
		super("You are trapped and cannot switch!");
	}
}
