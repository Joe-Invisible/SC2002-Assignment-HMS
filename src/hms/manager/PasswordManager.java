package hms.manager;


import java.io.IOException;
import java.util.Arrays;

import hms.HospitalManagementSystem;
import hms.exception.AccessDeniedException;
import hms.exception.AccountAccessViolationException;
import hms.exception.CommandStackViolationException;
import hms.exception.TableMismatchException;
import hms.exception.UndefinedCommandException;
import hms.exception.UndefinedVariableException;
import hms.exception.UserNotFoundException;
import hms.target.NewUserInfo;
import hms.utility.CSVHandler;
import hms.utility.TableHandler;
import hms.utility.Cryptography;



/**
 * The PasswordManager class is responsible for managing user authentication and session handling
 * in the Hospital Management System (HMS). It provides methods to check passwords, manage session keys,
 * and handle password modifications securely.
 */
public class PasswordManager extends HospitalResourceManager {
   private static PasswordManager pmInstance = null;
   private static TableHandler passwordTableHandler;
	
	// Table Variables
   private static final String ID = "ID";
   private static final String PASSWORD = "Password";
   private static final String ISNEW = "IsNew";

    /**
     * Private constructor for PasswordManager. Initializes the password file handler and loads user passwords.
     * 
     * @throws Exception if there is an error initializing the PasswordManager
     */
   private PasswordManager() throws Exception {
   	
      passwordTableHandler = new TableHandler(
         	"./res/passwords.csv", 
         	Arrays.asList(ID, PASSWORD, ISNEW),
         	0
         );
   }
	
    /**
     * Initializes the PasswordManager singleton instance.
     * 
     * @return the single instance of PasswordManager, or null if already initialized
     * @throws Exception if there is an error during initialization
     */
   public static PasswordManager PasswordManagerInit() throws Exception {
   	// allows only a single instance per process
      if (pmInstance == null) {
         pmInstance = new PasswordManager();
         return pmInstance;
      }
   	
      return null;
   }
	
    /**
     * Retrieves the password associated with the specified user ID.
     * 
     * @param uid the user ID to look up
     * @return the password associated with the user ID, or an empty string if not found
     * @throws UndefinedVariableException 
     */
   private static String getPasswordByUID(String uid) throws UndefinedVariableException {
      String hashedPassword = passwordTableHandler.readVariable(uid, PASSWORD, CSVHandler.NO_STRIP);
      
   	// will throw some UserNotFoundException
      if (hashedPassword == null) 
         return "";
      return hashedPassword;
   }
	
    /**
     * Checks the supplied password against the stored password for the user, for whether they match.
     * 
     * @param uid the hospital ID of the user attempting to log in
     * @param password the password supplied by the user
     * @return true if password matches. false otherwise.
     * @throws UndefinedVariableException 
     */
   public static boolean checkPassword(String uid, String password) throws UndefinedVariableException {
   	// something else, process by hash
	   String storedHashedPassword = getPasswordByUID(uid);
	   String inputHashedPassword = Cryptography.hash(password);
	   
	   if(storedHashedPassword.equals(inputHashedPassword))
	   {
		   return true;
	   }
   	
      return false;
   }
	
	
   public static boolean isNewUser(String hospitalId) throws UndefinedVariableException {
      return passwordTableHandler.readVariable(hospitalId, ISNEW).equals("TRUE");
   }
	
    /**
     * Adds a new user password with a default value. 
     * 
     * @param uid the user ID for which to add the password
     * @throws IOException if there is an error writing to the password file
     * @throws TableMismatchException 
     */
   public static void addPassword(String uid) throws IOException, TableMismatchException {
   	// The password default is, funnily, "password". 
   	// prompt the users to change their password immediately after first login
      final String defaultPassword = "password";
      String hashedDefaultPassword = Cryptography.hash(defaultPassword);
      passwordTableHandler.addRow(Arrays.asList(uid, hashedDefaultPassword, "TRUE"));
   }
	
	
    /**
     * Modifies the password of a user.
     * 
     * @param uid the identifier representing the user whose password is to be modified
     * @param newPassword the new password to set
     * @throws AccountAccessViolationException if the user is not logged in
     * @throws IOException 
     * @throws UserNotFoundException 
     */
   private static void modifyPassword(String uid, String newPassword) throws 
   	UndefinedVariableException, 
   	IOException, 
   	UserNotFoundException 
   {
	   passwordTableHandler.updateVariable(
         	uid, 
         	PASSWORD, 
         	newPassword
         );
   }
    
   public static class Command extends HospitalResourceManager.Command {
      public Command(String action) {
         super(action);
         super.managerDescription = "Passwords";
      }
   	
      /**
       * Executes the specified command related to password management.
       * 
       * @param hospitalId the ID of the hospital user executing the command
       * @throws AccessDeniedException if the user does not have permission to execute the command
       * @throws UndefinedCommandException if the command is not recognized
       * @throws AccountAccessViolationException if the user is not logged in
       * @throws UndefinedVariableException if the user ID or variable is undefined
       * @throws IOException if there is an error with file operations
       * @throws TableMismatchException if there is a table structure mismatch
       * @throws CommandStackViolationException if there is a violation in the command stack
       * @throws UserNotFoundException if the user is not found in the password table
       */
      public void invoke(String hospitalId) throws 
      	AccessDeniedException, 
      	UndefinedCommandException,
      	AccountAccessViolationException, 
      	UndefinedVariableException, 
      	IOException, 
      	TableMismatchException, 
      	CommandStackViolationException, 
      	UserNotFoundException 
      {
         super.setIssuerId(hospitalId);
      	// primary check, REQUIRED for all implementations.
      	// An explicit check, instead of refactoring it into the superclasses
         if(!RoleManager.checkIdHasPermission(hospitalId, super.getCommand())) super.rejectCommand();
      	// The following part of the control flow is only entered when user has enough permission
      	
         switch (super.getCommand()) {		
            case "WRITE_PERSONAL_PASSWORD" -> {
            // secondary check, because we can
               if (!HospitalManagementSystem.isLoggedIn(super.getIssuerId())) {
                  throw new AccountAccessViolationException(
                     "Illegal password modification: User not logged in."
                     );
               }
            
               promptModifyPersonalPassword(hospitalId);
            }
         
            case "ADD_USER" -> {
               provideAddUser();
            }
            
            case "REMOVE_USER" -> {
            	provideRemoveUser();
            }
         
            case "MODIFY_ANY_PASSWORD" -> {
            
            }
         
            default -> super.reportUndefinedCommand();
         }
      }
   	
      /**
       * Prompts the user to modify their personal password.
       * 
       * @param hospitalId the ID of the hospital user requesting the password change
       * @throws UndefinedVariableException if the user ID is undefined in the password table
       * @throws IOException if there is an error with file operations
       * @throws UserNotFoundException if the user is not found in the password table
       */
      private static void promptModifyPersonalPassword(String hospitalId) throws 
      	UndefinedVariableException, 
      	IOException, 
      	UserNotFoundException 
      {
         while (true) {
            System.out.print("\nPlease enter new Password\t: ");
         	
         	// this scanner used is initialized upon startup and inherited into this subclass
            String newPassword = Command.inputScanner.nextLine();
            String hashedPassword = Cryptography.hash(newPassword);
         	
            if (passwordTableHandler.readVariable(hospitalId, PASSWORD).equals(hashedPassword)) {
               System.out.println("\nYou cannot reuse the same password. Please enter another one.");
               continue;
            }
         	
            System.out.print("Please confirm password\t\t: ");
            if (!newPassword.equals(Command.inputScanner.nextLine())) {
               System.out.println("\nPasswords do not match. Please try again.");
               continue;
            }
         	
            modifyPassword(hospitalId, hashedPassword);
            System.out.println("\nSuccessfully modified password.\n");
         	
            if (isNewUser(hospitalId)) {
               passwordTableHandler.updateVariable(hospitalId, ISNEW, "FALSE");
            }
            break;
         }
      }
   	
      /**
       * Adds a new user with a default password.
       * 
       * @throws IOException if there is an error writing to the password file
       * @throws TableMismatchException if there is a mismatch in the table structure
       * @throws CommandStackViolationException if there is an issue with the command stack
       */
      private static void provideAddUser() throws 
      	IOException, 
      	TableMismatchException, 
      	CommandStackViolationException 
      {
    	  	String defaultPassword = "password";
    	  	String hashedDefaultPassword = Cryptography.hash(defaultPassword);
    	  	
	  		passwordTableHandler.addRow(
            	Arrays.asList(
            			HospitalManagementSystem.getParentTargetAs(NewUserInfo.class).hospitalId(), 
            			hashedDefaultPassword, 
            			"TRUE"
            	)
            );
      }
      
      /**
       * Removes a user from the password table.
       * 
       * @throws CommandStackViolationException if there is an issue with the command stack
       */
      private static void provideRemoveUser() throws IOException, CommandStackViolationException {
    	  passwordTableHandler.removeRow(
    		   HospitalManagementSystem.getParentTarget()
    	  );
      }
   }
}
