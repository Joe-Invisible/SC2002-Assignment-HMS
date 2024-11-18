package hms.exception;

public class UndefinedCommandException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public UndefinedCommandException(String message) {
		super(message);
	}
	
	public UndefinedCommandException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public UndefinedCommandException(Throwable cause) {
		super(cause);
	}
}
