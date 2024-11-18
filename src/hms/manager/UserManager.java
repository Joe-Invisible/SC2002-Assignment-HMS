package hms.manager;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import hms.HospitalManagementSystem;
import hms.exception.CommandStackViolationException;
import hms.exception.PollingFaultException;
import hms.exception.RoleNotFoundException;
import hms.exception.TableMismatchException;
import hms.exception.TableQueryException;
import hms.exception.UndefinedVariableException;
import hms.exception.UserNotFoundException;
import hms.target.NewUserInfo;
import hms.user.Administrator;
import hms.user.Doctor;
import hms.user.Patient;
import hms.user.Pharmacist;
import hms.user.User;
import hms.utility.PromptFormatter;
import hms.utility.TableHandler;
import hms.utility.TableHandler.TableQuery;

/**
 * This class provides methods for handling user profile-related transactions,
 * as well as providing the Hospital Management System with the active user's
 * context, which is a {@link hms.user.User User} object created with the
 * appropriate credentials according to the database record.
 */
public class UserManager extends HospitalResourceManager {
	private static UserManager umInstance = null;
	private static TableHandler userTableHandler;

	// Table Variable
	private static final String ID = "ID";
	private static final String NAME = "Name";
	private static final String ROLE = "Role";
	private static final String BIRTH_DATE = "BirthDate";
	private static final String GENDER = "Gender";
	private static final String AGE = "Age";
	private static final String BLOOD_TYPE = "BloodType";
	private static final String EMAIL = "Email";
	private static final String PHONE = "Phone";

	private UserManager() throws Exception {
		userTableHandler = new TableHandler("./res/users.csv",
				Arrays.asList(ID, NAME, ROLE, BIRTH_DATE, GENDER, AGE, BLOOD_TYPE, EMAIL, PHONE), 0);

		// probably not going to read the whole thing in this case. The file may be very
		// large

		umInstance = this;
	}

	/**
	 * Initializes the UserManager
	 * @return a handle to the singleton instance of UserManager
	 * @throws Exception
	 */
	public static UserManager UserManagerInit() throws Exception {
		if (umInstance == null) {
			umInstance = new UserManager();
			return umInstance;
		}

		return null;
	}

	/**
     * Creates a user context based on the provided hospital ID.
     * 
     * @param hospitalId The ID of the hospital for which to create a user context.
     * @return A {@link User} object representing the user's context.
     * @throws UserNotFoundException If the user cannot be found.
     * @throws RoleNotFoundException If the role associated with the user is invalid.
     * @throws UndefinedVariableException If an expected variable is missing.
     */
	public static User createUserContext(String hospitalId) throws 
		UserNotFoundException, 
		RoleNotFoundException, 
		UndefinedVariableException 
	{
		User user = null;
		String roleName = userTableHandler.readVariable(hospitalId, ROLE);

		switch (roleName) {
		
		case "Doctor" -> user = new Doctor();

		case "Patient" -> user = new Patient();

		case "Pharmacist" -> user = new Pharmacist();

		case "Administrator" -> user = new Administrator();

		default -> {
			throw new RoleNotFoundException(
					"User context creation failed: hospitalId " + 
					hospitalId + 
					" has invalid role " + 
					roleName + 
					"."
			);
		}
		}

		return user;
	}

	/**
     * Command class for handling user profile-related actions.
     */
	public static class Command extends HospitalResourceManager.Command {
		public Command(String action) {
			super(action);
			super.managerDescription = "Profiles";
		}

		public void invoke(String hospitalId) throws Exception {
			super.setIssuerId(hospitalId);
			if (!RoleManager.checkIdHasPermission(hospitalId, super.getCommand()))
				super.rejectCommand();

			switch (super.getCommand()) {

			case "ADD_USER" -> {
				promptAddUser(hospitalId);
			}

			case "READ_PERSONAL_PROFILE" -> {
				provideUserProfile(hospitalId);
			}

			case "WRITE_PERSONAL_PROFILE" -> {
				promptUpdateProfile(hospitalId);
			}

			case "WRITE_STAFF_LIST" -> {
				promptUpdateStaffList(hospitalId);
			}

			case "READ_STAFF_LIST" -> {
				promptViewStaffList();
			}

			default -> super.reportUndefinedCommand();
			}
		}
	}

	/**
     * Checks if a user exists by the provided hospital ID.
     * 
     * @param hospitalId The hospital ID to check.
     * @return True if the user exists, false otherwise.
     */
	public static boolean isExistentUser(String hospitalId) {
		return userTableHandler.isExistentId(hospitalId);
	}
	
	/**
     * Retrieves the name of the user associated with the given hospital ID.
     * 
     * @param hospitalId The hospital ID of the user.
     * @return The user's name.
     * @throws UndefinedVariableException If the variable is not found.
     * @throws TableMismatchException If there is a mismatch in the table.
     * @throws UndefinedVariableException If an expected variable is missing.
     */
	public static String getName(String hospitalId) throws 
		UndefinedVariableException, 
		TableMismatchException, 
		UndefinedVariableException 
	{
		return userTableHandler.readVariable(hospitalId, NAME);
	}

	/**
     * Provides the user's profile by displaying their details.
     * 
     * @param hospitalId The ID of the hospital to retrieve the user's profile.
     * @throws UserNotFoundException If the user is not found.
     * @throws UndefinedVariableException If the expected variable is missing.
     * @throws TableMismatchException If there is an issue with the table format.
     */
	private static void provideUserProfile(String hospitalId) throws 
		UserNotFoundException, 
		UndefinedVariableException, 
		TableMismatchException 
	{
		List<String> profile = userTableHandler.readRow(hospitalId);

		if (profile.size() != userTableHandler.format.getVariableCount()) {
			throw new UserNotFoundException(
					"Profile entry unavailable: query on ID " + 
					hospitalId + 
					"is either corrupted or missing."
			);
		}

		List<String> variableNames = userTableHandler.format.getVariableNames();
		List<String> justifiedLines = PromptFormatter.getJustifiedLines(variableNames);

		PromptFormatter.printSeparation("User Profile");
		for (String variableName : variableNames) {
			System.out.println(
				justifiedLines.removeFirst() + ": " + userTableHandler.getFromList(profile, variableName)
			);
		}
		PromptFormatter.printSeparation("");
	}

	/**
     * Prompts the user to update their profile, such as changing their email or phone number.
     * 
     * @param hospitalId The ID of the hospital to update the user's profile.
     * @throws UndefinedVariableException If the expected variable is missing.
     * @throws IOException If there is an input/output error.
     * @throws UserNotFoundException If the user cannot be found.
     * @throws PollingFaultException If there is an issue with polling input.
     */
	private static void promptUpdateProfile(String hospitalId) throws 
		UndefinedVariableException, 
		IOException, 
		UserNotFoundException, 
		PollingFaultException 
	{
		String choice = new PromptFormatter.Poll(Arrays.asList(
				// actual domain of response that will be recorded
				"Email", "Phone"),
				Arrays.asList(
						// elaboration for option Email, etc.
						"currently " + userTableHandler.readVariable(hospitalId, EMAIL),
						"currently " + userTableHandler.readVariable(hospitalId, PHONE)),
				"Select Field").pollUntilValid().getAnswerString();

		if (choice == null)
			return;

		System.out.print("Please input new " + choice + ": ");
		String newVariable = Command.inputScanner.nextLine();
		userTableHandler.updateVariable(hospitalId, choice, newVariable);
		System.out.println(choice + " change successful! New Value: " + newVariable);
	}

	/**
	 * Administrator operation, adds user to the data base
	 * 
	 * @return the hospitalId of the newly added user
	 * @throws TableMismatchException
	 * @throws IOException
	 * @throws UndefinedVariableException
	 * @throws CommandStackViolationException
	 */
	private static void promptAddUser(String hospitalId) throws 
		IOException, 
		TableMismatchException, 
		UndefinedVariableException,
		CommandStackViolationException 
	{
		PromptFormatter.printSeparation("Create Profile");
		
		// Creates a prompt out of all variables needed
		List<String> rowInput = PromptFormatter.collectInputs(userTableHandler.format.getVariableNames());

		NewUserInfo newUserInfo = new NewUserInfo(
				userTableHandler.getFromList(rowInput, ID), 
				userTableHandler.getFromList(rowInput, ROLE)
		);
		
		HospitalManagementSystem.setTarget(hospitalId, newUserInfo);
		// follow up actions, note that these commands are invoked under the name of the
		// currently active user
		try {
			HospitalManagementSystem.dispatchCommand(new PasswordManager.Command("ADD_USER"));
			HospitalManagementSystem.dispatchCommand(new RoleManager.Command("ADD_USER"));
		} catch (Exception e) {
			return; // no specific cleanup in this case. Might require in the future.
		}
		PromptFormatter.printSeparation("");

		// RoleManager validates the role supplied by user.
		userTableHandler.setInList(rowInput, ROLE, RoleManager.getRoleName(newUserInfo.hospitalId()));
		
		userTableHandler.addRow(rowInput);

		System.out.println("Successfully added user!");
	}

	/**
	 * Administrator operation, remove user to the data base
	 * 
	 * @throws UserNotFoundException
	 * @throws TableMismatchException
	 * @throws IOException
	 * @throws UndefinedVariableException
	 * @throws CommandStackViolationException
	 */
	private static void provideRemoveUser(String adminId, String removalId) throws 
		UserNotFoundException, 
		TableMismatchException,
		IOException, 
		CommandStackViolationException,
		UndefinedVariableException 
	{
		provideUserProfile(removalId);
		System.out.println("The user above will be removed. Are you sure? (y/*)");
		String decision = Command.inputScanner.nextLine();

		if (!decision.equals("y"))
			return;

		// If the prompt does not loop until input is valid, do not bother to
		// specifically start an InputSession
		System.out.println("Please enter your password: ");
		String password = Command.inputScanner.nextLine();
		if (!PasswordManager.checkPassword(adminId, password)) {
			System.out.println("Password incorrect, better luck next time.");
			return;
		}

		HospitalManagementSystem.setTarget(adminId, removalId);
		// follow up actions, note that these commands are invoked under the name of the
		// currently active user; the handle (hospitalId) of the new user can be
		// retrieved using a getter
		try {
			// userRoles.csv cannot be cleared before this command, as this condition is 
			// dependent on removalId being existent therein
			if (RoleManager.isAppointableUser(removalId)) {
				HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("REMOVE_USER"));
			}
			HospitalManagementSystem.dispatchCommand(new PasswordManager.Command("REMOVE_USER"));
			HospitalManagementSystem.dispatchCommand(new RoleManager.Command("REMOVE_USER"));
		} catch (Exception e) {
			e.printStackTrace();
			return; // no specific cleanup in this case. Might require in the future.
		}

		userTableHandler.removeRow(removalId);

		System.out.println("Successfully Promoted " + removalId + " to customer status!");
	}

	/**
	 * Prompts the user to view the list of staff members in the hospital.
	 * This method retrieves the staff list from the system and displays it in a formatted manner.
	 * 
	 * @throws UndefinedVariableException If the required variables for retrieving the staff list are missing or incorrect.
	 * @throws TableQueryException If there is an issue querying the staff data from the user table.
	 * @throws TableMismatchException If the required variables does not match with the table in the CSV.
	 * @throws PollingFaultException If there is an issue polling the staff data from the user table.
	 */
	private static void promptViewStaffList() throws 
		PollingFaultException, 
		TableMismatchException, 
		UndefinedVariableException, 
		TableQueryException 
	{
		String filterCriterion = new PromptFormatter.Poll(
			Stream.concat(
				userTableHandler.format.getVariableNames().stream(), Stream.of("All Staff")
			).toList(), 
			"Select Filter Type"
		).pollUntilValid().getAnswerString();
		
		if (filterCriterion == null) return;

		TableQuery staffQuery = userTableHandler.new TableQuery(userTableHandler.ALL_COLUMNS)
			.where(ROLE).doesNotMatch("Patient");
		List<String> tableHeader = userTableHandler.format.getVariableNames();
		List<List<String>> staffTable = null;
		
		if (filterCriterion.equals("All Staff")) {
			staffTable = staffQuery.yield();
		}
		
		// Or any other numerical columns
		else if (filterCriterion.equals(AGE)) {
			Map.Entry<String, Integer> operation = new PromptFormatter.InputSession<Map.Entry<String, Integer>>(
					"Input operation for " + filterCriterion + ": '!='|'=='|'>'|'<'|'>='|'<=' <value>"
			)
			.setConverter(s -> {
				List<String> tokens = Arrays.asList(s.split(" "));
				return new AbstractMap.SimpleEntry<String, Integer>(
						tokens.get(0), Integer.parseInt(tokens.get(1))
				);
			})
			// converter also serves as a validation mean, the input is valid
			// if and only if the converter exits normally
			.setValidator(s -> TableQuery.validateOperator(s.getKey()))
			.startPrompt();
			
			if (operation == null) return;
			
			staffTable = staffQuery.and()
				.where(filterCriterion)
				.getOperation(operation.getKey())
				.apply(operation.getValue())
				.yield();
		}
		
		// Non-comparable columns
		else {
			String condition = new PromptFormatter.InputSession<String>(
					"Enter " + filterCriterion + " to match"
			).startPrompt();
			
			if (condition == null) return;
			
			staffTable = staffQuery.and().where(filterCriterion).matches(condition).yield();
		}
		
		PromptFormatter.printTable(staffTable, tableHeader);
	}

	private static String localReadVariableNoThrow(String hospitalId, String verifiedVariableName) {
		try {
			return userTableHandler.readVariable(hospitalId, verifiedVariableName);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
	
	/**
	 * Prompt the admin to update staff list given operations such as adding, deleting and 
	 * updating staff list information
	 * 
	 */
	private static void promptUpdateStaffList(String adminId) throws Exception {
		String operation = new PromptFormatter.Poll(
			Arrays.asList("Add Staff", "Update Field", "Delete Staff"), "Select Operation"
		)
		.pollUntilValid()
		.getAnswerString();
		
		if (operation == null) return;
		
		if (operation.equals("Add Staff")) {
			promptAddUser(adminId);
			return;
		}
		

		String staffId = new PromptFormatter.InputSession<String>("Please input the staff's Hospital ID")
		.setValidator(s -> isExistentUser(s) && !localReadVariableNoThrow(s, ROLE).equals("Patient"))
		.setOnInvalidInput("Not an existent Staff!")
		.startPrompt();
		
		if (staffId == null) return;
		
		if (operation.equals("Delete Staff")) {
			provideRemoveUser(adminId, staffId);
			return;
		}
		
		
		final List<String> mutableFields = Arrays.asList(NAME, ROLE, BIRTH_DATE, GENDER, AGE, BLOOD_TYPE, EMAIL, PHONE);
		String field = new PromptFormatter.Poll(
				mutableFields,
				mutableFields.stream().map(f -> "Currently " + localReadVariableNoThrow(staffId, f)).toList(),
				"Select Field"
		).pollUntilValid().getAnswerString();
		
		if (field == null) return;
		
		
		// Functional variable
		if (field.equals(ROLE)) {
			HospitalManagementSystem.setTarget(adminId, staffId);
			HospitalManagementSystem.dispatchCommand(new RoleManager.Command("WRITE_ROLE"));
			userTableHandler.updateVariable(staffId, field, RoleManager.getRoleName(staffId));
			return;
		}
		
		String newValue = null;
		// Numerical variables
		if (field.equals(AGE)) {
			Integer newIntValue = new PromptFormatter.InputSession<Integer>("Please enter new " + field)
			.setConverter(Integer::parseInt)
			.setOnInvalidInput("Input must be a number!")
			.startPrompt();
			
			if (newIntValue == null) return;
			
			newValue = newIntValue.toString();
		}
		
		else {
			newValue = new PromptFormatter.InputSession<String>("Please enter new " + field)
			.startPrompt();
			
			if (newValue == null) return;
		}
		
		userTableHandler.updateVariable(staffId, field, newValue);
		
		System.out.println("Successfully updated " + field);
	}
}




















