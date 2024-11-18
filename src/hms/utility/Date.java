package hms.utility;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Date implements Comparable<Date> {
	private int year;
	private int month;
	private int day;
	private int hours;
	private int minutes;
	private String stringDate;
	
	/** List of all month names. */
	public static final List<String> ALL_MONTHS = Arrays.asList( 
			"January", "February", "March", "April", "May", "June", 
			"July", "August", "September", "October", "November", "December" 
	);
	
	/** List of available time slots in the format HH:MM. */
	public static final List<String> ALL_TIMESLOTS = Arrays.asList( 
			"08:00", "09:00", "10:00", "11:00", "12:00", 
			"13:00", "14:00", "15:00", "16:00", "17:00" 
	);
	

	private static final int DAY = 0;	// expected index format day
	private static final int MONTH = 1;	// expected index format month
	private static final int YEAR = 2;	// expected index format year
	private static final int TIME = 3;
	private static final int HOURS = 0;
	private static final int MINUTES = 1;
	
	/**
     * Constructs a Date object from a list of strings representing the date and time.
     * 
     * @param date A list of strings containing the day, month, year, and time in the format "HH:MM".
     */
	public Date(List<String> date) {

		this.year = Integer.parseInt(date.get(YEAR));
		this.month = ALL_MONTHS.indexOf(date.get(MONTH));
		this.day = Integer.parseInt(date.get(DAY));
		List<Integer> time = Arrays.asList(date.get(TIME).split(":"))
				.stream()
				.map(s -> Integer.parseInt(s))
				.toList();
		this.hours = time.get(HOURS);
		this.minutes = time.get(MINUTES);
		
		this.stringDate = String.join(" ", date);
	}

	/**
     * Returns the year as a string.
     * 
     * @return The year as a string.
     */
	public String getYear() {
		return String.valueOf(this.year);
	}
	
	/**
     * Returns the month as a string.
     * 
     * @return The month as a string.
     */
	public String getMonth() {
		return ALL_MONTHS.get(this.month);
	}
	
	/**
     * Returns the day as a string.
     * 
     * @return The day as a string.
     */
	public String getDay() {
		return String.valueOf(this.day);
	}
	
	/**
     * Returns the hours as a string.
     * 
     * @return The hours as a string.
     */
	public String getHours() {
		return String.valueOf(this.hours);
	}
	
	/**
     * Returns the minutes as a string.
     * 
     * @return The minutes as a string.
     */
	public String getMinutes() {
		return String.valueOf(this.minutes);
	}
	
	/**
	 * Constructs a Date object from a fixed-format date string D[D] MonthName Y[YYY] HH:MM
	 * @param dateString
	 * @return Date obj with the parsed date
	 */
	public static Date fromStringDMYHM(String dateString) {
		
		List<String> dateSegment = Arrays.asList(dateString.split(" ")).stream().map(s -> s.strip()).toList();
		
		return new Date(dateSegment);
	}
	
	@Override
	/**
	 * An Object instance o is said to be equal to this Date instance if and only if
	 * - o is an instance of Date; AND
	 * - o.day == this.day && o.month == this.month && o.year == this.year.
	 * In other words, that they point to the same calendar date.
	 */
	public boolean equals(Object _other) {
		if (!(_other instanceof Date)) return false;
		
		Date other = (Date)_other;
		return (
			this.minutes == other.minutes && 
			this.hours == other.hours && 
			this.day == other.day && 
			this.month == other.month && 
			this.year == other.year
		);
	}

	/**
     * Returns the string representation of the date.
     * 
     * @return The string representation of the date in the format "day month year hours:minutes".
     */
	public String getStringDate() {
		return stringDate;
	}

	 /**
     * Compares this {@code Date} object to another {@code Date} object.
     * 
     * @param o The {@code Date} object to compare with.
     * @return A negative integer, zero, or a positive integer as this {@code Date}
     *         is less than, equal to, or greater than the specified {@code Date}.
     */
	@Override
	public int compareTo(Date o) {
		if (this.year != o.year) return this.year - o.year;
		
		if (this.month != o.month) return this.month - o.month;
		
		if (this.day != o.day) return this.day - o.day;
		
		if (this.hours != o.hours) return this.hours - o.hours;
		
		return this.minutes - o.minutes;
	}
	
	/**
     * The CalendarView class is responsible for managing the calendar's month, year, and dates.
     * in a grid view
     */
	public static class CalendarView {
		private static final Calendar defaultCalendar = Calendar.getInstance();
		private Calendar calendar;
		
		/**
         * Creates a CalendarView for the current month and year based on windows time.
         */
		public CalendarView() {
			this(defaultCalendar.get(Calendar.MONTH), defaultCalendar.get(Calendar.YEAR));
		}
		
		/**
         * Creates a code CalendarView for the specified month and year.
         * 
         * @param month The month (0-based, e.g., 0 for January).
         * @param year The year.
         */
		public CalendarView(int month, int year) {
			this.calendar = Calendar.getInstance();
			this.calendar.set(Calendar.MONTH, month);
			this.calendar.set(Calendar.YEAR, year);
		}
		
		 /**
         * increase or decrease the month by the specified amount, rolling over to the next or previous year if needed.
         * 
         * @param amount The number of months to adjust by.
         */
		public void adjustMonthBy(int amount) {
			// check == 12
			final int monthRollOverValue = calendar.getActualMaximum(Calendar.MONTH) + 1;
			
			int rawSum = this.calendar.get(Calendar.MONTH) + amount;
			if (
				rawSum >= calendar.getActualMaximum(Calendar.MONTH) || 
				rawSum <= calendar.getActualMinimum(Calendar.MONTH)
			) {
				// the values become -1-based when falling below 0 ...
				if (rawSum < 0) rawSum -= calendar.getActualMaximum(Calendar.MONTH);
				this.calendar.set(
					Calendar.YEAR, 
					this.calendar.get(Calendar.YEAR) + rawSum / monthRollOverValue
				);
			}
			this.calendar.set(Calendar.MONTH, (rawSum < 0 ? (-rawSum - 1) : rawSum) % monthRollOverValue);
		}
		
		/**
         * Sets the month of the calendar.
         * 
         * @param month The month to set (1-based, e.g., 1 for January).
         */
		public void setMonth(int month) {
			this.calendar.set(Calendar.MONTH, month - 1);
		}
		
		 /**
         * Adjusts the year by the specified amount.
         * 
         * @param amount The number of years to adjust by.
         */
		public void adjustYearBy(int amount) {
			this.calendar.set(Calendar.YEAR, this.calendar.get(Calendar.YEAR) + amount);
		}
		
		public int getMaximumNumberOfDays() {
			return this.calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		}
		
		/**
         * Gets the maximum number of days in the current month.
         * 
         * @return The maximum number of days in the current month.
         */
		public int getMaximumNumberOfMonths() {
			return this.calendar.getActualMaximum(Calendar.MONTH);
		}
		
		 /**
         * Returns a Date arraylist for a specified day in the current month.
         * 
         * @param day The day of the month.
         * @return A Date arraylist representing the specified day.
         */
		public Date getDate(int day) {
			return new Date(Arrays.asList(
					String.valueOf(day),
					ALL_MONTHS.get(this.calendar.get(Calendar.MONTH)),	// I have no fucking idea about this 
					String.valueOf(this.calendar.get(Calendar.YEAR)),	// format we are using.
					"00:00"
			));
		}
		
		/**
         * Displays a simple view of the calendar for the current year and months.
         */
		public void displayCalendarMonths() {
			PromptFormatter.printSeparation("Calendar");
			
			System.out.println(this.calendar.get(Calendar.YEAR));
			
			System.out.println("Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec");
			
			System.out.println("<                  >");
			PromptFormatter.printSeparation("");
		}
		
		/**
         * Displays a detailed view of the calendar for the current month and year, including days of the week.
         */
		public void displayCalendarDates() {
			PromptFormatter.printSeparation("Calendar");
		
			int firstDayOfWeek = this.calendar.get(Calendar.DAY_OF_WEEK);
			int daysInMonth = this.calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // Get total number of days in a month
			// System.out.println(daysInMonth);
			int year = this.calendar.get(Calendar.YEAR);
		
			String monthName = new DateFormatSymbols().getMonths()[this.calendar.get(Calendar.MONTH)];
		
			System.out.println(monthName + " " + year);
			System.out.println("Su Mo Tu We Th Fr Sa");
		
			// Print the spaces before first day of the month
			for (int i = Calendar.SUNDAY; i < firstDayOfWeek; i++) {
				System.out.print("   ");
			}
		
			for (int day = 1; day <= daysInMonth; day++) {
				System.out.printf("%2d ", day);
		
				// After Saturday, print new line
				if ((day + firstDayOfWeek - 1) % 7 == 0) {
					System.out.println();
				}
			}
			System.out.println();
			System.out.println("<                  >");
			PromptFormatter.printSeparation("");
		}
		
		
	}
}