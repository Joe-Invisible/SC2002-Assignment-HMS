package hms.exception;

public class UndefinedVariableException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public UndefinedVariableException(String message) {
		super();
	}
	
	public UndefinedVariableException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public UndefinedVariableException(Throwable cause) {
		super(cause);
	}
}
