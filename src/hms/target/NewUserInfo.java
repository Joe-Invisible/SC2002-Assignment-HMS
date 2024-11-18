package hms.target;

/**
 * The convention record shared by PasswordManager, UserManager, and RoleManager,
 * to communicate the changes in user databases.
 */
public record NewUserInfo(
		/**
		 * The hospitalId of the new user
		 */
		String hospitalId, 
		/**
		 * The role name of the new user.
		 * This need not be those defined
		 * within ./res/permissions.csv
		 */
		String roleName
) { }
