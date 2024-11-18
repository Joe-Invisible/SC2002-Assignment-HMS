package hms.exception;

public class TableMismatchException extends Exception {
private static final long serialVersionUID = 1L;
	
	public TableMismatchException(String message) {
		super(message);
	}
	
	public TableMismatchException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TableMismatchException(Throwable cause) {
		super(cause);
	}
}
