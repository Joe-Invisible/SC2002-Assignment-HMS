package hms.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import hms.exception.UndefinedVariableException;

public class TableFormat {
	private final Map<String, Integer> variableIndexMapping;
	private final List<String> orderedVariableNames;
	private final int idColIndex;

	public TableFormat(List<String> orderedVariableNames, int idColIndex) {

		this.orderedVariableNames = orderedVariableNames;
		this.idColIndex = idColIndex;

		variableIndexMapping = new HashMap<String, Integer>();

		// map all variable names to their respective index
		IntStream.range(0, orderedVariableNames.size())
			.mapToObj(i -> Map.entry(orderedVariableNames.get(i), i))
			.forEach(e -> variableIndexMapping.put(e.getKey(), e.getValue()));
	}

	public int indexOf(String variableName) throws UndefinedVariableException {
		Integer index = this.variableIndexMapping.get(variableName);
		if (index == null) throw new UndefinedVariableException(
				"Unknown variable name: " + variableName
		);

		return index;
	}

	public int getVariableCount() {
		return this.orderedVariableNames.size();
	}

	public int getIdColIndex() {
		return this.idColIndex;
	}

	
	public List<String> getVariableNames() {
		return new ArrayList<String>(this.orderedVariableNames);
	}

}
