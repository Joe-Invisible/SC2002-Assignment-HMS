package hms.exception;

public class CommandStackViolationException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public CommandStackViolationException(String message) {
		super(message);
	}
	
	public CommandStackViolationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public CommandStackViolationException(Throwable cause) {
		super(cause);
	}
}
