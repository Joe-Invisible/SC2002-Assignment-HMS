package hms.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hms.exception.PollingFaultException;
import hms.exception.TableMismatchException;
import hms.exception.UndefinedVariableException;

/**
 * This utility class attempts to streamline various prompting processes
 * and provide a consistent look in the console outputs across the entire
 * system. 
 */
public class PromptFormatter {
	/**
	 * The tab width employed by PromptFormatter. This value is usually
	 * 8 in most IDE consoled, as opposed to 4 in most text editors.
	 */
	public static final int TAB_WIDTH = 8;
	private static Scanner inputScanner;
	private static Deque<String> workingDirectory;
	private static PromptFormatter pfInstance = null;

	private PromptFormatter(Scanner in, Deque<String> directoryList) {
		inputScanner = in;
		workingDirectory = directoryList;
	}

	/**
	 * Initializes the PromptFormatter.
	 * 
	 * @param in the input scanner to be used for all prompting operations within this class.
	 * @param directoryList the reference to the list containing the current execution directory 
	 * of the system.
	 */
	public static void PromptFormatterInit(Scanner in, Deque<String> directoryList) {
		if (pfInstance == null) {
			pfInstance = new PromptFormatter(in, directoryList);
		}

	}

	/**
	 * Prints the current working (execution) directory and prompts the user for input. The name 
	 * of the directory is defined by the service-providing manager classes.
	 * @return the input given by the user.
	 */
	public static String workingDirectoryPrompt() {
		System.out.print(String.join("/", workingDirectory.reversed()) + "> ");
		return inputScanner.nextLine();
	}

	/**
	 * Adjusts a list of strings by appending tabs to each line so that all lines
	 * are right-justified based on the longest string in the list. The number of
	 * tabs added is calculated based on a specified tab width.
	 * 
	 * If the input is a List of String containing a colon (:) in all of them,
	 * then the justification will be inserted right before the colons. This
	 * only applies when all Strings in the list has a colon, and only the
	 * first occurrence of which will be used.
	 *
	 * @param lines    the list of strings to be justified
	 * @return a new list of strings where each line has the necessary number of
	 *         tabs appended to reach the maximum length
	 */
	public static List<String> getJustifiedLines(List<String> lines) {
		List<String> paddedLines = new ArrayList<String>();

		List<String> values = new ArrayList<String>(Collections.nCopies(lines.size(), ""));
		// user intends to print key-value pairs
		if (lines.stream().allMatch(s -> s.contains(":"))) {
			values = new ArrayList<String>(lines.stream().map(l -> ": " + l.split(":", 2)[1]).toList());
			lines = lines.stream().map(l -> l.split(":", 2)[0]).toList();
		}

		int maxLength = lines.stream().mapToInt(String::length).max().orElse(0) + TAB_WIDTH; // something weird
																								// happened, so this is
																								// added

		for (String line : lines) {
			int spacesNeeded = maxLength - line.length();
			int tabsNeeded = (int) Math.ceil((double) spacesNeeded / TAB_WIDTH);
			String tabPad = "\t".repeat(tabsNeeded);

			paddedLines.add(line + tabPad + values.removeFirst());
		}

		return paddedLines;
	}

	/**
	 * Centers and prints a given string within a line of 40 characters, padded left
	 * and right with '='. If the string is longer than 40 characters, it trims the
	 * string to fit within 38 characters, adding padding on both sides to keep it
	 * centered. If the string is empty, a line of 40 '=' characters will be printed
	 *
	 * @param text the string to be centered
	 */
	public static void printSeparation(String text) {
		int lineLength = 38;
		int textLength = text.length();

		// If text is too long, trim it to fit within 36 characters
		if (textLength > lineLength - 2) {
			text = text.substring(0, lineLength - 2);
			textLength = text.length();
		}

		// Calculate the padding on each side
		int totalPadding = lineLength - textLength;
		int leftPadding = totalPadding / 2;
		int rightPadding = totalPadding - leftPadding;

		// Construct the centered line
		System.out.println("=".repeat(leftPadding) + (text.isEmpty() ? "=" : " ") + text + (text.isEmpty() ? "=" : " ")
				+ "=".repeat(rightPadding));
	}

	@FunctionalInterface
	private interface RowFormatter {
		String format(List<String> row);
	}

	/**
	 * Prints a table following the header (list of variable names in that table).
	 * @param table the table to print
	 * @param header the list of variable names corresponding to the columns of this table.
	 */
	public static void printTable(List<List<String>> table, List<String> header) {
		List<Integer> columnWidths = IntStream.range(0, header.size())
				.mapToObj(i -> Math.max(header.get(i).length(), table.stream()
						.map(row -> i < row.size() ? row.get(i).length() : 0).max(Integer::compareTo).orElse(0)))
				.collect(Collectors.toList());

		RowFormatter formatRow = row -> IntStream.range(0, columnWidths.size())
				.mapToObj(i -> String.format(" %-" + columnWidths.get(i) + "s ", i < row.size() ? row.get(i) : ""))
				.collect(Collectors.joining("|", "|", "|"));

		String border = columnWidths.stream().map(width -> "-".repeat(width + 2))
				.collect(Collectors.joining("+", "+", "+"));

		System.out.println(formatRow.format(header));
		System.out.println(border);
		if (table.isEmpty())
			System.out.println("<No Matching Records>");
		table.forEach(row -> System.out.println(formatRow.format(row)));

		System.out.println();
	}

	/**
	 * A list of enumerated choices.
	 */
	public static class Poll {
		private List<String> options;
		private List<String> elaborations;
		private String pollName;
		private int answer;
		private static final String RETURN = "__RETURN__";

		/**
		 * Creates a poll based on the list of options supplied.
		 * @param options the choices the user will be offered with.
		 */
		public Poll(List<String> options) {
			this(options, "Select Item");
		}

		/**
		 * Creates a poll based on the list of options supplied.
		 * 
		 * @param options the choices the user will be offered with.
		 * @param pollName the display name of the poll, the default is "Select Item".
		 */
		public Poll(List<String> options, String pollName) {
			// create poll with no elaborations (list of empty strings)
			this(options, options.stream().map(o -> "").toList(), pollName);
		}

		/**
		 * Creates a poll based on the list of options supplied.
		 * 
		 * @param options the choices the user will be offered with.
		 * @param elaboration the elaboration to accompany each option, the default is "" (empty string).
		 * @param pollName the display name of the poll, the default is "Select Item".
		 */
		public Poll(List<String> options, List<String> elaborations, String pollName) {
			this.options = new ArrayList<String>();
			List<List<String>> multiLine = options.stream().map(o -> Arrays.asList(o.split("\n"))).toList();

			if (multiLine.getFirst().size() > 1) {
				multiLine = multiLine.stream().map(ls -> getJustifiedLines(ls)).toList();
				multiLine.forEach(l -> l.set(0, "" + l.getFirst()));

				for (List<String> line : multiLine) {
					this.options.add(String.join("\n", line));
				}
			} else
				this.options = new ArrayList<String>(options);

			this.options.add(RETURN); // append for "__RETURN__" option
										// it was .add(null), but that was illegal
										// ArrayDeque throws an exception for that
			this.pollName = pollName;
			this.elaborations = elaborations;
			this.answer = -1;
		}

		/**
		 * Prompts the user to choose from the options provided. The prompting is done
		 * until the user enters either a valid choice (including Return)
		 * 
		 * @param options options, functionally a list of strings that represent the
		 *                outcome of each choice
		 * @return this poll object, to allow call chaining.
		 */
		public Poll pollUntilValid() {
			int option = -1;
			this.printFormattedPollQuestions();

			while (true) {
				try {
					System.out.print("Please enter an index: ");
					String safeOption = inputScanner.nextLine();
					option = Integer.parseInt(safeOption);

					// option = Integer.parseInt(workingDirectoryPrompt());
				} catch (NumberFormatException e) {
					System.out.println("Please enter a number from the poll above.");
					// "poll until valid" part of this method
					continue;
				}

				if (option > 0 && option <= options.size())
					break;

				System.out.println("Invalid option.");
			}

			this.answer = option - 1;

			return this;
		}

		private void printFormattedPollQuestions() {
			printSeparation(this.pollName);
			IntStream.range(0, this.options.size() - 1).forEach(i -> {
				// printing 1-based index then the associated option
				System.out.print("[" + (i + 1) + "]" + "\t" + this.options.get(i));

				if (!this.elaborations.get(i).isEmpty()) {
					// print elaboration in parentheses if available (not empty)
					System.out.println(" (" + this.elaborations.get(i) + ")");
				} else {
					// prints only the newline
					System.out.println();
				}
			});

			System.out.println("[" + this.options.size() + "]" + "	Return");
			printSeparation("");
		}

		/**
		 * Gets the number of options this poll contains. This includes the 
		 * additional __RETURN__ option inserted by the class itself. For a 
		 * poll with n user-defined options, this method will return n + 1.
		 * @return the number of all options in this poll. 
		 */
		public int size() {
			return this.options.size();
		}

		private void checkAnswerValid() throws PollingFaultException {
			if (this.answer == -1) {
				if (this.options.size() == 0)
					throw new PollingFaultException("Poll Operation Failure: Poll was empty.");
				else
					throw new PollingFaultException("Poll Operation Failure: Poll not yet answered.");
			}
		}

		/**
		 * Gets the answer string to this Poll exactly as written in the poll options.
		 * 
		 * @return the answer to this Poll, as a String; null when the answer was
		 *         __RETURN__
		 * @throws PollingFaultException when the poll has no questions, or when the
		 *                               answer is accessed before being answered.
		 */
		public String getAnswerString() throws PollingFaultException {
			this.checkAnswerValid();
			if (this.options.get(this.answer).equals(RETURN))
				return null;
			return this.options.get(this.answer);
		}

		/**
		 * Gets the answer to this Poll in the form of the index of poll options. The
		 * index of options here is 0-based and as specified during the constructor
		 * call.
		 * 
		 * @return the answer to this Poll, as an Integer; null when the answer was
		 *         __RETURN__
		 * @throws PollingFaultException when the poll has no questions, or when the
		 *                               answer is accessed before being answered.
		 */
		public Integer getAnswerIndex() throws PollingFaultException {
			this.checkAnswerValid();
			if (this.options.get(this.answer).equals(RETURN))
				return null;
			return this.answer;
		}

		/**
		 * Checks whether the user has opted to return instead of answering the poll.
		 * @return true if the user has chosen to return; false otherwise
		 * @throws PollingFaultException
		 */
		public boolean isReturn() throws PollingFaultException {
			return this.getAnswerIndex() == null;
		}
	}

	/**
	 * A series of polls grouped together. The user can navigate back and forth
	 * polls sequentially.
	 */
	public static class NestedPolls extends ArrayList<Poll> {
		private static final long serialVersionUID = 1L;

		public NestedPolls(List<Poll> polls) {
			super(polls);
		}

		/**
		 * Prompts the user to make a sequence of choices from the list of polls
		 * supplied. This method exits either when the user eventually answered all the
		 * polls, or when the user chooses Return on the first of the polls.
		 * 
		 * @param polls a list of polls, where each of them contains an number of
		 *              options
		 * @return this NestedPoll object, to allow call chaining.
		 * @throws PollingFaultException if and only if any of the Poll supplied is
		 *                               empty.
		 */
		public NestedPolls pollNestedUntilValid() throws PollingFaultException {
			int contextNumber = 0;

			do {
				Poll currentPoll = this.get(contextNumber);
				currentPoll.pollUntilValid();
				if (!currentPoll.isReturn()) {
					contextNumber++;
					continue;
				}

				contextNumber--;

			} while (contextNumber >= 0 && contextNumber < this.size());

			return this;
		}

		/**
		 * Gets the answers as written in the options to all polls in this group, where
		 * the order is as specified during constructor call.
		 * 
		 * @return the list of string as answers; null if any of the answers was null,
		 *         that is, the user aborted this poll session.
		 * @throws PollingFaultException if and only if any of the polls supplied was
		 *                               empty.
		 */
		public List<String> getAnswerStrings() throws PollingFaultException {
			ArrayList<String> answerStrings = new ArrayList<String>();
			for (Poll poll : this) {
				if (poll.isReturn())
					return null;
				answerStrings.add(poll.getAnswerString());
			}
			return answerStrings;
		}

		/**
		 * Get the answer to all polls in this NestedPoll, in the form of the index of
		 * poll options, where the order is as specified during constructor call.
		 * 
		 * @return the list of indices as answers; null if any of the answers was null,
		 *         that is, the user aborted this poll session.
		 * @throws PollingFaultException if and only if the polls supplied was empty.
		 */
		public List<Integer> getAnswerIndices() throws PollingFaultException {
			ArrayList<Integer> answerIndices = new ArrayList<Integer>();
			for (Poll poll : this) {
				if (poll.isReturn())
					return null;
				answerIndices.add(poll.getAnswerIndex());
			}
			return answerIndices;
		}
	}

	/**
	 * This utility class abstracts the traditional imperative prompts for receiving
	 * user input, and provides a more declarative approach to writing repetitive
	 * input validations and boundary checks.
	 * 
	 * @param <T> the type of the final output of an InputSession object. This is
	 *            the type desired by the caller.
	 */
	public static class InputSession<T> {
		private String prompt;
		private String input;
		private T conversionResult;
		private boolean hideEscape;
		private Predicate<T> validator = a -> true; // default, no validation
		private String onInvalidInput = "Invalid input. Please try again."; // default
		private String sessionTerminationToken = "-q"; // default
		@SuppressWarnings("unchecked")
		/**
		 * The function for converting String input to desired type and format. When
		 * unset, the default converter is a hard downcasting identity of String, and
		 * the invocation thereof is only safe when T strictly equals String.
		 */
		private Function<String, T> converter = s -> (T) s; // default identity

		/**
		 * Creates an InputSession object that streamlines the input collection process
		 * of a single value. Creating the object does not start prompting the user. Use
		 * the getInput method to start the session after all configurations are done.
		 * 
		 * @param prompt the prompt string
		 */
		public InputSession(String prompt) {
			this(prompt, false);
		}
		
		/**
		 * Creates an InputSession object that streamlines the input collection process
		 * of a single value. Creating the object does not start prompting the user. Use
		 * the getInput method to start the session after all configurations are done.
		 * The option to escape is hidden on demand.
		 * 
		 * @param prompt the prompt string
		 * @param hideEscape when set to true, the escape option e.g., (enter x to quit)
		 * will be hidden. Otherwise it is not hidden.
		 */
		public InputSession(String prompt, boolean hideEscape) {
			this.prompt = prompt;
			this.conversionResult = null;
			this.hideEscape = hideEscape;
		}

		/**
		 * Repeatedly prompts the user in a loop until the validator returns true when
		 * called on the input. The user will not be prompted again if the input is
		 * already valid after the first prompt.
		 * 
		 * @param validator a T-to-boolean mapping (function) that decides whether the
		 *                  input is valid.
		 * @return a handle to this InputSession object, for call chainning purposes.
		 */
		public InputSession<T> setValidator(Predicate<T> validator) {
			this.validator = validator;
			return this;
		}

		private T promptUntilValid() {
			while (true) {
				System.out.print(
						this.prompt + 
						(this.hideEscape ? "" : " (enter " + this.sessionTerminationToken + " to quit)") + 
						": "
				);
				this.input = inputScanner.nextLine();
				
				if (this.input.equals(this.sessionTerminationToken)) {
					this.input = null;	// just in case user still have the object handler
					return null;
				}
				
				try {
					this.conversionResult = this.converter.apply(this.input);
					
					if (this.validator.test(this.conversionResult)) {
						return this.conversionResult;
					}
				} catch (Exception e) { 
					// e.printStackTrace();	// debug purpose
				}
				
				System.out.println(this.onInvalidInput);
			}
		}

		/**
		 * Sets a conversion function to convert user input from String to the desired
		 * type and format.
		 * 
		 * @param converter String-to-T mapping (function)
		 * @return a handle to this InputSession object, for call chainning purposes.
		 */
		public InputSession<T> setConverter(Function<String, T> converter) {
			this.converter = converter;
			return this;
		}

		/**
		 * Sets the prompt output to print when the validator returns false.
		 * 
		 * @param onInvalidInput prompt output
		 * @return a handle to this InputSession object, for call chainning purposes.
		 */
		public InputSession<T> setOnInvalidInput(String onInvalidInput) {
			this.onInvalidInput = onInvalidInput;
			return this;
		}

		/**
		 * Sets the termination token, that is, a string that serves as the stopping
		 * condition of this input session, which allows the user to quit the prompt
		 * when needed. The input is only checked against this token on the second
		 * prompting attempt onwards.
		 * 
		 * @param sessionTerminationToken
		 * @return
		 */
		public InputSession<T> setSessionTerminationToken(String sessionTerminationToken) {
			this.sessionTerminationToken = sessionTerminationToken;
			return this;
		}

		/**
		 * Starts the prompt session, this is a terminal operation, as in it  breaks the chain
		 * and returns the result expected by the caller.
		 * @return user input converted to T as specified in the template initialization. null
		 * if the user quits.
		 */
		public T startPrompt() {
			return this.promptUntilValid();
		}

		/**
		 * Gets the input String entered by the user.
		 * @return the String literally as returned by Scanner.nextLine(), null if the user quits.
		 */
		public String getStringInput() {
			this.startPrompt();
			return this.input;
		}
	}

	/**
	 * Displays each prompt string in a justified format, collects user input for
	 * each prompt, and returns the inputs as a list of strings. This method does
	 * not provide convenience of input validation.
	 *
	 * @param prompts  the list of prompt strings to be displayed
	 * @param tabWidth the width of each tab in spaces, used for justification
	 * @return a list of user inputs corresponding to each prompt
	 */
	public static List<String> collectInputs(List<String> prompts) {
		List<String> justifiedLines = getJustifiedLines(prompts);
		List<String> userInputs = new ArrayList<>();

		// Display each prompt, collect user input, and store it in the list
		for (String justifiedLine : justifiedLines) {
			String input = new InputSession<String>(justifiedLine, true).startPrompt();

			userInputs.add(input);
		}

		return userInputs;
	}
	
	/**
	 * Prompts the user to choose a date. The format of the resultant date
	 * is specified by the Date class. The starting month and year depends 
	 * on the system time.
	 * 
	 * @return a Date object representing the user's choice of date. This is
	 * null if the user quits without choosing a date.
	 * 
	 * @throws Exception
	 */
	public static Date collectDateInput() throws Exception {
		Calendar calendar = Calendar.getInstance();
		return collectDateInput(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
	}
	
	/**
	 * Prompts the user to choose a date. The format of the resultant date
	 * is specified by the Date class. The starting month and year is a supplied.
	 * 
	 * @param currentMonth the starting month to display in the prompt
	 * @param currentYear the starting year to display in the prompt
	 * 
	 * @return a Date object representing the user's choice of date. This is
	 * null if the user quits without choosing a date.
	 * 
	 * @throws Exception
	 */
	public static Date collectDateInput(int currentMonth, int currentYear) throws Exception {
		Date.CalendarView view = new Date.CalendarView(currentMonth, currentYear);

		enum ViewMode {
			DATE, MONTH
		}

		ViewMode currentView = ViewMode.DATE;

		while (true) {
			if (currentView == ViewMode.DATE) {
				view.displayCalendarDates();
			} else {
				view.displayCalendarMonths();
			}

			String prompt = currentView == ViewMode.DATE
					? "Enter date to view time slots.\n[<|>] scroll left/right by month [M] - Switch to Month view"
					: "Enter month number to switch (1 = Jan, 12 = Dec).\n[<|>] scroll left/right by year [D] - Switch to Date view";

			final ViewMode currentViewfinalReference = currentView;	// enclosing scope workaround
			PromptFormatter.InputSession<String> session = new PromptFormatter.InputSession<String>(prompt)
			.setSessionTerminationToken("x").setValidator(s -> {
				if (s.strip().equals("<") || s.strip().equals(">")
						|| (currentViewfinalReference == ViewMode.DATE && s.strip().equalsIgnoreCase("M"))
						|| (currentViewfinalReference == ViewMode.MONTH && s.strip().equalsIgnoreCase("D"))) {
					return true;
				}

				try {
					int input = Integer.parseInt(s);
					if (currentViewfinalReference == ViewMode.DATE && input >= 1 && input <= view.getMaximumNumberOfDays()) {
						return true;
					}
					if (currentViewfinalReference == ViewMode.MONTH && input >= 1 && input <= 12) {
						return true;
					}
					System.out.println(
							currentViewfinalReference == ViewMode.DATE
									? "Input must be a number from 1 to " + view.getMaximumNumberOfDays()
											+ " inclusive!"
									: "Input must be a number from 1 to 12 inclusive!");
				} catch (NumberFormatException e) {
					System.out.println("Only supports operations '<'/'>' or valid input!");
				}
				return false;
			});

			String choice = session.startPrompt();
			if (choice == null)
				return null;

			switch (choice.strip()) {
			case "<" -> {
				if (currentView == ViewMode.DATE) {
					view.adjustMonthBy(-1);
					continue;
				} 
				view.adjustYearBy(-1);
			}
			case ">" -> {
				if (currentView == ViewMode.DATE) {
					view.adjustMonthBy(1);
					continue;
				}
				view.adjustYearBy(1);
			}
			case "M", "m" -> {
				currentView = ViewMode.MONTH;
			}
			case "D", "d" -> {
				currentView = ViewMode.DATE;
			}
			default -> {
				int selectedValue = Integer.parseInt(choice);
				if (currentView == ViewMode.DATE) {
					return view.getDate(selectedValue);
				}
				view.setMonth(selectedValue);
				currentView = ViewMode.DATE; // Automatically return to Date View after month selection
			}
			}
		}
	}


	/**
	 * Could not find a good portable way to clear the console... We just spam
	 * newlines to flush the previous outputs down.
	 */
	public static void clearScreen() {
		for (int i = 0; i < 512; i++) {
			System.out.println();
		}
		System.out.flush();
	}

	/**
	 * Formats A sub-table from a table, by creating a map of variable names
	 * and values and merge those of each row into one string.
	 * 
	 * Each variable in the same row is separated by newline "\n", and the
	 * result for one row (one element in the return value) looks like:
	 * </br>
	 * variable1:value1</br>
	 * variable2:value2</br>
	 * variable3:value3</br>
	 * ... etc.
	 * 
	 * @param requiredColumns
	 * @param resultSubtable
	 * @param fromTable
	 * @return
	 */
	public static List<String> formatFullColumnSubtable(
			List<String> requiredColumns, 
			List<List<String>> resultSubtable,
			TableHandler fromTable
	) {
		List<String> formattedResults = new ArrayList<String>();
		for (List<String> resultRow : resultSubtable) {
			
			List<String> segmentedResult = requiredColumns.stream().map(c -> {
				try {
					return c + ":" + fromTable.getFromList(resultRow, c);
				} catch (TableMismatchException | UndefinedVariableException e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
			
			formattedResults.add(String.join("\n", segmentedResult));
		}
		return formattedResults;
	}

}
