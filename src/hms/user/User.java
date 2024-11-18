package hms.user;

import hms.HospitalManagementSystem;
import hms.manager.PasswordManager;
import hms.manager.UserManager;


/**
 * Common interface for all classes of user
 */
public abstract class User {
	/**
     * Displays the user interface menu and handles user input for navigation.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
   public abstract void enterUI() throws Exception;

   /**
    * Shows the menu common to all users irrespective of role.
    */
   protected void usersCommonMenu() {
      System.out.println("-vp	View Profile");
      System.out.println("-up	Update Personal Information");
      System.out.println("-ps	Change password");
      System.out.println("-lo	Logout");
   }
	
	/**
	 * Handles the option fall-through of any subclass (options does not match their own menus).
	 * 
	 * @param option the option that none of the subclass's switch cases matched
	 * @return true if the option corresponds to a Logout operation. false otherwise. Effectively a isLogout signal.
	 * @throws Exception
	 */
   protected boolean handleCommonOptions(String option) throws Exception {
      switch (option) {
      
         case "-vp" -> {
            HospitalManagementSystem.dispatchCommand(new UserManager.Command("READ_PERSONAL_PROFILE"));
         }
      
         case "-up" -> {
            HospitalManagementSystem.dispatchCommand(new UserManager.Command("WRITE_PERSONAL_PROFILE"));
         }
      
         case "-ps" -> {
            HospitalManagementSystem.dispatchCommand(new PasswordManager.Command("WRITE_PERSONAL_PASSWORD"));
         }
      
         case "-lo" -> {
            System.out.println("\nThank you. Please visit again.");
            return true;
         }
      
         default -> {
            System.out.println("Invalid option.");
         }
      
      }
   	
      return false;
   }
}