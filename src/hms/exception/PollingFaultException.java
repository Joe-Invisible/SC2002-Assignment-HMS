package hms.exception;

public class PollingFaultException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public PollingFaultException(String message) {
		super(message);
	}
	
	public PollingFaultException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public PollingFaultException(Throwable cause) {
		super(cause);
	}
}
