package hms.exception;

public class AccountAccessViolationException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public AccountAccessViolationException(String message) {
		super(message);
	}
	
	public AccountAccessViolationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public AccountAccessViolationException(Throwable cause) {
		super(cause);
	}
}
