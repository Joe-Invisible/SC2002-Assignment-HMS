package hms.utility;

import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import hms.exception.TableMismatchException;
import hms.exception.TableQueryException;
import hms.exception.UndefinedVariableException;
import hms.exception.UserNotFoundException;

/**
 * A rectangular table, where each row must have exactly the same number of columns.
 */
public class TableHandler extends CSVHandler {
	/**
	 * The format object of this table, provides information like the 
	 * variable names available.
	 */
	public final TableFormat format;
	/**
	 * Shorthand for referencing a list of all variable names in this table.
	 * This list is ordered.
	 */
	public final List<String> ALL_COLUMNS;

	/**
	 * Constructs a TableHandler object
	 * @param filePath the path to the file storing the table
	 * @param orderedVariableName the ordered list of variable name, as they are specified 
	 * within the table
	 * @param idColIndex the index of the column to use as the id.
	 * @throws IOException
	 */
	public TableHandler(String filePath, List<String> orderedVariableName, int idColIndex) throws IOException {
		super(filePath, idColIndex);
		format = new TableFormat(orderedVariableName, idColIndex);
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
	 * Checks whether the supplied id exists in the column. The column checked for 
	 * id is as specified in the constructor call.
	 * 
	 * @param id string identifier
	 * @return true if the id is found; false otherwise.
	 */
	public boolean isExistentId(String id) {
		return findId(id) != -1;
	}

	/**
	 * Checks whether the supplied variable name exists in the table.
	 * 
	 * @param variableName the variable name to check
	 * @return the column index of the variable, if found.
	 * @throws UndefinedVariableException if the variable does not exist.
	 */
	public int isExistentVariable(String variableName) throws UndefinedVariableException {
		return this.format.indexOf(variableName);
	}
	
	/**
	 * Uses this.isExistentId to check whether the id exists in this table. 
	 * 
	 * @param id string identifier
	 * @throws UserNotFoundException if the id is nonexistent within the table.
	 */
	public void checkIsExistentId(String id) throws UserNotFoundException {
		if (!this.isExistentId(id)) throw new UserNotFoundException(
				"User Not Found: Nonexistent id " + id
		);
	}
	
	/**
	 * Reads a column from this table.
	 * @param variableName the variable name to read
	 * @return a list of String representing the column read
	 * @throws UndefinedVariableException
	 */
	public List<String> readColumn(String variableName) throws UndefinedVariableException {
		return super.readColumn(this.format.indexOf(variableName));
	}

	/**
	 * Yes, because tables with only two columns are very common.
	 * @param <T>
	 * @param variableName1 name of the first variable
	 * @param variableName2 name of the second variable
	 * @param valueConstructor optional, for concise construction of desired objects
	 * from the values read from the second column. 
	 * @return a HashMap containing the String's read from the first column as its keys,
	 * and the constructed T objects as its values. 
	 * @throws UndefinedVariableException
	 */
	public <T> HashMap<String, T> readTwoColumns(String variableName1, String variableName2,
			Function<String, T> valueConstructor) throws UndefinedVariableException {

		return super.readTwoColumns(this.format.indexOf(variableName1), this.format.indexOf(variableName2),
				valueConstructor);
	}

	/**
	 * Reads a specific variable from a row identified by a unique string.
	 * 
	 * @param id           identifier string
	 * @param variableName name of the variable to be read
	 * @param isStrip whether to strip the surrounding whitespace of each cell,
	 * default is true, see the overload without isStrip.
	 * 
	 * @return a {@link java.lang.String String} containing the value of the desired
	 *         variable. {@code null} if {@code id} or {@code variableName} does not
	 *         exist in the table associated with this handler.
	 * @throws UndefinedVariableException
	 */
	public String readVariable(String id, String variableName, boolean isStrip) throws UndefinedVariableException {
		if (!isExistentId(id))
			return null;
		return super.readVariable(this.findId(id), this.format.indexOf(variableName), isStrip);
	}
	
	/**
	 * Reads a specific variable from a row identified by a unique string. This overload
	 * defaults to stripping all whitespace around each cell.
	 * @param id identifier string
	 * @param variableName name of the variable to be read.
	 * @return
	 * @throws UndefinedVariableException
	 */
	public String readVariable(String id, String variableName) throws UndefinedVariableException {
		return this.readVariable(id, variableName, CSVHandler.STRIP);
	}

	/**
	 * Reads and returns a row identified by a unique string as its first entry.
	 * 
	 * @param id         identifier string
	 * @param idColIndex the column index where the contents is to be matched with
	 *                   id
	 * @return a {@link java.util.List List} containing the strings read from that
	 *         row. {@code null} if {@code id} does not exist in the table
	 *         associated with this handler. The changes made on the returned List
	 *         will not be reflected in the data array internal to this
	 *         TableHandler.
	 */
	public List<String> readRow(String id) {
		if (!isExistentId(id)) return null;
		return super.readRow(this.findId(id));
	}

	/**
	 * Adds a row to the table. This row must match in format and length
	 * as specified during the constructor call.
	 * @param rowData the list of data representing the new row
	 * @throws IOException
	 * @throws TableMismatchException
	 */
	public void addRow(List<String> rowData) throws IOException, TableMismatchException {
		if (rowData.size() != this.format.getVariableCount()) throw new TableMismatchException(
					"New row does not match table format: " + 
					this.getFilePath() + "expects " + 
					this.format.getVariableCount() + " variables, got " 
					+ rowData.size());

		writeNewRow(rowData);
	}

	/**
	 * Removes a row identified by id.
	 * @param id string identifier
	 * @return the removed row, if found; null otherwise
	 * @throws IOException
	 */
	public List<String> removeRow(String id) throws IOException {
		if (!isExistentId(id)) return null;
		return super.removeRow(this.findId(id));
	}
	
	private void checkListMatchesFormat(List<String> list) throws TableMismatchException {
		if (list.size() != this.format.getVariableCount()) {
			throw new TableMismatchException(
					"Input list does not match table format: " + this.getFilePath()
					+ " expects " + this.format.getVariableCount() + 
					" variables, got " + list.size()
			);
		}

	}

	/**
	 * Gets the variable from a list that is supposedly a row read from this table,
	 * or follows the header order and column count imposed by this table.
	 * @param list         input list
	 * @param variableName name of variable to get from the list
	 * @throws TableMismatchException
	 * @throws UndefinedVariableException
	 */
	public String getFromList(List<String> list, String variableName) throws 
		TableMismatchException, 
		UndefinedVariableException 
	{
		checkListMatchesFormat(list);

		return list.get(this.format.indexOf(variableName));
	}
	
	/**
	 * Sets a particular element of a list, which is supposedly conforming to the 
	 * row format of this table, to a supplied value. The position to set is 
	 * specified by the variable name provided. Removing a row, calling this method
	 * on the row, and inserting it back into the table is effectively an updateVariable
	 * operation.
	 * @param list the list to set
	 * @param variableName the variable name that specified the position within the list to set
	 * @param newValue the new value
	 * @throws UndefinedVariableException
	 * @throws TableMismatchException
	 */
	public void setInList(List<String> list, String variableName, String newValue) throws 
		UndefinedVariableException, 
		TableMismatchException 
	{
		checkListMatchesFormat(list);
		
		list.set(this.format.indexOf(variableName), newValue);
	}

	/**
	 * Updates a variable in a row identified by id.
	 * @param id string identifier
	 * @param variableName the name of the variable to update
	 * @param newValue the new value to set this variable to
	 * @throws IOException
	 * @throws UndefinedVariableException
	 * @throws UserNotFoundException
	 */
	public void updateVariable(String id, String variableName, String newValue)
			throws IOException, UndefinedVariableException, UserNotFoundException {
		checkIsExistentId(id);
		super.updateVariable(this.findId(id), this.format.indexOf(variableName), newValue);
	}

	/**
	 * takes a list of strings and concatenates them into one string delimited with
	 * semi-colons.
	 * @param id identifier string
	 * @param variableName variable name
	 * @param newList new value to write into the cell
	 * @throws IOException
	 * @throws UndefinedVariableException
	 * @throws UserNotFoundException
	 */
	public void updateVariable(String id, String variableName, List<String> newList) throws 
		IOException, 
		UndefinedVariableException, 
		UserNotFoundException 
	{
		checkIsExistentId(id);
		String newCell = String.join(";", newList);
		newCell = newCell + ";";
		super.updateVariable(this.findId(id), this.format.indexOf(variableName), newCell);
	}

	/**
	 * Takes in a list of strings and split them by semicolons (;) into a list.
	 * @param stringVariable the String to split
	 * @return the List of String's after the split
	 * @throws UndefinedVariableException
	 */
	public List<String> readListVariable(String stringVariable) throws UndefinedVariableException {
		String[] newString = stringVariable.split(";");
		if (newString.length < 1 || newString[0].equals("") || newString[0].toUpperCase().equals("NA")) {
			return Arrays.asList();
		}
		List<String> newList = Arrays.asList(newString);
		return newList;
	}

	/**
	 * Gets an Iterator to the rows of this table.
	 * @return an Iterator to the rows of this table.
	 */
	public Iterator<List<String>> getRows() {
		return super.getData().iterator();
	}
	
	public class TableQuery {
		/**
		 * Provided for the consumers of this feature to test whether a String matches any
		 * of the operator patterns that corresponds to supported query operations.
		 */
		public static final Pattern COMPARISON_OPERATOR_PATTERN = Pattern.compile("==|!=|<|<=|>|>=");
		
		private final Map<String, Function<Integer, TableQuery>> operationMap = Map.of(
			"==", this::equals,
			"!=", this::doesNotEqual,
			"<", this::lessThan,
			"<=", this::lessThanOrEquals,
			">", this::greaterThan,
			">=", this::greaterThanOrEquals
		);
		
		private List<String> subjects;
		private String operand;
		private Predicate<String> operation;
		private Set<List<String>> results;
		private Function<Set<List<String>>, Boolean> resultMerger;
		
		/**
		 * Constructs a query object associated with this table, providing basic 
		 * table query operations with String and Integer comparisons, and basic 
		 * set operations like intersection and union of the query results.
		 * 
		 * @param subjects the columns of the table to be included in the 
		 * query result. If subjects is null or empty, the result defaults to giving
		 * only the id column, which is as defined when the table was created.
		 */
		public TableQuery(List<String> subjects) {
			this.subjects = new ArrayList<String>();
			this.results = new HashSet<List<String>>();
			this.resultMerger = null;
			
			if (subjects == null || subjects.isEmpty()) {
				// default to retrieving id if subjects is null
				this.subjects.add(
						TableHandler.this.format.getVariableNames()
							.get(TableHandler.this.format.getIdColIndex())
				);
				return;
			}
			for (String subject : subjects) {
				this.subjects.add(subject);
			}
		}
		
		private void checkVariableName(String variableName) throws TableQueryException {
			if (!TableHandler.this.format.getVariableNames().contains(variableName)) throw new TableQueryException(
					"Undefined Variable Name: variableName " 	+ 
					variableName 						+ 
					" does not match any of " 			+ 
					this.subjects
			);
		}
		
		/**
		 * Starts a query operation on a variable name
		 * 
		 * @param variableName variable name to perform query on
		 * @return this TableQuery object for operation chaining.
		 * @throws TableQueryException
		 */
		public TableQuery where(String variableName) throws TableQueryException {
			this.checkVariableName(variableName);
			
			this.operand = variableName;
			return this;
		}
		
		/**
		 * Specifies the target value of the .where(...) clause it follows. Sets the 
		 * predicate ready for scanning through the table.
		 * @param <U> the type of the value. In most cases this will be String
		 * @param value the target value
		 * @return this TableQuery object for operation chaining.
		 */
		public <U> TableQuery matches(U value) {
			if (Integer.class.isInstance(value)) return this.equals((Integer)value);
			this.operation = s -> value.getClass().isInstance(s) ? (value.getClass().cast(s)).equals(value) : false;
			return this;
		}
		
		/**
		 * Specifies the target value of the .where(...) clause it follows. Sets the 
		 * negated predicate ready for scanning through the table.
		 * @param <U> the type of the value. In most cases this will be String
		 * @param value the target value
		 * @return this TableQuery object for operation chaining.
		 */
		public <U> TableQuery doesNotMatch(U value) {
			if (Integer.class.isInstance(value)) return this.doesNotEqual((Integer)value);
			this.operation = s -> value.getClass().isInstance(s) ? !(value.getClass().cast(s)).equals(value) : !false;
			return this;
		}
		
		private static int toIntNoThrow(String number) {
			try {
				return Integer.parseInt(number);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				System.exit(-1); 		// exit with error instead of throwing exception
			}
			
			return -1;
		}
		
		/**
		 * Sets the predicate for the .where(...) clause it follows, to test whether
		 * the variable in the column concerned is greater than a certain value.
		 * 
		 * @param i the value to test against.
		 * @return this TableQuery object for operation chaining.
		 */
		public TableQuery greaterThan(Integer i) {
			this.operation = n -> toIntNoThrow(n) > i;
			return this;
		}
		
		/**
		 * Sets the predicate for the .where(...) clause it follows, to test whether
		 * the variable in the column concerned is greater than or equal to a certain value.
		 * 
		 * @param i the value to test against.
		 * @return this TableQuery object for operation chaining.
		 */
		public TableQuery greaterThanOrEquals(Integer i) {
			this.operation = n -> toIntNoThrow(n) >= i;
			return this;
		}
		
		/**
		 * Sets the predicate for the .where(...) clause it follows, to test whether
		 * the variable in the column concerned is less than a certain value.
		 * 
		 * @param i the value to test against.
		 * @return this TableQuery object for operation chaining.
		 */
		public TableQuery lessThan(Integer i) {
			this.operation = n -> toIntNoThrow(n) < i;
			return this;
		}
		
		/**
		 * Sets the predicate for the .where(...) clause it follows, to test whether
		 * the variable in the column concerned is less than or equal to a certain value.
		 * 
		 * @param i the value to test against.
		 * @return this TableQuery object for operation chaining.
		 */
		public TableQuery lessThanOrEquals(Integer i) {
			this.operation = n -> toIntNoThrow(n) <= i;
			return this;
		}
		
		/**
		 * Sets the predicate for the .where(...) clause it follows, to test whether
		 * the variable in the column concerned equals a certain value.
		 * 
		 * @param i the value to test against.
		 * @return this TableQuery object for operation chaining.
		 */
		public TableQuery equals(Integer i) {
			this.operation = n -> toIntNoThrow(n) == i.intValue();
			return this;
		}
		
		/**
		 * Sets the predicate for the .where(...) clause it follows, to test whether
		 * the variable in the column concerned does not equal a certain value.
		 * 
		 * @param i the value to test against.
		 * @return this TableQuery object for operation chaining.
		 */
		public TableQuery doesNotEqual(Integer i) {
			this.operation = n -> toIntNoThrow(n) != i.intValue();
			return this;
		}
		
		/**
		 * Validates whether a String represents a token that maps to 
		 * a supported query operation by this class.
		 * 
		 * @param token the token to be checked against
		 * @return true if the token maps to 
		 * a supported query operation by this class; false otherwise. 
		 */
		public static boolean validateOperator(String token) {
			return TableQuery.COMPARISON_OPERATOR_PATTERN.matcher(token).matches();
		}
		
		/**
		 * Translates a string token, if recognized, into a query operation.
		 * @param token the 
		 * @return
		 */
		public Function<Integer, TableQuery> getOperation(String token) {
			return operationMap.get(token);
		}
		
		private String getFromListNoThrow(List<String> row, String variableName) {
			try {
				return TableHandler.this.getFromList(row, variableName);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			return null;
		}
		
		/**
		 * Executes and yields the sub-table that matches the whole query chain as a final result.
		 * 
		 * @return the sub-table formed so far
		 * @throws TableMismatchException
		 * @throws UndefinedVariableException
		 */
		public List<List<String>> yield() throws TableMismatchException, UndefinedVariableException {
			this.execute();
			return new ArrayList<List<String>>(this.results);
		}
		
		/**
		 * Constructs a sub-table containing all the required columns whose rows match the criterion
		 * provided in the preceding .where(...) clause. This sub-table is then merged with the current
		 * query result using either intersection or union as defined by the most recent call to .or()
		 * or .and(). If no such merging operation was defined for this step, the merge defaults to
		 * set union.
		 * 
		 * @return this query object for operation chaining.
		 * @throws TableMismatchException
		 * @throws UndefinedVariableException
		 */
		public TableQuery execute() throws TableMismatchException, UndefinedVariableException {
			Set<List<String>> partialResults = new HashSet<List<String>>();
			Iterator<List<String>> tableRows = getRows();
			
			if (tableRows.hasNext()) tableRows.next();
			
			while (tableRows.hasNext()) {
				List<String> tableRow = tableRows.next();
				String tableValue = TableHandler.this.getFromList(tableRow, this.operand);
				
				if (operation.test(tableValue)) {
					partialResults.add(
							this.subjects
								.stream()
								.map(n -> this.getFromListNoThrow(tableRow, n))
								.toList()
					);
				}
			}
			
			// prevents repeated trigger of execute()
			this.operand = null;
			this.operation = null;
			
			if (this.resultMerger != null) {
				this.resultMerger.apply(partialResults);
				this.resultMerger = null;
				return this;
			}
			
			// defaults to union
			this.results.addAll(partialResults);
			return this;
		}
		
		/**
		 * Performs a set intersection of the query result of the previous .where(...) clause
		 * and the other that will follow. Note that as this is just a very simple query API,
		 * support for operation grouping is limited, and the operation precedence is strictly 
		 * left-to-right (starting from the first call on the operation chain).
		 * </br></br>
		 * That is, q.where(...).matches(...).or().where(...).matches(...).and().where(...).matches(...)
		 * will yield the result of evaluating the queries pairwise from the start of the chain;
		 * regardless of whether the operations themselves by any mathematical definitions have
		 * a predefined precedence.
		 * 
		 * @return this query object for operation chaining
		 * @throws TableMismatchException
		 * @throws UndefinedVariableException
		 */
		public TableQuery and() throws TableMismatchException, UndefinedVariableException {
			this.execute();
			this.resultMerger = results::retainAll;
			return this;
		}
		
		
		/**
		 * Performs a set union of the query result of the previous .where(...) clause
		 * and the other that will follow. Note that as this is just a very simple query API,
		 * support for operation grouping is limited, and the operation precedence is strictly 
		 * left-to-right (starting from the first call on the operation chain).
		 * </br></br>
		 * That is, q.where(...).matches(...).or().where(...).matches(...).and().where(...).matches(...)
		 * will yield the result of evaluating the queries pairwise from the start of the chain;
		 * regardless of whether the operations themselves by any mathematical definitions have
		 * a predefined precedence.
		 * 
		 * @return this query object for operation chaining
		 * @throws TableMismatchException
		 * @throws UndefinedVariableException
		 */
		public TableQuery or() throws TableMismatchException, UndefinedVariableException {
			this.execute();
			this.resultMerger = results::addAll;
			return this;
		}
		
		/**
		 * Gets the variable from the resultRow provided, based on the variableName provided
		 * @param resultRow a row from the query results
		 * @param variableName the name of the variable to retrieve
		 * @return the variable read
		 */
		public String getFromResult(List<String> resultRow, String variableName) {
			return resultRow.get(this.subjects.indexOf(variableName));
		}
		
		/**
		 * Gets the result as if it is a single String, i.e., those of the form [[resultString]].
		 * This is provided for convenience and the sake of declarative-ness. If the results
		 * was empty, null is returned.
		 * @return the result as String
		 */
		public String getSingleResult() {
			try {
				return new ArrayList<List<String>>(this.results).getFirst().getFirst();
			} catch (NoSuchElementException e) {
				return null;
			}
		}
		
		/**
		 * Checks whether the query result is empty.
		 * @param result the query result
		 * @return true if and only if the query associated with this result matched zero (0) rows.
		 */
		public static boolean isEmptyResult(List<List<String>> result) {
			return result.isEmpty() || result.getFirst().isEmpty();
		}
	}
}




















