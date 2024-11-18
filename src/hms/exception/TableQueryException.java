package hms.exception;

public class TableQueryException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public TableQueryException(String message) {
		super(message);
	}
	
	public TableQueryException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TableQueryException(Throwable cause) {
		super(cause);
	}
}
