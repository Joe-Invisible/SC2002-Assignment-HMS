package hms.utility;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import hms.exception.UndefinedVariableException;

/**
 * THIS SHALL PREEMPT ANY OF THE DESCRIPTIONS Due to the multi-value-per-row
 * nature of wide tables, a consistent indexing mode of the values on the same
 * row is required. Here, VARIABLES NAMED valueIndex are 0-based, i.e., they
 * START FROM 0, BUT A 0 INDEX REFERS TO THE FIRST VALUE, INSTEAD OF THE ID THAT
 * PRECEDES IT. TO RETRIEVE THE FIRST VALUE, PASS IN 0 AS INDEX; DO NOT PASS IN
 * 1 FOR THAT.
 * <br><br>
 * A wide table is a irregular table with one header column (usually the id/tag)
 * followed by a variable number of value columns. Below is an example of a wide
 * table:
 * 
 * <table border="1">
 * <caption>Example of a Wide Table</caption>
 * <tr>
 * <th>Student Name</th>
 * <th>Subject 1</th>
 * <th>Subject 2</th>
 * <th>Subject 3</th>
 * <th>Subject 4</th>
 * </tr>
 * <tr>
 * <td>Alice</td>
 * <td>Math: 85</td>
 * <td>Science: 92</td>
 * <td>History: 78</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Bob</td>
 * <td>Math: 90</td>
 * <td>Science: 88</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Carol</td>
 * <td>Math: 95</td>
 * <td>Science: 91</td>
 * <td>History: 89</td>
 * <td>Art: 82</td>
 * </tr>
 * <tr>
 * <td>Dave</td>
 * <td>Math: 75</td>
 * <td></td>
 * <td></td>
 * <td></td>
 * </tr>
 * </table>
 * 
 * This format is useful for, specifically in this case, the diagnoses of each
 * patient, since each of them may have different numbers of written diagnoses.
 * Each row in the wide table is an "Entry", and in that entry we have an id
 * followed by an arbitrary (could be 0) counts of "values"
 */
public class WideTableHandler extends CSVHandler {
	public final TableFormat format;
	public final List<String> ALL_COLUMNS;
	
	public WideTableHandler(String filePath, List<String> orderedVariableName) throws IOException {
		super(filePath, 0);
		format = new TableFormat(orderedVariableName, 0);
		this.ALL_COLUMNS = this.format.getVariableNames();
	}
	
	/**
	 * Finds the specified id in the table. This invokes the base class function
	 * findId() which searches through a column specified as argument for the id.
	 * 
	 * @param id specified id
	 * @return the row index of the id if found. -1 otherwise.
	 */
	public int findId(String id) {
		return super.findId(id, this.format.getIdColIndex());
	}
	
	/**
	 * Finds if specified id exists in the table. This invokes the base class function
	 * findId() which searches through a column specified as argument for the id.
	 * 
	 * @param id specified id
	 * @return true if ID exists in the table, false if the ID does not exist
	 */
	public boolean isExistentId(String id) {
		return findId(id) != -1;
	}

	/**
	 * Appends a new value to the row identified by the unique string
	 * 
	 * @param id specified id, newValue is the value required to add
	 */
	public void addValue(String id, String newValue) throws IOException {
		super.addValue(this.findId(id), newValue);
	}

	/**
	 * remove the value at the valueIndex in row identified by the unique string
	 * 
	 * @param id specified id, valueIndex is the index of the value set for removal
	 * @exception IOExeption if there is an error with file operations
	 * @exception UndefinedVariableException if the user ID or variable is undefined
	 */
	public String removeValue(String id, int valueIndex) throws IOException, UndefinedVariableException {
		try {
			return super.removeValue(this.findId(id), valueIndex + 1);
		} catch (IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Invalid valueIndex (direct cause to " + e.getMessage() + ")");
		}
	}

	/**
	 * Adds an empty row (with only specified id) to the wide table, if the
	 * specified id does not already exist, otherwise do nothing. Since WideTable
	 * could be sparse, the column number check as implemented in TableHandler is
	 * not applicable here, so a row with only an id and without values is possible.
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void addRow(String id) throws IOException {
		if (!this.isExistentId(id))
			this.writeNewRow(Arrays.asList(id));
	}
	
	/**
	 * Reads 
	 * @param id
	 * @return
	 */
	public List<String> readRow(String id) {
		return super.readRow(this.findId(id));
	}

	/**
	 * update the value at the valueIndex in row identified by the unique string with the String newValue
	 * 
	 * @param id specified id, valueIndex is the index set to be updated, newValue is the value required to add
	 * @exception IOExeption if there is an error with file operations
	 * @exception UndefinedVariableException if the user ID or variable is undefined
	 */
	public void updateValue(String id, int valueIndex, String newValue) throws 
		IOException, 
		UndefinedVariableException 
	{
		try {
			super.updateValue(this.findId(id), valueIndex + 1, newValue);
		} catch (IndexOutOfBoundsException e) {
			throw new UndefinedVariableException(
					"Invalid valueIndex (direct cause to " + e.getMessage() + ")"
			);
		}
	}

	/**
	 * Finds a value in the row (a.k.a. entry) associated with the supplied id
	 * 
	 * @param id        string identifier
	 * @param predicate testing condition
	 * @return a 0-based index representing the position of the found item; The
	 *         return value is exactly the argument position passed to others
	 *         functions in this API that requires a {@code valueIndex}
	 */
	public int findIndexOfValue(String id, Predicate<String> predicate) {
		int i = 0;
		List<String> row = this.readRow(id);
		row = row.subList(1, row.size());
		for (String entry : row) {
			if (predicate.test(entry)) return i;
			i++;
		}

		return -1;
	}

	/**
	 * Gets the value pointed to by the 0-based value-Index in the row (entry)
	 * associated with the supplied id
	 * 
	 * @param id         identifier string
	 * @param valueIndex 0-based index of the desired value
	 * @return the value, null if id does not exist in this table.
	 */
	public String getValue(String id, int valueIndex) {
		List<String> entry = this.readRow(id);
		return entry == null ? null : entry.get(valueIndex + 1);
	}

}
