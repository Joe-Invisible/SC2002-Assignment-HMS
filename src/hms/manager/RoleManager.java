package hms.manager;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import hms.HospitalManagementSystem;
import hms.exception.AccessDeniedException;
import hms.exception.CommandStackViolationException;
import hms.exception.RoleNotFoundException;
import hms.exception.TableMismatchException;
import hms.exception.UndefinedVariableException;
import hms.exception.UserNotFoundException;
import hms.target.NewUserInfo;
import hms.utility.Action;
import hms.utility.PromptFormatter;
import hms.utility.TableHandler;


/*
 * Responsibilities:
 * 		Bookkeeping of all User's roles
 * 		Verification of User's permissions to perform certain action
 */
public class RoleManager {
	// Components of Role objects
	//			roleName : allowedActions
   private static Map<String, List<Action>> roleTable;
	//			Id : Role
   private static Map<String, Role> userRoles;
	
   private static RoleManager rmInstance = null;
	
   private static TableHandler userRoleTableHandler;
   private static TableHandler permissionTableHandler;
	
   private static final String ID = "ID";
   private static final String ROLE = "Role";
   private static final String PERMISSIONS = "Permissions";
   private static List<Role> appointableRoles;

	// Access level common to all users (e.g., r/w on own profile)
   private static final String COMMON_ACCESS = "CommonAccess";
	
   /**
    * Private constructor for the RoleManager class. Initializes the role and permission tables.
    * 
    * @throws Exception if there is an error initializing the RoleManager
    */
   private RoleManager() throws Exception {
   	// These files are smaller relative to users.csv
      userRoleTableHandler = new TableHandler(
         	"./res/userRoles.csv", 
         	Arrays.asList(ID, ROLE),
         	0
         );
      permissionTableHandler = new TableHandler(
         	"./res/permissions.csv",
         	Arrays.asList(ROLE, PERMISSIONS),
         	0
         );
   	
      roleTable = permissionTableHandler.readTwoColumns(
         	ROLE, 
         	PERMISSIONS, 
         	s -> Arrays.asList(s.split(";"))
         			.stream()
         			.map(a -> new Action(a))
         			.toList()
         );
   	
      userRoles = userRoleTableHandler.readTwoColumns(
         	ID, 
         	ROLE, 
         	s -> new Role(s)
         );
      
      appointableRoles = Arrays.asList(new Role("Doctor"));
   }
   
   /**
    * Checks if a user with the given hospital ID has a role that is considered appointable.
    * 
    * @param hospitalId the ID of the user to check
    * @return true if the user has an appointable role, false otherwise
    */	
   public static boolean isAppointableUser(String hospitalId) {
	   return appointableRoles.contains(userRoles.get(hospitalId));
   }
	
   /**
    * Initializes the singleton instance of the RoleManager class.
    * 
    * @return the singleton instance of RoleManager if not already initialized, otherwise null
    * @throws Exception if there is an error during initialization
    */
   public static RoleManager RoleManagerInit() throws Exception {
   	// Allows only a single instance per process
      if (rmInstance == null) {
         rmInstance = new RoleManager();
         return rmInstance;
      }
   	
      return null;
   }
	
   /**
    * The Role class represents a role in the system. It contains the role name and the list of actions allowed for that role.
    */
   private static class Role {
      private final String role;
      private List<Action> allowedActions;
      
   	/**
   	 * Constructs a Role object that provide role-specific permissions verification.
   	 * @param role the name of the role as a String. Possible values of which can
   	 * be referred to from ./res/permissions.csv. If a undefined role name is supplied,
   	 * this.allowedActions will be null.
   	 */
      public Role(String role) {
         this.role = role;
         this.allowedActions = roleTable.get(role);
      }
   	
      /**
       * Gets the name of the role associated with this object.
       * @return the name of the role as a String
       */
      private String getName() {
         return this.role;
      }
      
      /**
       * Checks whether the role assignment (the calling of Role constructor)
       * resulted in a valid Role object, in which this.allowedAction points
       * to a valid list permissions.
       * @return true if the assignment was valid; false otherwise
       */
      private boolean wasValidAssignment() {
    	  return this.allowedActions != null;
      }
   	
      /**
       * Performs permission verification on the supplied action.
       * @param action the action to which the role-holder's eligibility is 
       * to be checked against.
       * @return true if the role-holder is allowed to perform said action;
       * false otherwise.
       */
      private boolean isAllowed(Action action) {
         return this.allowedActions.contains(action);
      }
   	
      @Override
      public int hashCode() {
         return this.role.hashCode();
      }
   	
      @Override
      public boolean equals(Object o) {
         return o instanceof Role && this.role.equals(((Role)o).getName());
      }
   }
	
	/**
	 * Checks whether the supplied role name corresponds to a valid role.
	 * @param role the name of the role to be checked against.
	 * @return
	 */
   public static boolean isValidRole(String role) {
      return roleTable.containsKey(role);
   }
   
   
   /**
    * Assigns a new role to a user.
    * 
    * @param hospitalId the ID of the user to assign the role to
    * @param newRole the new role to assign to the user
    * @throws RoleNotFoundException if the role is not found
    * @throws IOException if there is an error writing to the role file
    * @throws UndefinedVariableException if the user role is undefined
    * @throws UserNotFoundException if the user is not found
    */
   public static void assignRole(String hospitalId, Role newRole) throws 
   		RoleNotFoundException, 
   		IOException, 
   		UndefinedVariableException, 
   		UserNotFoundException 
   {
      userRoles.put(hospitalId, newRole);
      userRoleTableHandler.updateVariable(hospitalId, ROLE, newRole.getName());
   }
	
   /**
    * Checks if the user with the given hospital ID has the permission to perform the specified action.
    * 
    * @param hospitalId the ID of the user
    * @param action the action to check for permission
    * @return true if the user has permission, false otherwise
    */
   public static boolean checkIdHasPermission(String hospitalId, String action) {
   	
   	// He's is not gonna do much harm with this action anyways
      if (roleTable.get(COMMON_ACCESS).contains(new Action(action))) 
         return true;
   	
      return userRoles.get(hospitalId).isAllowed(new Action(action));
   }
	
   /**
    * Retrieves the role name of a user based on their hospital ID.
    * 
    * @param hospitalId the ID of the user
    * @return the role name of the user
    */
   public static String getRoleName(String hospitalId) {
      return userRoles.get(hospitalId).getName();
   }
	
	
   public static class Command extends HospitalResourceManager.Command { 
      public Command(String action) {
         super(action);
         super.managerDescription = "Roles";
      }
   	
      public void invoke(String hospitalId) throws 
      	Exception 
      {
         super.setIssuerId(hospitalId);
         if(!RoleManager.checkIdHasPermission(hospitalId, super.getCommand())) super.rejectCommand();
      	// The following part of the control flow is only entered when user has enough permission
      	
         switch (super.getCommand()) {
            case "ADD_USER" -> {
               promptAddUser();
            }
            
            case "REMOVE_USER" -> {
            	provideRemoveUser();
            }
            
            case "WRITE_ROLE" -> {
            	promptUpdateRole(hospitalId);
            }
         
            default -> super.reportUndefinedCommand();
         }
      }
   }
   
   /**
    * Prompts the admin to update the role of a user.
    * 
    * @param adminId the ID of the admin updating the role
    * @throws Exception if there is an error while updating the role
    */
   private static void promptUpdateRole(String adminId) throws 
   		Exception 
   {
	   String affectedId = HospitalManagementSystem.getParentTarget();
	   
	   Role newRole = new PromptFormatter.InputSession<Role>("Please Enter new Role")
	   .setConverter(s -> new Role(s))
	   .setValidator(r -> r.wasValidAssignment())
	   .setOnInvalidInput("Not a valid role. Please choose from\n" + String.join(" ", roleTable.keySet()))
	   .startPrompt();
	   
	   if (newRole == null) return;
	   
	   if (appointableRoles.contains(userRoles.get(affectedId)) && !appointableRoles.contains(newRole)) {
		   HospitalManagementSystem.setTarget(adminId, affectedId);
		   HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("REMOVE_USER"));
	   }
	   
	   assignRole(affectedId, newRole);
   }
	 
   /**
    * Removes a user from the system.
    * 
    * @throws CommandStackViolationException if there is an issue with the command stack
    * @throws IOException if there is an error with file operations
    */
   private static void provideRemoveUser() throws CommandStackViolationException, IOException {
	   String removalId = HospitalManagementSystem.getParentTarget();
	   userRoles.remove(removalId);
	   userRoleTableHandler.removeRow(removalId);
   }
   
   /**
    * adds a user to the system.
    * 
    * @throws CommandStackViolationException if there is an issue with the command stack
    * @throws IOException if there is an error with file operations
    * @throws AccessDeniedException if the admin does not have the necessary permissions
    * @throws CommandStackViolationException if there is an issue with the command stack
    */
   private static void promptAddUser() throws 
    	IOException, 
    	TableMismatchException, 
    	AccessDeniedException, 
    	CommandStackViolationException 
   {
      NewUserInfo affectedUser = HospitalManagementSystem.getParentTargetAs(NewUserInfo.class);
   	  String hospitalId = affectedUser.hospitalId();
   	  String roleName = affectedUser.roleName();
   	  
      while (true) {
         if (isValidRole(roleName)) {
            userRoles.put(hospitalId, new Role(roleName));
         	
            userRoleTableHandler.addRow(Arrays.asList(hospitalId, roleName));
            break;
         }
      	 
         System.out.println(roleName + " is not a valid role. Please choose from:");
      
         for (String validName: roleTable.keySet()) {
            System.out.print(validName + " ");
         }
         System.out.println();
         System.out.print("Role	: ");
         roleName = Command.inputScanner.nextLine();
      }
   }
}
