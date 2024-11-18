package hms;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

import hms.exception.AccessDeniedException;
import hms.exception.CommandStackViolationException;
import hms.manager.AppointmentManager;
import hms.manager.HospitalResourceManager;
import hms.manager.Invocable;
import hms.manager.MedicalRecordManager;
import hms.manager.MedicationStockManager;
import hms.manager.PasswordManager;
import hms.manager.RoleManager;
import hms.manager.UserManager;
import hms.user.User;
import hms.utility.PromptFormatter;

public class HospitalManagementSystem {
	/* Self */
	private static HospitalManagementSystem hmsInstance = null;

	/* Manager Instances (Unused, Reserved) */
	private static RoleManager roleManagerInstance;
	private static PasswordManager passwordManagerInstance;
	private static UserManager userManagerInstance;
	private static MedicalRecordManager medicalRecordManagerInstance;
	private static AppointmentManager appointmentManagerInstance;
	private static MedicationStockManager medicationStockManagerInstance;
	
	/* Execution State Fields */
	private static Deque<Invocable> commandStack;
	private static Deque<String> executionDirectory;
	private static String activeUserHospitalId;
	private static User activeUser;
	private static Scanner inputScanner;
	
	private HospitalManagementSystem() {
		hmsInstance = this;
		activeUserHospitalId = null;

		try {
			roleManagerInstance = RoleManager.RoleManagerInit();
			passwordManagerInstance = PasswordManager.PasswordManagerInit();
			userManagerInstance = UserManager.UserManagerInit();
			medicalRecordManagerInstance = MedicalRecordManager.MedicalRecordManagerInit();
			appointmentManagerInstance = AppointmentManager.AppointmentManagerInit();
			medicationStockManagerInstance = MedicationStockManager.MedicationStockManagerInit();

			commandStack = new ArrayDeque<Invocable>();
			executionDirectory = new ArrayDeque<String>();

			inputScanner = new Scanner(System.in);
			HospitalResourceManager.Command.CommandInit(inputScanner);
			PromptFormatter.PromptFormatterInit(inputScanner, executionDirectory);

		} catch (Exception e) {
			System.err.println("Hospital Management System Startup Failure.");
			System.err.println("Reason: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Initialises the HospitalManagementSystem (HMS). This invokes
	 * the initialization procedures of all required managers as well.
	 * @return a reference to this singleton instance
	 */
	public static HospitalManagementSystem HospitalManagementSystemInit() {
		if (hmsInstance == null) {
			hmsInstance = new HospitalManagementSystem();
			return hmsInstance;
		}
		return null;
	}
	
	/**
	 * Sets a target for the currently running command. 
	 * @param issuerId the hospitalId of the issuer of this command
	 * @param targetObject the object containing all the required information
	 * the subsequent commands may need to complete their services. The exact
	 * type of which is determined by the convention between co-operating 
	 * manager classes.
	 * @throws CommandStackViolationException when the issuerId does not
	 * match the currently active user's hospitalId
	 */
	public static void setTarget(String issuerId, Object targetObject) throws CommandStackViolationException {
		commandStack.peek().setTarget(issuerId, targetObject);
	}

	
	/**
	 * Gets the parent command's targetObject as if it is of type string.
	 * Provided for backward compatibility.
	 * @return the targetObject as String
	 * @throws CommandStackViolationException when the object type 
	 * of targetObject is not String, this exception is thrown in place
	 * of the runtime class-casting exception.
	 */
	public static String getParentTarget() throws CommandStackViolationException {
		return getParentTargetAs(String.class);
	}


	/**
	 * Gets the parent command's targetObject as if it is of type T
	 * @param <T> the expected type of the targetObject
	 * @param expectedType the Class instance representing the expected
	 * type of the targetObject
	 * @return the targetObject as type T
	 * @throws CommandStackViolationException when targetObject cannot
	 * be cast to T.
	 */
	public static <T> T getParentTargetAs(Class<T> expectedType) throws CommandStackViolationException {
		if (commandStack.size() <= 1) throw new CommandStackViolationException(
					"Command Stack Singular or Empty: This command has no parents, what are you trying to do?"
		);

		// examine the one command below the current command (its parent)
		Invocable currentCommand = commandStack.pop();
		T parentTarget = commandStack.peek().getTargetAs(expectedType);
		commandStack.push(currentCommand);

		return parentTarget;
	}

	/**
	 * Dispatch a command to the appropriate manager, via calling the invoke
	 * method of the supplied command object.
	 * @param command the command
	 * @throws Exception 
	 */
	public static void dispatchCommand(Invocable command) throws Exception {
		try {
			commandStack.push(command);
			executionDirectory.push(command.getName());
			command.invoke(activeUserHospitalId);
		} catch (AccessDeniedException e) {
			System.err.println("\n" + e.getMessage());
			// future plan to call special error screen upon access violation
			return;
		} catch (CommandStackViolationException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} finally { // other kinds of exceptions should be handled by the Managers themselves
			commandStack.pop();
			executionDirectory.pop();
		}
	}

	/**
	 * Checks whether the said hospitalId is logged in
	 * @param hospitalId id of the user to be checked
	 * @return true if the user is logged in; false otherwise.
	 */
	public static boolean isLoggedIn(String hospitalId) {
		return hospitalId.equals(activeUserHospitalId);
	}

	/*** HMS Entry Point ***/
	public static void main(String[] args) throws Exception {
		System.out.println("Initialising System...");
		HospitalManagementSystemInit();

		final Scanner inputScannerFinalDeclaration = inputScanner; // workaround
		try (inputScannerFinalDeclaration) {

			/*** System Loop (outermost application loop) ***/
			while (true) {
				promptLogin(inputScanner);

				System.out.println("\nWelcome to HMS!\n");
				if (PasswordManager.isNewUser(activeUserHospitalId)) {
					
					System.out.println("But first, you need to change your password.");
					
					dispatchCommand(new PasswordManager.Command("WRITE_PERSONAL_PASSWORD"));
				}

				executionDirectory.push(activeUserHospitalId + "@" + RoleManager.getRoleName(activeUserHospitalId));
				// Login process successful. Passing control to active user
				/*** User Loop (secondary loop), exits when user logs out ***/
				activeUser.enterUI();

				/*** Session ended ***/
				executionDirectory.pop();
				activeUser = null;
				activeUserHospitalId = null;

				PromptFormatter.clearScreen();
			}
		}
	}

	private static void promptLogin(Scanner in) throws Exception {
		String hospitalId = null;
		String password = null;
		// Prompt Login
		while (true) {
			List<String> login = PromptFormatter.collectInputs(Arrays.asList("Enter Hospital ID", "Enter Password"));
			hospitalId = login.get(0);
			password = login.get(1);

			// ignore the exception thrown from this call
			if (!PasswordManager.checkPassword(hospitalId, password)) {
				System.out.println("Incorrect Hospital ID or Password.\n");
				continue;
			}

			break;
		}

		try {
			// login verified, retrieve user context
			activeUser = UserManager.createUserContext(hospitalId);
			// sequence sensitive, DO NOT REARRANGE
			activeUserHospitalId = hospitalId;
		} catch (Exception e) {

			System.out.println("\nAn unexpected error occurred: ");
			System.out.println("\n" + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static HospitalManagementSystem getInstance() {
		return hmsInstance;
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static RoleManager getRoleManager() {
		return roleManagerInstance;
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static PasswordManager getPasswordManager() {
		return passwordManagerInstance;
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static UserManager getUserManagerInstance() {
		return userManagerInstance;
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static MedicalRecordManager getMedicalRecordManagerInstance() {
		return medicalRecordManagerInstance;
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static AppointmentManager getAppointmentManagerInstance() {
		return appointmentManagerInstance;
	}

	/**
	 * Unused, for warning suppression purposes.
	 * @return
	 */
	public static MedicationStockManager getMedicationStockManagerInstance() {
		return medicationStockManagerInstance;
	}

}
