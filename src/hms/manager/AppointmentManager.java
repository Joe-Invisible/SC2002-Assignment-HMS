package hms.manager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import hms.HospitalManagementSystem;
import hms.exception.CommandStackViolationException;
import hms.exception.TableMismatchException;
import hms.exception.TableQueryException;
import hms.exception.UndefinedVariableException;
import hms.target.MedicalRecordModifier;
import hms.target.MedicationStockModifier;
import hms.utility.Date;
import hms.utility.PromptFormatter;
import hms.utility.PromptFormatter.InputSession;
import hms.utility.TableHandler;
import hms.utility.TableHandler.TableQuery;

/**
 * Manages appointment-related functionality in the hospital management system.
 * <p>
 * The AppointmentManager handles scheduling, updating, and viewing patient appointment and doctor
 * schedule. It also manages the interactions with the appointment table and the
 * doctor schedule table, including querying available time slots and rescheduling appointments.
 * </p>
 * 
 * This class uses singleton design to ensure a single instance throughout the application.
 */
public class AppointmentManager extends HospitalResourceManager {
   private static AppointmentManager aInstance = null;
   private static TableHandler appointmentTableHandler;
   private static TableHandler doctorAppointmentTableHandler;

	// Table Variable
   private static final String APPOINTMENTID = "AppointmentId";
   private static final String SCHEDULEID = "ScheduleId";
   private static final String DOCTORID = "DoctorId";
   private static final String PATIENTID = "PatientId";
   private static final String YEAR = "Year";
   private static final String MONTH = "Month";
   private static final String DAY = "Day";
   private static final String TIMESLOT = "Time Slot";
   private static final String STATUS = "Status";
   private static final String APPOINTMENT = "Appointment";
   private static final String DIAGNOSES = "Diagnoses";
   private static final String TREATMENTS = "Treatments";
   private static final String TYPE_OF_SERVICE = "Type Of Service";
   private static final String MEDICATIONS = "Medications";
   private static final String PRESCRIPTION_STATUS = "Prescription Status";
   private static final String PRESCRIBED_QUANTITY = "Prescription Quantity";

   private AppointmentManager() throws Exception {
      appointmentTableHandler = new TableHandler(
         	"./res/patientAppointments.csv", 
         	Arrays.asList(
         		APPOINTMENTID, PATIENTID, DAY, MONTH, YEAR, TIMESLOT, STATUS, DOCTORID, DIAGNOSES,
         		TREATMENTS, TYPE_OF_SERVICE, MEDICATIONS, PRESCRIPTION_STATUS, 
         		PRESCRIBED_QUANTITY
         	),
         	0
         );
   
      doctorAppointmentTableHandler = new TableHandler(
         	"./res/doctorSchedule.csv",
         	Arrays.asList(
         			SCHEDULEID, DOCTORID, DAY, MONTH, YEAR, 
         			TIMESLOT, STATUS, APPOINTMENT, PATIENTID, APPOINTMENTID
         	), 
         	0
         );
   }

   /**
    * Initializes the AppointmentManager singleton instance.
    * <p>
    * This method ensures that only a single instance of AppointmentManager is created
    * during the application's runtime.
    * </p>
    *
    * @return the singleton instance of AppointmentManager
    * @throws Exception if there is an issue initializing the appointment tables
    */
   public static AppointmentManager AppointmentManagerInit() throws Exception {
   	// allows only a single instance per process
      if (aInstance == null) {
         aInstance = new AppointmentManager();
         return aInstance;
      }
      return null;
   }

   /**
    * Command class used to handle various appointment-related commands.
    * <p>
    * This class handles actions such as scheduling, rescheduling, canceling, and
    * viewing available appointments, upcoming appointment schedule, past appointment records, 
    * based on the user's permissions.
    * 
    * </p>
    */
   public static class Command extends HospitalResourceManager.Command {
   	// private static List <Integer> timeInfo;
   
      public Command(String action) {
         super(action);
         super.managerDescription = "Appointments";
      }
   
      public void invoke(String hospitalId) throws Exception {
         super.setIssuerId(hospitalId);
         if (!RoleManager.checkIdHasPermission(hospitalId, super.getCommand()))
            super.rejectCommand();
      	// The following part of the control flow is only entered when user has enough
      	// permission
      
         switch (super.getCommand()) {
            case "REMOVE_USER" -> {
               provideRemoveUser();
            }
         
            case "READ_AVAILABLE_APPOINTMENT" -> { // -vaa for patient
               displayAvailableTimeSlots();
            }
         
            case "PATIENT_SCHEDULE_APPOINTMENT" -> { // -sa for patient
               patientScheduleAppointment(hospitalId);
            }
         
            case "CANCEL_PATIENT_APPOINTMENT" -> { // -ca for patient
               cancelAppointment(hospitalId);
            }
         
            case "RESCHEDULE_PATIENT_APPOINTMENT" -> { // -ra for patient
               patientRescheduleAppointment(hospitalId);
            }
         
            case "VIEW_PATIENT_APPOINTMENT" -> { // -vsa for patient
               patientViewAppointments(hospitalId);
            }
         
            case "READ_PERSONAL_APPOINTMENT_OUTCOME" -> {	// -vpa for patient
               readPersonalOutcomeRecord(hospitalId);
            }
         
            case "WRITE_ANY_MEDICAL_RECORD" -> { // -umr for doctor
               promptUpdateDMT(hospitalId);
            }
         
            case "READ_PERSONAL_APPOINTMENT" -> { // -vps for doctor
               readPersonalAppointment(hospitalId);
            }
         
            case "WRITE_PERSONAL_APPOINTMENT" -> { // -sa for doctor
               doctorScheduleAppointment(hospitalId);
            }
         
            case "WRITE_APPOINTMENT_REQUESTS" -> { // -ada for doctor
               writeAppointmentRequests(hospitalId);
            }
         
            case "READ_UPCOMING_APPOINTMENTS" -> { // -vua for doctor
               readUpcomingAppointments(hospitalId);
            }
         
            case "WRITE_APPOINTMENT_OUTCOME" -> { // -rao for doctor
               writeAppointmentOutcome(hospitalId);
            }
         
            case "READ_APPOINTMENT_OUTCOME" -> { // -vao for pharmacist
               viewAppointmentOutcomeRecord();
            }
         
            case "WRITE_PRESCRIPTION_STATUS" -> { // -ups for pharmacist
               updatePrescriptionStatus(hospitalId);
            }
         
            default -> super.reportUndefinedCommand();
         }
      }
   }

   /**
    * Removes a user from both the doctor appointment and patient appointment tables.
    * <p>
    * This method will remove the user's appointments and update the status of
    * any ongoing appointments associated with the user.
    * </p>
    *
    * @throws CommandStackViolationException if a command violates the expected sequence
    * @throws TableMismatchException if there is a mismatch in the data table
    * @throws UndefinedVariableException if a required variable is undefined
    * @throws TableQueryException if there is an error querying the table
    */
   private static void provideRemoveUser() throws CommandStackViolationException, TableMismatchException, UndefinedVariableException, TableQueryException {
      String removalId = HospitalManagementSystem.getParentTarget();
   	
      doctorAppointmentTableHandler.new TableQuery(null).where(DOCTORID)
         .matches(removalId)
         .yield()
         .forEach(
         i -> {
            try {
               doctorAppointmentTableHandler.removeRow(i.get(0));
            } catch (Exception e) {
               e.printStackTrace();
               System.exit(-1);
            }
         });
   	
      appointmentTableHandler.new TableQuery(null).where(DOCTORID)
         .matches(removalId)
         .and()
         .where(STATUS)
         .doesNotMatch("Completed")
         .yield()
         .forEach(
         i -> {
            try {
               String selectedAppointmentId = i.get(0);
               appointmentTableHandler.updateVariable(selectedAppointmentId, STATUS, "Cancelled");
               appointmentTableHandler.updateVariable(
                  selectedAppointmentId, 
                  APPOINTMENTID, 
                  selectedAppointmentId + "C" + Instant.now().getEpochSecond()
                  );
            } catch (Exception e) {
               e.printStackTrace();
               System.exit(-1);
            }
         });
   }
	
   	/**
	 * Sorts a list of query results based on the date and time in ascending order.
	 * <p>
	 * This method extracts the date and time information from each query result row, then compares
	 * and sorts the rows by their corresponding date and time values. The date and time columns
	 * are identified by the `DAY` and `TIMESLOT` fields, and sorting is done using the `Date` class.
	 * </p>
	 * 
	 * @param queryResult a list of lists where each inner list represents a row of data 
	 *                    from the query result, and each row contains columns that include
	 *                    the date and time information.
	 * @return the sorted list of query results in ascending order by date and time.
	 * @throws UndefinedVariableException if the `DAY` or `TIMESLOT` fields are not defined 
	 *                                     in the table format.
	 */
   private static List<List<String>> sortFullColumnQueryResultByTime(TableHandler handler, List<List<String>> queryResult) {
      try {
         int dateStart = handler.format.indexOf(DAY);
         int dateEnd = handler.format.indexOf(TIMESLOT) + 1;
      	// figuring out a good way to sort these;
         queryResult.sort(
            (a1, a2) -> {
               return new Date(a1.subList(dateStart, dateEnd))
                  .compareTo(new Date(a2.subList(dateStart, dateEnd)));
            });
      } catch (UndefinedVariableException e) {
         e.printStackTrace();
         System.exit(-1);
      }
   	
      return queryResult;
   }
	
   /**
	 * Displays available time slots for appointments based on a date input by the user.
	 * <p>
	 * The method prompts the user to input a date and checks for available appointment slots on that date.
	 * It filters through the doctorSchedule to find available slots, then prints out the details of these
	 * available slots, including the doctor's name, date, and time slot. If no slots are available, the method 
	 * informs the user. The process repeats until the user chooses to return home (exit the loop).
	 * </p>
	 *
	 * @throws Exception if an error occurs during the date input process or while fetching appointment data.
	 */
   private static void displayAvailableTimeSlots() throws Exception {
   
      Date timeInfo = PromptFormatter.collectDateInput();
      if (timeInfo == null) 
         return;
   	
      String year = timeInfo.getYear();
      String month = timeInfo.getMonth();
      String day = timeInfo.getDay();
   
      List<List<String>> availableSlots = doctorAppointmentTableHandler.new TableQuery(doctorAppointmentTableHandler.ALL_COLUMNS)
         .where(STATUS).matches("Available")
         .and()
         .where(DAY).matches(day)
         .and()
         .where(MONTH).matches(month)
         .and()
         .where(YEAR).matches(year)
         .yield();
   
   	// Print available timeslots
      if (TableQuery.isEmptyResult(availableSlots)) {
         System.out.println("> No available time slots for " + day + " " + month + " " + year + ".");
         return;
      }
   	// int count = 1;
      List<String> availableTimeSlots = new ArrayList<>();
      for (List<String> row : availableSlots) {
         String appointmentInfo = "Doctor Name\t: " + UserManager.getName(doctorAppointmentTableHandler.getFromList(row, DOCTORID))
            	+ "\n\tDate\t\t: " + doctorAppointmentTableHandler.getFromList(row, DAY) + " "
            	+ doctorAppointmentTableHandler.getFromList(row, MONTH) + " "
            	+ doctorAppointmentTableHandler.getFromList(row, YEAR) + "\n\tTime Slot\t: "
            	+ doctorAppointmentTableHandler.getFromList(row, TIMESLOT);
         availableTimeSlots.add(appointmentInfo);
      }
   	
      PromptFormatter.printSeparation("Available Time Slots");
      for (String value : availableTimeSlots) {
         System.out.println("-\t" + value);
      }
      PromptFormatter.printSeparation("");
   }

   /**
	 * Prompts the user to select an available time slot from a list of available or cancelled appointments.
	 * <p>
	 * The method collects a date from the user and fetches appointments for the specified day, month, and year
	 * where the status is either "Cancelled" or "Available". It then formats the available time slots into a user-friendly
	 * format and prompts the user to select one. If no available time slots are found for the given date, the method
	 * notifies the user and returns `null`. The method returns the selected time slot information from the available slots.
	 * </p>
	 *
	 * @return A list containing the details of the selected time slot, or `null` if no time slots are available or
	 *         the user cancels the selection.
	 * @throws Exception If an error occurs during the date input process, querying the appointment table, or formatting the output.
	 */
   private static List<String> promptSelectAvailableTimeSlots() throws Exception {
   
      Date timeInfo = PromptFormatter.collectDateInput();
      if (timeInfo == null) 
         return null;
   	
      String year = timeInfo.getYear();
      String month = timeInfo.getMonth();
      String day = timeInfo.getDay();
   	
      List<List<String>> availableSlots = doctorAppointmentTableHandler.new TableQuery(
         	doctorAppointmentTableHandler.ALL_COLUMNS
         )
         .where(STATUS).matches("Cancelled")
         .or()
         .where(STATUS).matches("Available")
         .and()
         .where(DAY).matches(day)
         .and()
         .where(MONTH).matches(month)
         .and()
         .where(YEAR).matches(year)
         .yield();
   	
      if (availableSlots.isEmpty()) {
         System.out.println("No available slots for " + day + " " + month + " " + year + ".");
         return null;
      }
   	
      List<String> formattedSlots = new ArrayList<String>();
   //      List<String> requiredColumns = Arrays.asList(DOCTORID, DAY, MONTH, YEAR, TIMESLOT);
   //      int doctorIdIndex = requiredColumns.indexOf(DOCTORID);
      
      for (List<String> row : sortFullColumnQueryResultByTime(doctorAppointmentTableHandler, availableSlots)) {
         String format = "Doctor Name:\t" + UserManager.getName(doctorAppointmentTableHandler.getFromList(row, DOCTORID))
            	+ "\n\t\tDate:\t" + doctorAppointmentTableHandler.getFromList(row, DAY) + " "
            	+ doctorAppointmentTableHandler.getFromList(row, MONTH) + " "
            	+ doctorAppointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot:\t"
            	+ doctorAppointmentTableHandler.getFromList(row, TIMESLOT);
         formattedSlots.add(format);
      }
      Integer choice = new PromptFormatter.Poll(formattedSlots, "Select Time Slot")
         	.pollUntilValid().getAnswerIndex();
   	
      if (choice == null) 
         return null;
   	
      return availableSlots.get(choice);
   }
	
   /**
	 * Schedules an appointment for patient by selecting an available date
	 * <p>
	 * The method allows the patient to select an available time slot, checks if there are any clashing appointments
	 * for the patient on the same date and time, and then schedules the appointment if no conflicts are found.
	 * It updates the appointment and doctor appointment tables simultaneously to reflect the scheduled appointment. 
	 * The appointment status is set to "Pending" when the user selects a valid date that has an available appointment
	 * slot.
	 * </p>
	 *
	 * @param patientId The ID of the patient scheduling the appointment.
	 * @throws Exception If an error occurs during querying, table updates, or any other unexpected conditions.
	 */
   private static void patientScheduleAppointment(String patientId) throws Exception {
      boolean returnHome = false;
   	
      while (!returnHome) {
      	
         List<String> appointmentInfo = promptSelectAvailableTimeSlots();
         if (appointmentInfo == null || appointmentInfo.isEmpty()) {
            returnHome = true;
            continue;
         }
      
         String scheduleId = doctorAppointmentTableHandler.getFromList(appointmentInfo, SCHEDULEID);
         String doctorId = doctorAppointmentTableHandler.getFromList(appointmentInfo, DOCTORID);
         String year = doctorAppointmentTableHandler.getFromList(appointmentInfo, YEAR);
         String month = doctorAppointmentTableHandler.getFromList(appointmentInfo, MONTH);
         String day = doctorAppointmentTableHandler.getFromList(appointmentInfo, DAY);
         String time = doctorAppointmentTableHandler.getFromList(appointmentInfo, TIMESLOT);
      
         String clashingAppointments = appointmentTableHandler.new TableQuery(null)
            .where(PATIENTID).matches(patientId)
            .and()
            .where(STATUS).doesNotMatch("Cancelled")
            .and()
            .where(STATUS).doesNotMatch("Completed")
            .and()
            .where(DAY).matches(day)		// there must be a better way to do this date query, though.
            .and()
            .where(MONTH).matches(month)
            .and()
            .where(YEAR).matches(year)
            .and()
            .where(TIMESLOT).matches(time)
            .execute().getSingleResult();
      	
         if (clashingAppointments != null) {
            System.out.println("You have a Confirmed appointment for this time slot. Please try again.");
            continue;
         }
      	
         if (scheduleId != null) scheduleId = scheduleId.replace("'", "");
         String newAppointmentId = "'" + patientId + scheduleId;
         scheduleId = "'" + scheduleId;
      	
         List<String> newRow = Arrays.asList(
            	newAppointmentId, patientId, day, month, year, time, "Pending", doctorId
            );
      	
      	// Update tables
         appointmentTableHandler.addRow(
            	Stream.concat(
            		newRow.stream(),
            		Stream.generate(
            			() -> "NA"
            		).limit(appointmentTableHandler.format.getVariableCount() - newRow.size())
            	).collect(Collectors.toList())
            );
      	
         
         System.out.println("Appointment Status Pending.");
         returnHome = true;
      }
   }
	
   private static String localGetFromListNoThrow(TableHandler handler, List<String> row, String variableName) {
      try {
         return handler.getFromList(row, variableName);
      } catch (UndefinedVariableException e) {
         e.printStackTrace();
      } catch (TableMismatchException e) {
         e.printStackTrace();
      }
      return null;
   }
	
   /**
	 * Sorts a list of appointment IDs based on the date of the corresponding appointments.
	 * <p>
	 * This method retrieves appointment data for each provided appointment ID, sorts the data based on the 
	 * appointment date, and returns a list of sorted appointment IDs.
	 * </p>
	 *
	 * @param appointmentIds A list of appointment IDs to be sorted.
	 * @return A list of appointment IDs sorted by their associated appointment date.
	 */
   private static List<String> sortAppointmentIdByDate(TableHandler handler, List<String> appointmentIds) {
      return sortFullColumnQueryResultByTime(
         handler, new ArrayList<List<String>>(
         		appointmentIds.stream().map(id -> handler.readRow(id)).toList()
         )
         )
         .stream().map(r -> localGetFromListNoThrow(handler, r, APPOINTMENTID))
         .toList();
   }
	
   /**
	 * Displays the past appointment outcomes for a specific patient.
	 * <p>
	 * This method allows a patient to view their past completed appointments and the details of each appointment outcome.
	 * The method queries the patientAppointment for completed appointments based on the provided date and displays 
	 * details like type of service, medications, and diagnoses for each appointment.
	 * </p>
	 *
	 * @param patientId The ID of the patient whose appointment outcomes are being viewed.
	 * @throws Exception If an error occurs during the process (e.g., database query failure or data extraction issues).
	 */
   private static void readPersonalOutcomeRecord(String patientId) throws Exception {
      TableQuery completedAppointmentQuery = appointmentTableHandler.new TableQuery(null)
         .where(PATIENTID).matches(patientId)
         .and()
         .where(STATUS).matches("Completed");
   	
      String decision = new PromptFormatter.Poll(Arrays.asList("View all past records", "Select date"))
         .pollUntilValid()
         .getAnswerString();
      if (decision == null) 
         return;
   	
      if (decision.equals("Select date")) {
         Date date = PromptFormatter.collectDateInput();
         if (date == null) 
            return;
      	
         completedAppointmentQuery.and()
            .where(DAY).matches(date.getDay())
            .and()
            .where(MONTH).matches(date.getMonth())
            .and()
            .where(YEAR).matches(date.getYear());
      }
   	
      PromptFormatter.printSeparation("Past Appointment Outcomes");
   	
      List<List<String>> queryResult = completedAppointmentQuery.yield();
      if (TableQuery.isEmptyResult(queryResult)) {
         System.out.println("> No Appointment Outcome Records to Date.");
         return;
      }
   	
   	// flatten list
      List<String> appointmentIds = sortAppointmentIdByDate(appointmentTableHandler, queryResult.stream().map(r -> r.getFirst()).toList());
      List<String> requiredColumns = Arrays.asList(TYPE_OF_SERVICE, MEDICATIONS, DIAGNOSES, TREATMENTS);
   	// query row only has one column, that is the appointmentId
      for (String appointmentId : appointmentIds) {
      	
         String date = getAppointmentDate(appointmentId);
         String doctorName = UserManager.getName(appointmentTableHandler.readVariable(appointmentId, DOCTORID));
      	
         System.out.println("(Dated " + date + ")");
         System.out.println("By Dr. " + doctorName);
      	
         PromptFormatter.getJustifiedLines(
            requiredColumns.stream().map(
            v -> {
               try {
                  return v + ":" + appointmentTableHandler.readVariable(appointmentId, v);
               } catch (UndefinedVariableException e) {
                  e.printStackTrace();
               }
               return null;
            }).toList()
            ).forEach(l -> System.out.println(l));
      	
         System.out.println();
      }
   	
      PromptFormatter.printSeparation("");
   }

   
   private static String getAppointmentDate(String appointmentId) {
      return String.join(" ", Arrays.asList(DAY, MONTH, YEAR, TIMESLOT).stream().map(
         v -> {
            try {
               return appointmentTableHandler.readVariable(appointmentId, v);
            } catch (UndefinedVariableException e) {
               e.printStackTrace();
            }
            return null;
         }).toList());
   }

   /**
	 * Displays the confirmed appointments for a specific doctor on a given date.
	 * <p>
	 * This method allows a doctor to view their confirmed appointments for a selected date. The doctor is prompted to input
	 * a date, and the method retrieves the confirmed appointments for that day. If appointments exist, they are sorted by 
	 * timeslot and displayed in a formatted manner with the appointment details.
	 * </p>
	 *
	 * @param doctorId The ID of the doctor whose confirmed appointments are being viewed.
	 * @throws Exception If an error occurs during the process (e.g., database query failure or data extraction issues).
	 */
   private static void readPersonalAppointment(String doctorId) throws Exception {
   
      boolean returnHome = false;
      while (!returnHome) {
         List<List<String>> sortedInfo = null;
      
         Date timeInfo = PromptFormatter.collectDateInput();
         if (timeInfo == null) 
            return;
      	
         String year = timeInfo.getYear();
         String month = timeInfo.getMonth();
         String day = timeInfo.getDay();
      
         List<List<String>> confirmedAppointments = doctorAppointmentTableHandler.new TableQuery(
            	doctorAppointmentTableHandler.ALL_COLUMNS
            )
            .where(DOCTORID).matches(doctorId)
            .and()
            .where(STATUS).matches("Confirmed")
            .and()
            .where(DAY).matches(day)
            .and()
            .where(MONTH).matches(month)
            .and()
            .where(YEAR).matches(year)
            .yield();
      
         if (TableQuery.isEmptyResult(confirmedAppointments)) {
            System.out.println("> No confirmed appointments.");
            return;
         }
      
         sortedInfo = new ArrayList<>();
         sortedInfo = sortFullColumnQueryResultByTime(doctorAppointmentTableHandler, confirmedAppointments);
         List<String>formattedSortedInfo = PromptFormatter.formatFullColumnSubtable(
            	Arrays.asList(TIMESLOT, APPOINTMENT), sortedInfo, doctorAppointmentTableHandler
            );
      	
         PromptFormatter.printSeparation("Confirmed Time Slots");
         IntStream.range(0, formattedSortedInfo.size()).forEach(
            i -> {
               System.out.println((i + 1) + ".");
               PromptFormatter.getJustifiedLines(
                  Arrays.asList(formattedSortedInfo.get(i).split("\n"))
                  ).forEach(l -> { System.out.println(l); });
            });
         PromptFormatter.printSeparation("");
      }
   }

   /**
	 * Allows doctor to schedule an appointment for a specified date and timeslot.
	 * <p>
	 * This method prompts the doctor to input a date and timeslot, checks for any existing appointment clashes,
	 * and then allows the doctor to enter details about the appointment. Depending on the type of appointment,
	 * the appointment's status is set to either "Available" or "Confirmed." If no clashes are found, the appointment is added to the system.
	 * </p>
	 *
	 * @param doctorId The ID of the doctor scheduling the appointment.
	 * @throws Exception If an error occurs during the process (e.g., unable to open csv, invalid input).
	 */
   private static void doctorScheduleAppointment(String doctorId) throws Exception {
   
      boolean returnHome = false;
   	
      while (!returnHome) {
         Date timeInfo = PromptFormatter.collectDateInput();
         if (timeInfo == null) 
            return;
      	
         String year = timeInfo.getYear();
         String month = timeInfo.getMonth();
         String day = timeInfo.getDay();
      
         String targetTime = new PromptFormatter.Poll(Date.ALL_TIMESLOTS, "Time Slots").pollUntilValid()
            	.getAnswerString();
      
         if (targetTime == null) {
            returnHome = true;
            continue;
         }
      
         String checkClash = doctorAppointmentTableHandler.new TableQuery(null)
            .where(DOCTORID).matches(doctorId)
            .and()
            .where(DAY).matches(day)
            .and()
            .where(TIMESLOT).matches(targetTime)
            .and()
            .where(MONTH).matches(month)
            .and()
            .where(YEAR).matches(year)
            .execute().getSingleResult();
      	
         if (checkClash != null) {
            System.out.println("> You have an active appointment for this time slot. Please try again.");
            continue;
         }
      
         String status = null;
         System.out.println("Enter appointment details:\t");
         String appointmentDetails = Command.inputScanner.nextLine();
      
         if (appointmentDetails.toLowerCase().equals("consultation")) {
            status = "Available";
         } else {
            status = "Confirmed";
         }
      
         String newScheduleId = "'" + doctorId + year + Date.ALL_MONTHS.indexOf(month) + day + targetTime.replaceAll(":", "");
      
         doctorAppointmentTableHandler.addRow(Arrays.asList(
            newScheduleId, doctorId, day, month, year, targetTime, status, appointmentDetails, "NA", "NA"
            ));
      
         if (appointmentDetails.toLowerCase().equals("consultation")) {
            System.out.println("> Status Available for consultation.");
         } else {
            status = "Confirmed";
            System.out.println("> Status confirmed for appointment.");
         }
         return;
      }
   }

   /**
	 * Allows doctor to accept or decline an appointment for a specified date and timeslot.
	 * <p>
	 * This method prompts the doctor to accept or decline a pending appointment requested by the patient.
	 * The appointment's status is set to either "Confirmed" or "Cancelled." If no clashes are found, the appointment is added to the system.
	 * </p>
	 *
	 * @param doctorId The ID of the doctor scheduling the appointment.
	 * @throws Exception If an error occurs during the process (e.g., unable to open csv, invalid input).
	 */
   private static void writeAppointmentRequests(String doctorId) throws Exception {
   
      List<List<String>> pendingAppointments = appointmentTableHandler.new TableQuery(
         	appointmentTableHandler.ALL_COLUMNS
         )
         .where(DOCTORID).matches(doctorId)
         .and()
         .where(STATUS).matches("Pending")
         .yield();
   	
      boolean returnHome = false;
      while (!returnHome) {
      
         if (TableQuery.isEmptyResult(pendingAppointments)) {
            System.out.println("You do not have pending appointments at the moment!");
            return;
         }
      
         pendingAppointments = sortFullColumnQueryResultByTime(appointmentTableHandler, pendingAppointments);
         List<String> pendingAppointmentsFormatted = new ArrayList<>();
      	
         for (List<String> row : pendingAppointments) {
            String format = "Patient ID:\t" + appointmentTableHandler.getFromList(row, "PatientId")
               + "\n\t\tDate:\t" + appointmentTableHandler.getFromList(row, DAY) + " "
               + appointmentTableHandler.getFromList(row, MONTH) + " "
               + appointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot:\t"
               + appointmentTableHandler.getFromList(row, TIMESLOT);
            pendingAppointmentsFormatted.add(format);
         }
         
         Integer appointmentIndex = new PromptFormatter.Poll(
            	pendingAppointmentsFormatted, "Pending Appointments"
            )
            .pollUntilValid()
            .getAnswerIndex();
      	
         if (appointmentIndex == null) 
            return;
      	
         List<String> chosenAppointment = pendingAppointments.get(appointmentIndex);
         PromptFormatter.printSeparation("Selected Appointment");
         System.out.println("-\t\t" + pendingAppointmentsFormatted.get(appointmentIndex));
         PromptFormatter.printSeparation("");
      
         List<String> options = Arrays.asList("Approve", "Reject");
         String decision = new PromptFormatter.Poll(options, "").pollUntilValid().getAnswerString();
      
         if (decision == null) 
            return;
      	
         String appointmentId = appointmentTableHandler.getFromList(
            	chosenAppointment, APPOINTMENTID
            );
      	
         String scheduleId = doctorAppointmentTableHandler.new TableQuery(null)
            .where(DOCTORID).matches(doctorId)
            .and()
            .where(DAY).matches(appointmentTableHandler.getFromList(chosenAppointment, DAY))
            .and()
            .where(MONTH).matches(appointmentTableHandler.getFromList(chosenAppointment, MONTH))
            .and()
            .where(YEAR).matches(appointmentTableHandler.getFromList(chosenAppointment, YEAR))
            .and()
            .where(TIMESLOT).matches(appointmentTableHandler.getFromList(chosenAppointment, TIMESLOT))
            .execute().getSingleResult();
      	
         if (scheduleId == null) throw new Exception("DEBUG ASSERTION FAILED. scheduleId was null");
      
         if (decision.equals("Approve")) {
            doctorAppointmentTableHandler.updateVariable(scheduleId, STATUS, "Confirmed");
            doctorAppointmentTableHandler.updateVariable(scheduleId, PATIENTID, appointmentTableHandler.getFromList(chosenAppointment, PATIENTID));
            doctorAppointmentTableHandler.updateVariable(scheduleId, APPOINTMENTID, appointmentId);
            appointmentTableHandler.updateVariable(appointmentId, STATUS, "Confirmed");
         	
            List<List<String>> otherAppointments = appointmentTableHandler.new TableQuery(
               	appointmentTableHandler.ALL_COLUMNS
               )
               .where(DOCTORID).matches(doctorId)
               .and()
               .where(STATUS).matches("Pending")
               .and()
               .where(DAY).matches(appointmentTableHandler.getFromList(chosenAppointment, DAY))
               .and()
               .where(MONTH).matches(appointmentTableHandler.getFromList(chosenAppointment, MONTH))
               .and()
               .where(YEAR).matches(appointmentTableHandler.getFromList(chosenAppointment, YEAR))
               .and()
               .where(TIMESLOT).matches(appointmentTableHandler.getFromList(chosenAppointment, TIMESLOT))
               .yield();
         	
            for (List<String> otherAppointment : otherAppointments) {
               String otherAppointmentId = appointmentTableHandler.getFromList(otherAppointment, APPOINTMENTID);
               if (!otherAppointmentId.equals(appointmentId)) {
                  appointmentTableHandler.updateVariable(otherAppointmentId, STATUS, "Cancelled");
               }
            }
         	
            System.out.println("Appointment Confirmed.");
            return;
         }
      	
         appointmentTableHandler.updateVariable(appointmentId, STATUS, "Cancelled");
         System.out.println("Appointment Cancelled.");
      	
         return;
      }
   }

   /**
	 * Allows doctors to read all upcoming confirmed appointments details.
	 * <p>
	 * This method allows the user to view all upcoming confirmed appointment status and prints it on the
	 * console.
	 * The method filters the appointmentSlot csv to find the entries with Status marked as confirmed,
	 * and if the ID matches doctorId, passed into the method, it will store the entry in the ArrayList 
	 * consultationInfoFormatted, and prints all rows in the ArrayList. If the ArrayList is empty, it will 
	 * display no confirmed appointment.
	 * </p>
	 *
	 * @param doctorId The ID of the user viewing the scheduled appointment.
	 * @throws Exception If an error occurs during the process (e.g., unable to open csv, invalid input).
	 */
   private static void readUpcomingAppointments(String doctorId) throws Exception {
      List<List<String>> appointmentInfo = appointmentTableHandler.new TableQuery(appointmentTableHandler.ALL_COLUMNS)
         .where(DOCTORID).matches(doctorId)
         .and()
         .where(STATUS).matches("Confirmed")
         .yield();
   	
      if (TableQuery.isEmptyResult(appointmentInfo)) {
         System.out.println("> No confirmed appointments.");
         return;
      }
   	
      List<String> consultationInfoFormatted = new ArrayList<>();
      for (List<String> row : sortFullColumnQueryResultByTime(appointmentTableHandler, appointmentInfo)) {
         String format = "\tPatient ID:\t" + appointmentTableHandler.getFromList(row, "PatientId")
               + "\n\t\tDate:\t" + appointmentTableHandler.getFromList(row, DAY) + " "
               + appointmentTableHandler.getFromList(row, MONTH) + " "
               + appointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot:\t"
               + appointmentTableHandler.getFromList(row, TIMESLOT)
               + "\n\t\tAppointment Details:\tConsultation";
         consultationInfoFormatted.add(format);
      
      }
   
      PromptFormatter.printSeparation("Selected Appointment");
      for (String row : consultationInfoFormatted) {
         System.out.println("-  " + row);
      }
   }

   /**
	 * Prompts the doctor to select or enter a valid patient ID, ensuring the ID corresponds to an existing patient.
	 * <p>
	 * This method validates the input patient ID to ensure it belongs to an existing user with the "Patient" role.
	 * If the provided patient ID is valid but does not yet exist in the `appointmentTableHandler` records, 
	 * a new row is automatically added for the patient in the appointment table.
	 * </p>
	 *
	 * @return The validated patient ID as a string.
	 * @throws Exception If an error occurs during the input session or table operations.
	 */
   private static String promptSelectPatientId() throws Exception {
      return new InputSession<String>("Please enter Patient's hospitalId")
         // print when input is invalid
         .setOnInvalidInput("Patient not found!")
         // call back to check whether an id is valid, and if it is, whether is it of a
         // patient's.
         .setValidator(s -> UserManager.isExistentUser(s) && RoleManager.getRoleName(s).equals("Patient"))
         // end input session
         .startPrompt();
   }

   /**
	 * Prompts the doctor to update Diagnoses, Medications, or Treatments (DMT) for a selected patient appointment.
	 * <p>
	 * The method performs the following steps:
	 * <ul>
	 *     <li>Prompts the doctor to select a patient ID and fetch their appointment records.</li>
	 *     <li>Displays available appointments and allows the doctor to select an appointment by date and time slot.</li>
	 *     <li>Prompts the doctor to choose a DMT field (Diagnoses, Medications, or Treatments) and an operation 
	 *         (Update, Add, or Delete).</li>
	 *     <li>Validates and applies the requested changes to the DMT records both locally and in the database.</li>
	 * </ul>
	 * </p>
	 *
	 * @param doctorId The ID of the doctor performing the update.
	 * @throws Exception If an error occurs during input sessions or database operations.
	 */
   private static void promptUpdateDMT(String doctorId) throws Exception {
      String patientId = promptSelectPatientId();
      if (patientId == null){
         return;
      }
   	// appointmentString returns the patient appointment info to edit
      List<String> appointmentString = getPatientDMTTimeSlots(patientId);
      if (appointmentString == null) {	// patient does not have completed appointments 
         return;
      }
      if (appointmentString.size() == 0) {
         return;
      }
   
      String appointmentId = appointmentTableHandler.getFromList(appointmentString, APPOINTMENTID);
      String year = appointmentTableHandler.getFromList(appointmentString, YEAR);
      String month = appointmentTableHandler.getFromList(appointmentString, MONTH);
      String day = appointmentTableHandler.getFromList(appointmentString, DAY);
      String timeSlot = appointmentTableHandler.getFromList(appointmentString, TIMESLOT);
   
   	/* To collect all possible arguments before writing to database */
      List<String> answers = new PromptFormatter.NestedPolls(Arrays.asList(
         	new PromptFormatter.Poll(Arrays.asList("Diagnoses", "Treatments", "Medications"), "Select Field"),
         	new PromptFormatter.Poll(Arrays.asList("Update", "Add", "Delete"), "Select Operation")))
         	.pollNestedUntilValid().getAnswerStrings();
   
      if (answers == null)
         return;
   
      String variableName = answers.get(0); // DMT
   	// extract the cell of the variable name which is of string type
      String oldCell = appointmentTableHandler.getFromList(appointmentString, variableName);
      List<String> oldCellList = appointmentTableHandler.readListVariable(oldCell); // convert delimited cell string
   																					// into separate DMT list
   
      provideDMT(oldCellList, variableName); // print the indexed DMTs
      Integer listSize = oldCellList.size(); // get existing number of DMTs
      String operation = answers.get(1);
      Integer valueIndex = null;
      String newValue = null;
   
      if (!operation.equals("Add")) {
         if (listSize < 1) {
            System.out.println("No existing record!");
            return;
         }
      	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~input session that yields an Integer
         valueIndex = new PromptFormatter.InputSession<Integer>("Please enter the index number of the record")
            	// callback for conversion to Integer
            	.setConverter(Integer::parseInt)
            	// print when input is invalid
            	.setOnInvalidInput("Please enter numbers from 1 to " + listSize + "!")
            	// the input session will end, yielding null as output, when the user enters
            	// "-q"
            	.setSessionTerminationToken("-1")
            	// callback to verify the range of input (implicitly converted before comparing)
            	.setValidator(i -> i > 0 && i <= listSize)
            	// end input session
            	.startPrompt();
      
      	// if the user chose to quit, then valueIndex is null
         if (valueIndex == null) 
            return;
      	
         valueIndex -= 1; // retrieve 0-based index
      }
   
      if (!operation.equals("Delete")) {
      	// ~~~~~~~~~~~~~~~~~~~~~~~~~input session that yields a String
         newValue = new PromptFormatter.InputSession<String>("Please Enter new " + variableName).startPrompt();
      
         if (newValue == null) 
            return;
      }
   	
   
   	// build the command target object
      MedicalRecordModifier commandTarget = new MedicalRecordModifier(
         	operation, 
         	variableName, 
         	patientId,
         	valueIndex != null ? valueIndex : -1, // Integer value index converts to int, cannot be null
         	new Date(Arrays.asList(day, month, year, timeSlot)), 
         	newValue
         );
   
   	// configure and dispatch follow up command
      HospitalManagementSystem.setTarget(doctorId, commandTarget);
      HospitalManagementSystem.dispatchCommand(new MedicalRecordManager.Command("WRITE_ANY_MEDICAL_RECORD"));
   
   	// Local modification end point
      List<String> editCell = new ArrayList<>(oldCellList);
      switch (operation) {
         case "Add" -> {
            editCell.add(newValue); // add new variable to the cell
         }
      
         case "Delete" -> {
            editCell.remove(valueIndex.intValue());
         }
      
         case "Update" -> {
            editCell.set(valueIndex, newValue);
         }
      
         case "Return" -> {
            return;
         }
      }
   	
      appointmentTableHandler.updateVariable(appointmentId, variableName, editCell);
      System.out.println("> " + variableName + " modified successfully.");
   
   }
	
   /**
	 * Facilitates the completion of a confirmed appointment for a doctor.
	 * 
	 * <p>This method allows a doctor to:
	 * <ul>
	 *   <li>Select a confirmed appointment for a specific patient.</li>
	 *   <li>Update the patient's medical records, including Diagnoses, Treatments, Medications, 
	 *       and Type of Service.</li>
	 *   <li>Update the appointment's prescription information and mark it as completed.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>Steps:
	 * <ol>
	 *   <li>Retrieve and display confirmed appointments for the selected patient and doctor.</li>
	 *   <li>Allow the doctor to select an appointment to complete.</li>
	 *   <li>Prompt the doctor to add Diagnoses, Treatments, Medications, and Type of Service 
	 *       records to the appointment.</li>
	 *   <li>Update the prescription status and prescribed quantity.</li>
	 *   <li>Mark the appointment as completed in both the doctor's schedule and patient's appointment records.</li>
	 * </ol>
	 * </p>
	 * 
	 * @param doctorId the ID of the doctor completing the appointment.
	 * @throws Exception if an error occurs during input handling or database updates.
	 */
   private static void writeAppointmentOutcome(String doctorId) throws Exception {
      String patientId = promptSelectPatientId();
      if (patientId == null){
         return;
      }
      
      List<List<String>> confirmedAppointments = doctorAppointmentTableHandler.new TableQuery(doctorAppointmentTableHandler.ALL_COLUMNS)
         .where(STATUS).matches("Confirmed")
         .and()
         .where(DOCTORID).matches(doctorId)
         .and()
         .where(PATIENTID).matches(patientId)
         .yield();
   
      if (TableQuery.isEmptyResult(confirmedAppointments)) {
         System.out.println("No confirmed appointments!");
         return;
      }
   	
      confirmedAppointments = sortFullColumnQueryResultByTime(doctorAppointmentTableHandler, confirmedAppointments);
   
      List<String> confirmedAppointmentsFormatted = new ArrayList<>();
      for (List<String> row : confirmedAppointments) {
         String format = "Patient ID:\t" + doctorAppointmentTableHandler.getFromList(row, PATIENTID)
               + "\n\t\tDate:\t" + doctorAppointmentTableHandler.getFromList(row, DAY) + " "
               + doctorAppointmentTableHandler.getFromList(row, MONTH) + " "
               + doctorAppointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot:\t"
               + doctorAppointmentTableHandler.getFromList(row, TIMESLOT);
         confirmedAppointmentsFormatted.add(format);
      }
   
      Integer appointmentIndex = new PromptFormatter.Poll(confirmedAppointmentsFormatted, "Confirmed Appointments")
         	.pollUntilValid().getAnswerIndex();
   
      if (appointmentIndex == null) 
         return;
   
      List<String> chosenAppointment = confirmedAppointments.get(appointmentIndex);
   
      PromptFormatter.printSeparation("Selected Appointment");
      System.out.println("-\t\t" + confirmedAppointmentsFormatted.get(appointmentIndex));
      PromptFormatter.printSeparation("");
   
      List<String> options = Arrays.asList("Complete Appointment");
      String decision = new PromptFormatter.Poll(options, " Select option").pollUntilValid().getAnswerString();
   
      if (decision == null)
         return;
   
      String appointmentId = doctorAppointmentTableHandler.getFromList(chosenAppointment, APPOINTMENTID);
   
      String scheduleId = doctorAppointmentTableHandler.getFromList(chosenAppointment, SCHEDULEID);
   
      String year = doctorAppointmentTableHandler.getFromList(chosenAppointment, YEAR);
      String month = doctorAppointmentTableHandler.getFromList(chosenAppointment, MONTH);
      String day = doctorAppointmentTableHandler.getFromList(chosenAppointment, DAY);
      String timeSlot = doctorAppointmentTableHandler.getFromList(chosenAppointment, TIMESLOT);
   
      Integer diagnosesCount = new PromptFormatter.InputSession<Integer>("Enter number of " + DIAGNOSES)
           .setConverter(Integer::parseInt)
           .setValidator(n -> n > 0)
           .setOnInvalidInput("Input must be a number greater than zero!")
           .startPrompt();
      if (diagnosesCount == null) 
         return;
   
   	
   	// add diagnoses for newly completed appointments
      int valueIndex = -1;
      
      List<String> diagnosesValue = new ArrayList<>();
      for (int i = 0; i < diagnosesCount; i++) {
         diagnosesValue
            	.add(new PromptFormatter.InputSession<String>("Please enter " + DIAGNOSES + " " + (i + 1) + "", true)
            			.startPrompt());
      
         MedicalRecordModifier commandTarget = new MedicalRecordModifier("Add", DIAGNOSES, patientId, valueIndex,
            	new Date(Arrays.asList(day, month, year, timeSlot)), diagnosesValue.get(i));
      
         HospitalManagementSystem.setTarget(doctorId, commandTarget);
         HospitalManagementSystem.dispatchCommand(new MedicalRecordManager.Command("WRITE_ANY_MEDICAL_RECORD"));
      }
      String diagnosesCell = String.join(";", diagnosesValue);
      diagnosesCell += ";";
      appointmentTableHandler.updateVariable(appointmentId, DIAGNOSES, diagnosesCell);
   	
   
   	// add treatments for newly completed appointments
      Integer treatmentCount = new PromptFormatter.InputSession<Integer>("Enter number of " + TREATMENTS)
           .setConverter(Integer::parseInt)
           .setValidator(n -> n > 0)
           .setOnInvalidInput("Input must be a number greater than zero!")
           .startPrompt();
      if (treatmentCount == null) 
         return;
      
      List<String> treatmentsValue = new ArrayList<>();
      for (int i = 0; i < treatmentCount; i++) {
         treatmentsValue
            	.add(new PromptFormatter.InputSession<String>("Please enter " + TREATMENTS + " " + (i + 1) + "", true)
            			.startPrompt());
      
         MedicalRecordModifier commandTarget = new MedicalRecordModifier("Add", TREATMENTS, patientId, valueIndex,
            	new Date(Arrays.asList(day, month, year, timeSlot)), treatmentsValue.get(i));
      
         HospitalManagementSystem.setTarget(doctorId, commandTarget);
         HospitalManagementSystem.dispatchCommand(new MedicalRecordManager.Command("WRITE_ANY_MEDICAL_RECORD"));
      }
      String treatmentsCell = String.join(";", treatmentsValue);
      treatmentsCell += ";";
      appointmentTableHandler.updateVariable(appointmentId, TREATMENTS, treatmentsCell);
   	
   	// add medications for newly completed appointments
      Integer medicationCount = new PromptFormatter.InputSession<Integer>("Enter number of " + MEDICATIONS)
           .setConverter(Integer::parseInt)
           .setValidator(n -> n > 0)
           .setOnInvalidInput("Input must be a number greater than zero!")
           .startPrompt();
      if (medicationCount == null) 
         return;
      
      List<String> medicationsValue = new ArrayList<>();
      for (int i = 0; i < medicationCount; i++) {
         medicationsValue
            	.add(new PromptFormatter.InputSession<String>("Please enter " + MEDICATIONS + " " + (i + 1) + "", true)
            			.startPrompt());
                       
         MedicationStockModifier commandTarget1 = new MedicationStockModifier(medicationsValue.get(i), 1, false);
         HospitalManagementSystem.setTarget(doctorId, commandTarget1);
         HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("CHECK_FOR_MEDICINE"));
         if (!commandTarget1.getavalibility()) {
            System.out.println(medicationsValue.get(i) + " is not available");
            medicationsValue.remove(i);
            i--;
            continue;
         }           
      
         MedicalRecordModifier commandTarget2 = new MedicalRecordModifier("Add", MEDICATIONS, patientId, valueIndex,
            	new Date(Arrays.asList(day, month, year, timeSlot)), medicationsValue.get(i));
      
         HospitalManagementSystem.setTarget(doctorId, commandTarget2);
         HospitalManagementSystem.dispatchCommand(new MedicalRecordManager.Command("WRITE_ANY_MEDICAL_RECORD"));
      }
      String medicationsCell = String.join(";", medicationsValue);
      medicationsCell += ";";
      appointmentTableHandler.updateVariable(appointmentId, MEDICATIONS, medicationsCell);
   	
   	// add type of service for newly completed appointments
      Integer serviceCount = new PromptFormatter.InputSession<Integer>("Enter number of " + TYPE_OF_SERVICE)
           .setConverter(Integer::parseInt)
           .setValidator(n -> n > 0)
           .setOnInvalidInput("Input must be a number greater than zero!")
           .startPrompt();
      if (serviceCount == null) 
         return;
      
      List<String> serviceValue = new ArrayList<>();
      for (int i = 0; i < serviceCount; i++) {
         serviceValue.add(
            	new PromptFormatter.InputSession<String>("Please enter " + TYPE_OF_SERVICE + " " + (i + 1) + "", true)
            			.startPrompt());
      }
      String serviceCell = String.join(";", serviceValue);
      serviceCell += ";";
      System.out.println(serviceCell);
      appointmentTableHandler.updateVariable(appointmentId, TYPE_OF_SERVICE, serviceCell);
   
   	// update prescription info
      appointmentTableHandler.updateVariable(appointmentId, PRESCRIPTION_STATUS, "Pending");
      appointmentTableHandler.updateVariable(appointmentId, PRESCRIBED_QUANTITY, "1");
   
   	// update appointment status to completed
      String newStatus = "Completed";
      doctorAppointmentTableHandler.updateVariable(scheduleId, STATUS, newStatus);
      appointmentTableHandler.updateVariable(appointmentId, STATUS, newStatus);
      System.out.println("Appointment " + newStatus + ".");
   }

   /**
	 * Displays a list of Diagnoses, Medications, or Treatments for a patient in a formatted manner.
	 * 
	 * <p>If no records are found, an appropriate message is displayed. Otherwise, the records are listed
	 * with a numerical index for easier selection or reference.</p>
	 *
	 * @param row the list of DMT records to display.
	 * @param variableName the name of the variable (e.g., Diagnoses, Medications, Treatments) being displayed.
	 * @throws Exception if an error occurs during formatting or display.
	 */
   private static void provideDMT(List<String> row, String variableName) throws Exception {
      PromptFormatter.printSeparation(variableName);
      if (row.size() == 0) {
         System.out.println("	<No Records Found>");
         return;
      }
   
      int count = 1;
      for (String value : row) {
         System.out.println(count++ + ".\t" + value);
      }
      PromptFormatter.printSeparation("");
   }

   /**
	 * Retrieves and displays a list of completed appointment time slots for a specified patient
	 * on a given date. Allows the user to select an appointment and returns the corresponding
	 * data.
	 * 
	 * <p>Steps:
	 * <ol>
	 *   <li>Prompt the user to input a date for which to retrieve completed appointments.</li>
	 *   <li>Filter completed appointments for the specified patient on the given date.</li>
	 *   <li>Display the available time slots for selection, including details such as the
	 *       patient ID, date, and time slot.</li>
	 *   <li>Return the selected appointment data as a list of strings.</li>
	 * </ol>
	 * </p>
	 *
	 * @param patientId the ID of the patient whose completed appointment time slots are to be retrieved.
	 * @return a list of strings representing the selected appointment's data, or {@code null} if no 
	 *         completed appointments are found or the user cancels the selection process.
	 * @throws Exception if an error occurs during date input collection, data retrieval, or display.
	 */
   private static List<String> getPatientDMTTimeSlots(String patientId) throws Exception {
   
      Date timeInfo = PromptFormatter.collectDateInput();
      if (timeInfo == null) 
         return null;
   	
      String year = timeInfo.getYear();
      String month = timeInfo.getMonth();
      String day = timeInfo.getDay();
   
      List<List<String>> completedSlots = appointmentTableHandler.new TableQuery(appointmentTableHandler.ALL_COLUMNS)
         .where(PATIENTID).matches(patientId)
         .where(STATUS).matches("Completed")
         .and()
         .where(DAY).matches(day)
         .and()
         .where(MONTH).matches(month)
         .and()
         .where(YEAR).matches(year)
         .yield();
   
   	// Print available timeslots
      if (TableQuery.isEmptyResult(completedSlots)) {
         System.out.println("> No completed appointments for patient " + patientId + " " + "on " + day + " "
            	+ month + " " + year + ".");
         return null;
      }
   	
      List<String> completedSlotsFormatted = new ArrayList<>();
      for (List<String> row : completedSlots) {
         String format = "Patient ID:\t" + appointmentTableHandler.getFromList(row, "PatientId")
               + "\n\t\tDate:\t" + appointmentTableHandler.getFromList(row, DAY) + " "
               + appointmentTableHandler.getFromList(row, MONTH) + " "
               + appointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot:\t"
               + appointmentTableHandler.getFromList(row, TIMESLOT);
         completedSlotsFormatted.add(format);
      }
      
      Integer index = new PromptFormatter.Poll(completedSlotsFormatted, "Completed Appointments").pollUntilValid()
         	.getAnswerIndex();
   
      if (index == null) 
         return null;
      return completedSlots.get(index);
   
   }

   /**
	 * Displays all confirmed appointments for a specified patient.
	 * 
	 * <p>The method retrieves all appointments with the status "Confirmed" for the patient
	 * identified by {@code patientId}. It then formats and displays the appointments,
	 * including doctor name, date, time slot, and consultation details.</p>
	 * 
	 * @param patientId the ID of the patient whose appointments are to be displayed.
	 * @throws Exception if an error occurs during data retrieval or formatting.
	 */
   private static void patientViewAppointments(String patientId) throws Exception {
      List<String> consultationInfoFormatted = new ArrayList<>();
   
      List<List<String>> appointmentInfo = appointmentTableHandler.new TableQuery(appointmentTableHandler.ALL_COLUMNS)
         .where(PATIENTID).matches(patientId)
         .and()
         .where(STATUS).matches("Confirmed")
         .yield();
   
      if (TableQuery.isEmptyResult(appointmentInfo)) {
         System.out.println("> No confirmed appointments.");
         return;
      }
   
      for (List<String> row : sortFullColumnQueryResultByTime(appointmentTableHandler, appointmentInfo)) {
         String format = "\tDoctor Name:\t" + UserManager.getName(appointmentTableHandler.getFromList(row, DOCTORID))
            	+ "\n\t\tDate:\t" + appointmentTableHandler.getFromList(row, DAY) + " "
            	+ appointmentTableHandler.getFromList(row, MONTH) + " "
            	+ appointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot:\t"
            	+ appointmentTableHandler.getFromList(row, TIMESLOT) + "\n\t\tStatus:\t"
               + appointmentTableHandler.getFromList(row, STATUS)
            	+ "\n\t\tAppointment Details:\tConsultation";
         consultationInfoFormatted.add(format);
      }
   
   
      PromptFormatter.printSeparation("Selected Appointment");
      for (String row : consultationInfoFormatted) {
         System.out.println("-  " + row);
      }
   }

   /**
	 * Cancels a confirmed appointment for a specified patient.
	 * 
	 * <p>This method lists all confirmed appointments for the patient and allows them to select
	 * an appointment to cancel. Upon cancellation, the appointment ID is updated to reflect its
	 * cancelled status, and corresponding records in the doctor's schedule are updated as well.</p>
	 * 
	 * @param patientId the ID of the patient whose appointment is to be cancelled.
	 * @return {@code true} if the appointment was successfully cancelled; {@code false} otherwise.
	 * @throws Exception if an error occurs during data retrieval, selection, or cancellation.
	 */
   private static boolean cancelAppointment(String patientId) throws Exception {
   //      List<String> requiredColumns = Arrays.asList(DOCTORID, YEAR, MONTH, DAY, TIMESLOT);
   	
      List<List<String>> confirmedAppointments = appointmentTableHandler.new TableQuery(
         	appointmentTableHandler.ALL_COLUMNS		// so that we can use getFromList
         )
         .where(PATIENTID).matches(patientId)
         .and()
         .where(STATUS).matches("Confirmed")
         .yield();
   	
      if (confirmedAppointments.isEmpty()) {
         System.out.println("You have no confirmed / pending appointments. Check back later.");
         return false;
      }
      
      List<String> formattedAppointments = new ArrayList<>();
      for (List<String> row : confirmedAppointments) {
         String appointmentInfo = "Doctor Name\t: " + UserManager.getName(appointmentTableHandler.getFromList(row, DOCTORID))
                  	+ "\n\t\tDate\t\t: " + appointmentTableHandler.getFromList(row, DAY) + " "
                  	+ appointmentTableHandler.getFromList(row, MONTH) + " "
                  	+ appointmentTableHandler.getFromList(row, YEAR) + "\n\t\tTime Slot\t: "
                  	+ appointmentTableHandler.getFromList(row, TIMESLOT);
         formattedAppointments.add(appointmentInfo);
      }
      
      
      Integer choice = new PromptFormatter.Poll(
         	formattedAppointments, 
         	"Select Appointment"
         ).pollUntilValid().getAnswerIndex();
   	
      if (choice == null) 
         return false;
   	
      String decision = new PromptFormatter.InputSession<String>(
         	"Are you sure you want to cancel this appointment?[y/n]"
         )
         .setValidator(s -> s.toLowerCase().equals("y") || s.toLowerCase().equals("n"))
         .startPrompt();
   	
      if (decision == null || decision.toLowerCase().equals("n")) 
         return false;
   	
      String selectedAppointmentId = appointmentTableHandler.getFromList(
         	confirmedAppointments.get(choice), APPOINTMENTID
         );
   	
      appointmentTableHandler.updateVariable(selectedAppointmentId, STATUS, "Cancelled");
   	
   	
      String lapsedAppointmentId = selectedAppointmentId + "C" + Instant.now().getEpochSecond();
   	// Update the Appointment ID to indicate its cancelled status
   	// This allows the patient to reschedule on the exact same time slot
      appointmentTableHandler.updateVariable(
         	selectedAppointmentId, APPOINTMENTID, lapsedAppointmentId
         );
   	
      String doctorScheduleId = doctorAppointmentTableHandler.new TableQuery(null)
         	.where(APPOINTMENTID).matches(selectedAppointmentId)
         	.execute().getSingleResult();
   	
      String lapsedScheduleId = doctorScheduleId + "C" + Instant.now().getEpochSecond();
   	
      doctorAppointmentTableHandler.updateVariable(doctorScheduleId, APPOINTMENTID, lapsedAppointmentId);
      doctorAppointmentTableHandler.updateVariable(doctorScheduleId, STATUS, "Available");
      doctorAppointmentTableHandler.updateVariable(doctorScheduleId, SCHEDULEID, lapsedScheduleId);
   	
      System.out.println("Successfully cancelled appointment!");
   	
      return true;
   }

   /**
	 * Reschedules an appointment for a specified patient.
	 * 
	 * <p>This method first cancels an existing appointment by invoking {@link #cancelAppointment},
	 * then allows the patient to schedule a new appointment.</p>
	 * 
	 * @param hospitalId the ID of the hospital where the appointment is to be rescheduled.
	 * @throws Exception if an error occurs during the cancellation or rescheduling process.
	 */
   private static void patientRescheduleAppointment(String hospitalId) throws Exception {
      if (!cancelAppointment(hospitalId)) 
         return;
      patientScheduleAppointment(hospitalId);
   }
	
   /**
	 * Displays all appointment outcome records.
	 * 
	 * <p>The method retrieves and prints all records from the appointment table,
	 * displaying the outcome details for each appointment.</p>
	 */
   private static void viewAppointmentOutcomeRecord() {
      PromptFormatter.printSeparation("Appointment Outcome Records");
   
      List<List<String>> AppointmentOutcomeRecords = appointmentTableHandler.getData();
      PromptFormatter.printTable(AppointmentOutcomeRecords, appointmentTableHandler.format.getVariableNames());
   	
      PromptFormatter.printSeparation("");
   }

   /**
	 * Prompts the pharmacist to input an appointment ID and validates it.
	 * 
	 * <p>The method ensures that the entered appointment ID exists in the system.
	 * If the ID is invalid, an error message is displayed, and the user is prompted
	 * to re-enter the ID.</p>
	 * 
	 * @return the validated appointment ID entered by the user.
	 * @throws Exception if an error occurs during input or validation.
	 */
   private static String promptSelectAppointmentId() throws Exception {
   
      String appointmentId = new InputSession<String>("Please enter Appointment's Id")
         	// print when input is invalid
         	.setOnInvalidInput("Appointment not found!")
         	// call back to check whether an id is valid, and if it is, whether is it of a
         	// patient's.
         	.setValidator(s -> appointmentTableHandler.isExistentId(s))
         	// end input session
         	.startPrompt();
   
      return appointmentId;
   }
	
   /**
	 * Updates the prescription status of an appointment based on medication availability.
	 * 
	 * <p>The method retrieves the prescribed medications for the appointment identified by
	 * {@code appointmentId}. It checks if the required medications are available in stock
	 * and updates the prescription status to "Dispensed" if all medications are available.
	 * If any medication is unavailable, the prescription status remains unchanged.</p>
	 * 
	 * @param pharmacistId the ID of the pharmacist performing the update.
	 * @throws Exception if an error occurs during medication validation or stock updates.
	 */
   private static void updatePrescriptionStatus(String pharmacistId) throws Exception {
      String appointmentId = promptSelectAppointmentId();
      if (appointmentId == null){
         return;
      }
      String[] medications = appointmentTableHandler.readVariable(appointmentId, MEDICATIONS).split(";");
      Boolean doPrescribe = true;
      
      if (!"NA".equals(medications[0]) && !"Dispensed".equals(appointmentTableHandler.readVariable(appointmentId, PRESCRIPTION_STATUS))) {
         for (String medicine : medications) {
            int deductQty = Integer
               	.parseInt(appointmentTableHandler.readVariable(appointmentId, PRESCRIBED_QUANTITY));
            MedicationStockModifier commandTarget = new MedicationStockModifier(medicine, deductQty, false);
         
            HospitalManagementSystem.setTarget(pharmacistId, commandTarget);
            HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("CHECK_FOR_MEDICINE"));
            if (!commandTarget.getavalibility()) {
               doPrescribe = false;
               System.out.println(medicine + " not available");
            }
         }
      
         if (doPrescribe) {
            for (String medicine : medications) {
               int deductQty = Integer
                  	.parseInt(appointmentTableHandler.readVariable(appointmentId, PRESCRIBED_QUANTITY));
               MedicationStockModifier commandTarget = new MedicationStockModifier(medicine, deductQty, true);
            
               HospitalManagementSystem.setTarget(pharmacistId, commandTarget);
               HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("UPDATE_STOCK_VALUE"));
            }
            appointmentTableHandler.updateVariable(appointmentId, PRESCRIPTION_STATUS, "Dispensed");
            System.out.println("Updated Appointment " + appointmentId + "'s prescription status");
            viewAppointmentOutcomeRecord();
         } else {
            System.out.println("Unable to dispense due to lack of medications");
         }
      }
      else if("Dispensed".equals(appointmentTableHandler.readVariable(appointmentId, PRESCRIPTION_STATUS))){
         System.out.println("The medication has already been dispensed.");
      } 
      else {
         System.out.println("There is no medication to dispense");
      }
   }
}
