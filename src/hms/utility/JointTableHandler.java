package hms.utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import hms.exception.UndefinedVariableException;

/**
 * Joins multiple WideTable's and provides streamlined operations directed to
 * specific wide table with the supplied variable names. This class specifically
 * only supports wide tables because it is imitating the features of an ordinary
 * table.
 */
public class JointTableHandler {
	private List<WideTableHandler> handlers;
	private Map<String, WideTableHandler> variables;
	private final List<String> orderedVariableNames;

	public JointTableHandler(List<WideTableHandler> handlers) throws Exception {
		this.handlers = handlers;
		this.variables = new HashMap<String, WideTableHandler>();
		this.orderedVariableNames = new ArrayList<String>();

		for (WideTableHandler handler : this.handlers) {
			if (handler.format.getVariableCount() != 2) {
				throw new Exception("Joint Table Creation Failure : " + handler.getFilePath()
						+ " does not provide exactly 2 variables names.");
			}

			this.variables.put(handler.format.getVariableNames().getLast(), handler);
			this.orderedVariableNames.add(handler.format.getVariableNames().getLast());
		}
	}

	/**
	 * Reads and returns the values associated with the id in the specified
	 * WideTable.
	 * 
	 * @param id           identifier string
	 * @param variableName the variable name associated with the WideTable (in
	 *                     principle the header of the WideTable's second column)
	 * 
	 * @return a List containing the values requested. The List is empty if the row
	 *         of the id only contains the id itself (empty row). null if the id
	 *         does not exist within the table.
	 * @throws UndefinedVariableException
	 */
	public List<String> readVariable(String id, String variableName) throws UndefinedVariableException {
		if (this.variables.get(variableName) == null)
			throw new UndefinedVariableException("Unknown variable name: " + variableName);

		List<String> ret = this.variables.get(variableName).readRow(id);
		if (ret.size() > 1)
			ret = ret.subList(1, ret.size());
		else
			ret.clear();

		return ret;
	}

	/**
     * Adds a new value to the specified ID and variable name.
     *
     * @param id           the hms id
     * @param variableName the variable name associated with a WideTable
     * @param newValue     the new value to add
     * @throws IOException if an I/O error occurs
     */
	public void addValue(String id, String variableName, String newValue) throws IOException {
		this.variables.get(variableName).addValue(id, newValue);
	}

	/**
     * Removes a value by index for the specified ID and variable name.
     *
     * @param id           the hms id
     * @param variableName the variable name associated with a WideTable
     * @param valueIndex   the index of the value to remove
     * @return the removed value
     * @throws IOException                 if an I/O error occurs
     * @throws UndefinedVariableException if the variable name is unknown
     */
	public String removeValue(String id, String variableName, int valueIndex)
			throws IOException, UndefinedVariableException {
		return this.variables.get(variableName).removeValue(id, valueIndex);
	}

	/**
     * Overwrites a value in the specified ID and variable name at the given index.
     *
     * @param id           the hms ID
     * @param variableName the variable name associated with a WideTable
     * @param valueIndex   the index of the value to overwrite
     * @param newValue     the new value
     * @throws IOException                 if an I/O error occurs
     * @throws UndefinedVariableException if the variable name is unknown
     */
	public void overwriteValueInList(String id, String variableName, int valueIndex, String newValue)
			throws IOException, UndefinedVariableException {
		this.variables.get(variableName).updateValue(id, valueIndex, newValue);
	}

	/**
	 * Reads a row identified by id from the joint table, that is, reading the
	 * row identified by id from each component table.
	 * 
	 * @param id identifier string
	 * @return A map of string, which is the variable name provided by each table;
	 *         to a list of strings, which are the values associated with the
	 *         supplied id in that table.
	 */
	public Map<String, List<String>> readRow(String id) {
		Map<String, List<String>> rowData = new HashMap<String, List<String>>();

		for (Map.Entry<String, WideTableHandler> entry : this.variables.entrySet()) {
			// only return the non-id columns of the tables
			List<String> values = entry.getValue().readRow(id);
			values = values.subList(1, values.size());
			rowData.put(entry.getKey(), values);
		}

		return rowData;
	}

	/**
	 * @return an immutable copy of the variable names contained in this JointTable.
	 *         The order of which is as specified during the constructor call.
	 */
	public List<String> getVariableNames() {
		// make immutable
		return Arrays.asList(this.orderedVariableNames.toArray(new String[0]));
	}

	/**
     * Adds a new row with the specified ID to all wide tables.
     *
     * @param id the identifier string
     * @throws IOException if an I/O error occurs
     */
	public void addRow(String id) throws IOException {
		for (WideTableHandler table : this.variables.values()) {
			table.addRow(id);
		}
	}

	/**
	 * Checks if an id exists in any component wide table of this joint table.
	 * 
	 * @param id string identifier
	 * @return true if any of the component wide table contains the specified id.
	 *         false otherwise
	 */
	public boolean isExistentIdAny(String id) {
		return this.handlers.stream().anyMatch(h -> h.isExistentId(id));
	}

	/**
	 * Checks if an id exists in any component wide table of this joint table.
	 * 
	 * @param id string identifier
	 * @return true if any of the component wide table contains the specified id.
	 *         false otherwise
	 */
	public boolean isExistentId(String id, String variableName) {
		return this.variables.get(variableName).isExistentId(id);
	}

	/**
	 * Checks if an id exists in all component wide tables of this joint table.
	 * 
	 * @param id string identifier
	 * @return true if any of the component table contains the specified id. false
	 *         otherwise
	 */
	public boolean isExistentIdAll(String id) {
		return this.handlers.stream().allMatch(h -> h.isExistentId(id));
	}

	/**
     * Finds the index of a value that satisfies a predicate in the specified table.
     *
     * @param id           the hms ID
     * @param variableName the variable name
     * @param predicate    the condition to satisfy
     * @return the index of the matching value, or -1 if none match
     */
	public int findIndexOfValue(String id, String variableName, Predicate<String> predicate) {
		return this.variables.get(variableName).findIndexOfValue(id, predicate);
	}

	/**
     * Get a value by index for the specified ID and variable name.
     *
     * @param id           the hms ID
     * @param variableName the variable name
     * @param valueIndex   the index of the value
     * @return the value
     * @throws UndefinedVariableException if the variable name is unknown
     */
	public String getValue(String id, String variableName, int valueIndex) throws UndefinedVariableException {
		return this.variables.get(variableName).getValue(id, valueIndex);
	}
	
	/**
     * Finds the first entry that satisfies a predicate in the specified table.
     *
     * @param id           the hms ID
     * @param variableName the variable name
     * @param predicate    the condition to satisfy
     * @return the matching value, or null if none match
     */
	public String findEntry(String id, String variableName, Predicate<String> predicate) {
		int entryIndex = this.variables.get(variableName).findIndexOfValue(id, predicate);
		return entryIndex == -1 ? null : this.variables.get(variableName).getValue(id, entryIndex);
	}
}
