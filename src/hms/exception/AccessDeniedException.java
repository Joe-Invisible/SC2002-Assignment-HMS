package hms.exception;

public class AccessDeniedException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public AccessDeniedException(String message) {
		super(message);
	}
	
	public AccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public AccessDeniedException(Throwable cause) {
		super(cause);
	}
}