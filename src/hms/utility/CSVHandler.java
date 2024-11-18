package hms.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class that handles R/W operations from a CSV file.
 * This class does not impose any preconditions on the dimensions
 * of the table represented by the CSV. The derivatives of this class
 * could be non-rectangular, i.e., having variable number of columns
 * each row.
 */
public class CSVHandler implements AutoCloseable {
	/**
	 * Use this to specify in table read operations, where applicable,
	 * to strip the surrounding whitespace characters of the data
	 */
	public static final boolean STRIP = true;
	/**
	 * Use this to specify in table read operations, where applicable,
	 * to not strip the surrounding whitespace characters of the data
	 */
	public static final boolean NO_STRIP = !STRIP;
	private String filePath;
	private Data data;

	/**
	 * A list of {@code String[]} that is used to store the CSV data. Each list
	 * entry represents a row, and each element in the entry represents a cell.
	 */
	private static class Data extends ArrayList<String[]> {
		private static final long serialVersionUID = 1L;

		/**
		 * Returns the specified row of the table with each cell's content stripped off
		 * of surrounding whitespace.
		 */
		@Override
		public String[] get(int index) {
			return Arrays.asList(super.get(index)).stream().map(s -> s.strip()).collect(Collectors.toList())
					.toArray(new String[0]);
		}

		public String[] getNoStrip(int index) {
			return super.get(index);
		}
	}

	/**
	 * Loads a file and stores its content in memory.
	 * 
	 * @param filePath path of the file to be read
	 * @throws IOException see link for reasons this exception maybe thrown.
	 */
	protected CSVHandler(String filePath, int idColIndex) throws IOException {
		this.filePath = filePath;
		this.data = new Data();

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] row = line.split(",");
				data.add(row);
			}
		}
	}

	
	protected List<String> readColumn(int colIndex) {
		List<String> column = new ArrayList<String>();

		IntStream.range(1, data.size()).forEach(i -> {
			// Avoiding data.subList as it is even more complex to override to strip
			// whitespace
			String[] row = data.get(i);
			if (row.length > colIndex) column.add(row[colIndex]);
		});

		return column;
	}
	
	/**
	 * Reads two columns and returns a {@link java.util.HashMap HashMap} with the
	 * first column as keys and the second as values. The first row is ignored.
	 * 
	 * @param <T>              the return type of the supplied
	 *                         {@link java.util.function.Function Function}.
	 * @param col1             first column to be read
	 * @param col2             second column to be read and processed per element by
	 *                         {@code valueConstructor}
	 * @param valueConstructor any function that takes a {@link java.lang.String
	 *                         String} and returns {@code T}. The default is the
	 *                         String identity ({@code s -> s : Function<String, String>}).
	 *                         See the overload without valueConstructor for more details.
	 * @return a {@link java.util.HashMap HashMap} with keys consisting of strings
	 *         from {@code col1} and values as results of applying
	 *         {@code valueConstructor} on each value read from {@code col2}.
	 */
	protected <T> HashMap<String, T> readTwoColumns(int col1, int col2, Function<String, T> valueConstructor) {
		HashMap<String, T> map = new HashMap<>();

		IntStream.range(1, data.size()).forEach(i -> {
			// Avoiding data.subList as it is even more complex to override to strip
			// whitespace
			String[] row = data.get(i);
			if (row.length > col1 && row.length > col2) {
				map.put(row[col1], valueConstructor.apply(row[col2]));
			}
		});

		return map;
	}
	
	protected HashMap<String, String> readTwoColumns(int col1, int col2) {
		return this.readTwoColumns(col1, col2, s -> s);
	}

	/**
	 * Reads a row identified by a by index.
	 * 
	 * @param idColIndex the column index where the contents is to be matched with
	 *                   id
	 * @return a {@link java.util.List List} containing the strings read from that
	 *         row associated with this handler. The changes made on the returned List
	 *         will not be reflected in the data array internal to this CSVHandler.
	 */
	protected List<String> readRow(int rowIndex) {
		List<String> rowData = new ArrayList<String>(Arrays.asList(data.get(rowIndex).clone()));

		if (rowData.size() == 0)
			return null;

		return rowData;
	}

	/**
	 * Finds the specified id in the table.
	 * 
	 * @param id         specified id
	 * @param idColIndex the column index where the contents is to be matched with
	 *                   id
	 * @return the row index of the id if found. -1 otherwise.
	 */
	protected int findId(String id, int idColIndex) {

		for (int i = 1; i < data.size(); i++) {
			if (data.get(i)[idColIndex].equals(id))
				return i;
		}

		return -1;
	}

	/**
	 * Reads a specific variable from a row identified by column index (0-based).
	 * 
	 * @param id           identifier string
	 * @param variableName name of the variable to be read
	 * @return a {@link java.lang.String String} containing the value of the desired
	 *         variable.
	 */
	protected String readVariable(int rowIndex, int colIndex) {
		return this.readVariable(rowIndex, colIndex, STRIP);
	}
	
	protected String readVariable(int rowIndex, int colIndex, boolean isStrip) {
		if (isStrip) {
			return data.get(rowIndex)[colIndex];
		}
		return data.getNoStrip(rowIndex)[colIndex];
	}

	/**
	 * Writes a new row to the CSV file, appending the contents of a supplied
	 * {@link java.util.List List}. This method does not check whether the input
	 * matches the table format, because it simply cannot.
	 * 
	 * @param rowData a {@link java.util.List List} of {@link java.lang.String
	 *                String}'s to be appended as the new row
	 * @throws IOException see link for reasons this exception maybe thrown.
	 */
	protected void writeNewRow(List<String> rowData) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
			bw.write(String.join(",", rowData));
			bw.newLine();
		}
		// Also update the in-memory data
		data.add(rowData.toArray(new String[0]));
	}

	/**
	 * Removes a row indexed by rowIndex.
	 * 
	 * @param rowIndex the 0-based index of the row to remove.
	 * @return the remove row as a List of Strings; null otherwise.
	 * @throws IOException
	 */
	protected List<String> removeRow(int rowIndex) throws IOException {
		List<String> ret = Arrays.asList(data.remove(rowIndex));
		writeAllData();

		return ret;
	}

	/**
	 * Overwrites a variable in a row with the unique string as its first entry.
	 * 
	 * @param id           identifier string
	 * @param variableName name of the variable to be read
	 * @param newValue     new value for the specified variable
	 * @throws IOException see link for reasons this exception maybe thrown.
	 */
	protected void updateVariable(int rowIndex, int colIndex, String newValue) throws IOException {
		String[] newRow = data.get(rowIndex);
		newRow[colIndex] = newValue;
		data.set(rowIndex, newRow);
		// Write back the updated data to the file
		writeAllData();
	}

	/**
	 * Appends a new value to the row identified by the unique string
	 * 
	 * @param id       identifier string
	 * @param newValue value to append
	 * @throws IOException
	 */
	protected void addValue(int rowIndex, String newValue) throws IOException {
		List<String> newRow = new ArrayList<String>(Arrays.asList(data.get(rowIndex)));
		newRow.add(newValue);

		data.set(rowIndex, newRow.toArray(new String[0]));

		writeAllData();
	}

	/**
	 * Remove the value at the specified index from the row identified by the unique
	 * string
	 * 
	 * @param id         identifier string
	 * @param valueIndex index of the value to be removed
	 * @return the removed value, if it exists, that is, the specified valueIndex
	 *         does not overrun the value list of the specified row; null otherwise.
	 * @throws IOException
	 */
	protected String removeValue(int rowIndex, int valueIndex) throws IOException {
		List<String> newRow = new ArrayList<String>(Arrays.asList(data.get(rowIndex)));
		String ret = newRow.remove(valueIndex);

		data.set(rowIndex, newRow.toArray(new String[0]));
		writeAllData();

		return ret;
	}

	protected void updateValue(int rowIndex, int valueIndex, String newValue) throws IOException {
		this.updateVariable(rowIndex, valueIndex, newValue);
	}

	/**
	 * Gets a separate, immutable copy of the data array representing this table
	 * @return the table data
	 */
	public List<List<String>> getData() {
		return this.data.stream().map(r -> Arrays.asList(r)).toList();
	}

	/**
	 * Writes {@code this.data} back to the CSV file associated with this handler.
	 * 
	 * @throws IOException see link for reasons this exception maybe thrown.
	 */
	private void writeAllData() throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
			for (String[] row : data) {
				bw.write(String.join(",", row));
				bw.newLine();
			}
		}
	}

	@Override
	public void close() throws Exception {

	}

	/**
	 * Gets the path to the file associated with this CSVHandler object.
	 * @return the file path, as specified in the constructor call.
	 */
	public String getFilePath() {
		return this.filePath;
	}

}
