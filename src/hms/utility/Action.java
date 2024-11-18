package hms.utility;

/**
 * Represents an action of a specific name
 * This class is essentially a wrapper for a String and provides equality
 * checking and a string representation for the action.
 */
// Basically just a String wrapper
public class Action {
	private final String name;
	
	/**
     * Constructs an Action with the specified name.
     *
     * @param name the name of the action
     */
	public Action(String name) {
		this.name = name;
	}
	
	/**
     * Retrieves the name of this action.
     *
     * @return the name of the action
     */
	public String getName() {
		return name;
	}
	
	/**
     * Returns a string representation of the action.
     *
     * @return a string in the format "Action: {name}"
     */
	public String toString() {
		return "Action: " + this.getName();
	}
	
	/**
     * Compares this action to the specified object to check if they are equal.
     * Two actions are considered equal if their names are the same.
     *
     * @param o the object to compare to
     * @return true if the specified object is an Action with the same name,
     *         false otherwise
     */
	@Override
	public boolean equals(Object o) {
		return o instanceof Action && ((Action)o).name.equals(this.name);
	}
}