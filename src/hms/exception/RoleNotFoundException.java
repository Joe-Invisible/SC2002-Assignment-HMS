package hms.exception;


public class RoleNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public RoleNotFoundException(String message) {
		super(message);
	}
	
	public RoleNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public RoleNotFoundException(Throwable cause) {
		super(cause);
	}
}
