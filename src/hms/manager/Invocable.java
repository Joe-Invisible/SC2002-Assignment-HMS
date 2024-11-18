package hms.manager;

import hms.exception.AccessDeniedException;
import hms.exception.CommandStackViolationException;

public interface Invocable {
	/**
	 * This method is overridden by the subclasses of
	 * {@link hms.manager.HospitalResourceManager.Command Command} found 
	 * within all concrete manager classes in {@link hms.manager}.
	 * The implementations of this method will handle the permission checks
	 * and business logics of the various hospital resources.
	 * 
	 * @param hospitalId identification number of the user dispatching the command.
	 * 
	 * @throws AccessDeniedException the throw clause is generalized to allow 
	 * service-providing managers to throw different exceptions.
	 */
	public void invoke(String hospitalId) throws Exception;
	
	/**
	 * Initializes the target object of the Command, which type is implementation-specific;
	 * the most common form of such target is a String which contains the hospitalId of the 
	 * user currently being acted upon. Conventionally, unless explicitly set via a call 
	 * to this method, the field must be null, to indicate that follow up actions are neither 
	 * required nor expected. All implementations must check the parameter issuerId 
	 * appropriately against its own internal field(s) to ensure that this method is indeed 
	 * called during the processing of the issuer's command.
	 * 
	 * @param issuerId hospitalId of the issuer of the command
	 * @param targetId hospitalId of the target of the command
	 * 
	 * @throws CommandStackViolationException when the issuerId 
	 */
	public void setTarget(String issuerId, Object targetId) throws CommandStackViolationException;
	
	/**
	 * Retrieves the target of the current command as a String. This is a backward-compatible
	 * specialization of String type, of the method getTargetAs.
	 * 
	 * @return String representing the target, the content of which is implementation-specific, 
	 * always non-null; see throw clause for behavior when targetObject in implementation is null.
	 * 
	 * @throws CommandStackViolationException if a target was not explicitly specified during
	 * the processing of this command.
	 */
	public String getTarget() throws CommandStackViolationException;
	
	/**
	 * Retrieves the target of the current command; the type of target is bound by conventions between 
	 * parent and child commands. The provision of this method allows for a more flexible inter-command
	 * communication.
	 * 
	 * @param <T> the type of the target; ideally an independently defined class
	 * @param _class class of the target
	 * @return the object of type as specified in the argument
	 */
	public <T> T getTargetAs(Class<T> expectedType) throws CommandStackViolationException;

	/**
	 * Gets the name of this Invocable object, which is implementation specific. Conventionally,
	 * this should reflect the name or functionality of the class that provided such implementation.
	 * 
	 * @return the name of the Invocable object
	 */
	public String getName();
}
