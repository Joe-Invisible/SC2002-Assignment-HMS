package hms.manager;

import java.util.Scanner;

import hms.exception.AccessDeniedException;
import hms.exception.CommandStackViolationException;
import hms.exception.UndefinedCommandException;

/**
 * The HospitalResourceManager class provides a framework for managing 
 * hospital resource commands and their associated logic. It contains an abstract
 * inner class code Command, which serves as the base for all commands that
 * can be invoked in the system.
 * 
 * <p>This class is designed to handle the initialization, validation, and execution
 * of commands while ensuring proper security and type safety for the target objects
 * associated with each command.</p>
 */
public class HospitalResourceManager {
	/**
	 * This is a containment abstract class within HospitalResourceManager,
	 * which covers all the general features a command object might need,
	 * including, invoking the command, setting/getting the targetObject, 
	 * and rejecting the command upon access violation. This class is to
	 * be extended by concrete command classes within service-providing 
	 * manager classes to realize specific role verification and service 
	 * multiplexing.
	 */
   public abstract static class Command implements Invocable {
      private String command;
      private String issuerId;
      private Object targetObject;
      protected String managerDescription;
      /**
       * The public Scanner object with which the manager classes
       * can prompt for input without having to manage their own.
       */
      public static Scanner inputScanner;
   	
      /**
       * Constructs a command object that represents a request to a 
       * service-providing manager class.
       * 
       * @param action the name of the action as a String literal.
       * The value of which can be referenced from ./res/permissions.csv
       */
      public Command(String action) {
         this.command = action;
         this.targetObject = null;
         this.issuerId = null;
         this.managerDescription = "Abstract Manager";
      }
   	
      /**
       * Initializes a Scanner object for the service-providing
       * managers to use in prompting processes.
       * 
       * @param in a reference to an opened Scanner. This scanner
       * cannot be closed if commands are still to be invoked.
       */
      public static void CommandInit(Scanner in) {
         inputScanner = in;
      }
   	
      /**
       * Retrieves the name of the command action.
       * 
       * @return the command action name.
       */
      public String getCommand() {
         return this.command;
      }
   	
   	/**
   	 * Initializes the field issuerId, which is the hospitalId of the 
   	 * user who dispatched this command
   	 * @param hospitalId id of the issuer of the command
   	 */
      protected void setIssuerId(String hospitalId) {
         this.issuerId = hospitalId;
      }
   	
      /**
       * Retrieves the hospital ID of the user who dispatched this command.
       * 
       * @return the hospital ID of the command issuer.
       */
      protected String getIssuerId() {
         return this.issuerId;
      }
      
      /**
       * Sets the target object for this command, ensuring the issuer ID matches
       * the command's original dispatcher.
       * 
       * @param issuerId the ID of the command dispatcher.
       * @param targetId the object to be set as the command's target.
       * @throws CommandStackViolationException if the issuer ID does not match 
       *         the original dispatcher.
       */
      public final void setTarget(String issuerId, Object targetId) throws 
      	CommandStackViolationException 
      {
         if (!issuerId.equals(this.issuerId)) throw new CommandStackViolationException(
               "Command Stack Access Violation: Command dispatched by " + 
               issuerId + 
               " was attempting to modify the target of an unrelated Command."
         );
         this.targetObject = targetId;
      }
   
      /**
       * Performs checks on targetObject field against the supplied class, for whether 
       * the targetObject is not null, and whether the targetObject is an instance
       * of the class as supplied. This is a wrapper for the internal safe down-casting logic.
       * 
       * @param <T> type to check targetObject against
       * @param expectedType an instance of Class; callers may obtain this information by TypeName.class
       * @throws CommandStackViolationException
       */
      private final <T> void checkTargetObjectType(Class<T> expectedType) throws CommandStackViolationException {
         if (this.targetObject == null) throw new CommandStackViolationException(
                  "Command Stack Access Violation: Parent command did not specify target."
               );
      
         if (!expectedType.isInstance(this.targetObject)) throw new CommandStackViolationException(
               "Command Stack Access Violation: Child command " +
               this.command 									+
               " expects " 										+ 
               expectedType.getSimpleName() 					+ 
               " target. Got "									+
               this.targetObject.getClass().getSimpleName()		+
               " instead."
               
               );
      }
      
      /**
       * Retrieves the target object of this command as a String.
       * 
       * @return the target object as a String.
       * @throws CommandStackViolationException if the target object is not set or 
       *         is not of type String.
       */
      public final String getTarget() throws CommandStackViolationException {
         this.checkTargetObjectType(String.class);
         
         return (String)this.targetObject;
      }
      
      /**
       * Retrieves the name of the command's manager description.
       * 
       * @return the manager description associated with this command.
       */
      public final String getName() {
         return this.managerDescription;
      }
      
      /**
       * Retrieves the target object as an instance of the specified type.
       * 
       * @param <T> the expected type of the target object.
       * @param expectedType the Class of the expected type.
       * @return the target object cast to the specified type.
       * @throws CommandStackViolationException if the target object is not set or 
       *         is not of the expected type.
       */
      public <T> T getTargetAs(Class<T> expectedType) throws CommandStackViolationException {
         this.checkTargetObjectType(expectedType);
        
         return (expectedType.cast(this.targetObject));
      }
      
      /**
       * Rejects the command by throwing an {@link AccessDeniedException}.
       * 
       * @throws AccessDeniedException indicating that the command is not allowed.
       */
      public void rejectCommand() throws AccessDeniedException {
         throw new AccessDeniedException(
            	"Access Denied or Undefined Command: User (hospitalId: " 	+ 
            	this.issuerId 												+ 
            	") is not allowed to perform " 								+ 
            	this.command
            );
      }
   	
      /**
       * Rejects the command with a custom reason by throwing an {@link AccessDeniedException}.
       * 
       * @throws AccessDeniedException indicating that the command is not allowed.
       */
      public void rejectCommand(String reason) throws AccessDeniedException {
         throw new AccessDeniedException(reason);
      }
   	
      /**
       * Reports an undefined command by throwing an {@link UndefinedCommandException}.
       * 
       * @throws UndefinedCommandException indicating that the command is not recognized.
       */
      public void reportUndefinedCommand() throws UndefinedCommandException {
         throw new UndefinedCommandException(
            	"Undefined Command: " 				+ 
            	this.getCommand() 					+ 
            	" is not a recognised command by " 	+ 
            	this.getClass().getName()
            );
      }
   }

}
