package hms.manager;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hms.HospitalManagementSystem;
import hms.exception.UndefinedVariableException;
import hms.target.MedicalRecordModifier;
import hms.utility.Date;
import hms.utility.JointTableHandler;
import hms.utility.PromptFormatter;
import hms.utility.PromptFormatter.InputSession;
import hms.utility.WideTableHandler;


/**
 * This is a Manager class that manages and provides services regarding
 * the accesses to the medical records database (diagnoses.csv, treatments.csv, and medications.csv)
 */
public class MedicalRecordManager extends HospitalResourceManager {
   private static MedicalRecordManager mrmInstance = null;
   private static WideTableHandler diagnosisTableHandler;
   private static WideTableHandler medicationTableHandler;
   private static WideTableHandler treatmentTableHandler;
   private static JointTableHandler medicalRecordTableHandler;

	// Table Variable
   private static final String ID = "ID";
   private static final String DIAGNOSES = "Diagnoses";
   private static final String MEDICATIONS = "Medications";
   private static final String TREATMENTS = "Treatments";

	// * record1 record2 ...
	// id,item_a1;item_a2;date_a;,item_b1;...;item_bn;date_b; <-- An "ENTRY" or "RECORDS"
	// An instance of this class, or a "RECORD" stores items associated with a certain date
	// a MedicalReocrd maintains a list of "ITEM"'s
	/**
	 * This inner private class encapsulates the parsing logic exclusive to the tables handled by its
	 * containing class MedicalRecordManager, and is only intended to promote the declarative-ness and
	 * readability of this source file
	 */
   private static class MedicalRecord implements Comparable<MedicalRecord>{
      private List<String> items;
      private Date date;
      private boolean isNewRecord;
   
   	/**
   	 * Constructs an object representing a medical record, for easy processing.
   	 * 
   	 * @param ownerId the owner of the medical record, i.e., the patient's id.
   	 * @param variableName one of DIAGNOSES, MEDICATIONS or TREATMENTS. This 
   	 * indicates the destination (and source) table of the record.
   	 * @param recordIndex the position of the concerned record within the row
   	 * of the WideTable. For more details see the heading comment for this inner class.
   	 * @throws UndefinedVariableException
   	 */
      public MedicalRecord(String ownerId, String variableName, int recordIndex, String recordDate) throws 
      	UndefinedVariableException 
      {	
         this(recordIndex == -1 ? 
            	recordDate : // if record is empty (on first insertion)
            	medicalRecordTableHandler.getValue(ownerId, variableName, recordIndex)
            );
      	
         this.isNewRecord = (recordIndex == -1);
      }
   
   	/**
   	 * Parses the entries read from the medical records table into a list of
   	 * MedicalRecords.
   	 * 
   	 * @param entriesReadFromTable self-explanatory
   	 * @return a list of MedicalRecords; this returned list will not contain empty
   	 *         records (those that contains only date but no actual records)
   	 */
      public static List<MedicalRecord> constructMedicalRecordList(List<String> entriesReadFromTable) {
         List<MedicalRecord> medicalRecords = new ArrayList<MedicalRecord>();
      
         entriesReadFromTable.forEach(e -> medicalRecords.add(new MedicalRecord(e)));
      
         return medicalRecords.stream().filter(r -> !r.isEmpty()).toList();
      }
   
      public MedicalRecord(String entryReadFromTable) {
    	 // Entry format item 1;item 2;...;item n;date;
         this.items = new ArrayList<String>(Arrays.asList(entryReadFromTable.split(";")));
         String dateTime = items.removeLast();
         this.date = Date.fromStringDMYHM(dateTime);
         this.isNewRecord = false;
      }
   
   	/**
   	 * Removes an entry from the record.
   	 * 
   	 * @param index 0-based index of the position of the record
   	 * @return true if the record has no items left; false otherwise
   	 */
      public boolean removeRecord(int index) {
         this.items.remove(index);
         return this.isEmpty();
      }
   
      /**
   	 * Adds an entry to the record.
   	 * 
   	 * @param string of new entry
   	 */
      public void addRecord(String newEntry) {
         this.items.add(newEntry);
      }
   
		/**
		 * update an entry on the record.
		 * 
		 * @param index 0-based index of the position of the record, newValue, the new
		 *              value needed to replace the existing value
		 */
      public void updateRecord(int index, String newValue) {
         this.items.set(index, newValue);
      }
   
   	/**
   	 * A MedicalRecord object is empty if and only if the record contains
   	 * zero (0) items, which does not include the date. An empty record
   	 * may be those that only have a date.
   	 * 
   	 * @return true if this record is empty; false otherwise
   	 */
      public boolean isEmpty() {
         return this.items.isEmpty();
      }
   
      /**
       * Displays the record, including the date and items.
       */
      public void display() {
         System.out.println("	Dated " + this.date.getStringDate());
         for (String _record : this.items) {
            System.out.println("	- " + _record);
         }
      }
   
      /**
       * @Override
       * Converts the record to a semicolon-separated string format.
       *
       * @return The string representation of the record.
       */
      @Override
      public String toString() {
         if (this.isEmpty()) 
            return "";
         return String.join(";", this.items) + ";" + this.date.getStringDate() + ";";
      }
   
      /**
       * Answers whether the record associated with this MedicalRecord object is a 
       * newly created one, i.e., not yet exists in the table.
       * 
       * @return true if the record is new; false otherwise.
       */
      public boolean isNewRecord() {
         return isNewRecord;
      }
   
      /**
       * @Override
       * Compares this record with another record based on their dates.
       *
       * @param o The other record to compare with.
       * @return A negative, zero, or positive value based on the comparison.
       */
      @Override
      public int compareTo(MedicalRecord o) {
         return this.date.compareTo(o.date);
      }
   }

   /**
    * Constructs the MedicalRecordManager and initializes table handlers.
    * This is a private constructor to enforce the singleton pattern.
    *
    * @throws Exception if any of the table handlers fail to initialize.
    */
   private MedicalRecordManager() throws Exception {
      diagnosisTableHandler = new WideTableHandler("./res/diagnoses.csv", Arrays.asList(ID, DIAGNOSES));
   
      medicationTableHandler = new WideTableHandler("./res/medications.csv", Arrays.asList(ID, MEDICATIONS));
   
      treatmentTableHandler = new WideTableHandler("./res/treatments.csv", Arrays.asList(ID, TREATMENTS));
   
      medicalRecordTableHandler = new JointTableHandler(
         	Arrays.asList(diagnosisTableHandler, medicationTableHandler, treatmentTableHandler));
   
      mrmInstance = this;
   }

   /**
    * Initializes the MedicalRecordManger.
    * @return a handler to the singleton instance of MedicalRecordManager
    * @throws Exception
    */
   public static MedicalRecordManager MedicalRecordManagerInit() throws Exception {
   	// allows only a single instance per process
      if (mrmInstance == null) {
         mrmInstance = new MedicalRecordManager();
         return mrmInstance;
      }
   
      return null;
   }

   public static class Command extends HospitalResourceManager.Command {
      public Command(String action) {
         super(action);
         super.managerDescription = "Library";
      }
   
      public void invoke(String hospitalId) throws Exception {
         super.setIssuerId(hospitalId);
         if (!RoleManager.checkIdHasPermission(hospitalId, super.getCommand()))
            super.rejectCommand();
      	// The following part of the control flow is only entered when user has enough
      	// permission
      
         switch (super.getCommand()) {
            case "READ_PERSONAL_MEDICAL_RECORD" -> {
               HospitalManagementSystem.dispatchCommand(new UserManager.Command("READ_PERSONAL_PROFILE"));
               provideMedicalRecord(hospitalId);
            }
         
            case "READ_ANY_MEDICAL_RECORD" -> {
               provideMedicalRecord(promptSelectPatientId());
            }
         
            case "WRITE_ANY_MEDICAL_RECORD" -> {
               provideUpdateMedicalRecord();
            }
         
            default -> super.reportUndefinedCommand();
         }
      }
   }

   /**
    * Displays the medical record of a specified user.
    *
    * @param hospitalId The hospital ID of the user whose records are to be displayed.
    * @throws Exception If the user does not exist or records cannot be retrieved.
    */
   private static void provideMedicalRecord(String hospitalId) throws Exception {
      if (hospitalId == null)
         return;
   
      if (!medicalRecordTableHandler.isExistentIdAny(hospitalId)) {
         System.out.println("No past medical records. We look forward to more.");
         return;
      }
      
      PromptFormatter.printSeparation("Medical Record");
      Map<String, List<String>> allMedicalRecords = medicalRecordTableHandler.readRow(hospitalId);
   
      medicalRecordTableHandler.getVariableNames().forEach(
         variableName -> {
            System.out.println(variableName + " : ");
         
            List<MedicalRecord> specificMedicalRecords = MedicalRecord.constructMedicalRecordList(
               allMedicalRecords.get(variableName)
               );
         
            if (specificMedicalRecords.isEmpty()) {
               System.out.println("	<No Records Found>");
               return;
            }
            specificMedicalRecords.stream().sorted().forEachOrdered(r -> r.display());
         });
   	
      PromptFormatter.printSeparation("");
   }

   /**
    * Prompts the user to select a patient's hospital ID.
    *
    * @return The selected patient's hospital ID.
    * @throws Exception If the input is invalid or the patient does not exist.
    */
   private static String promptSelectPatientId() throws Exception {
      String patientId = new InputSession<String>(
         	"Please enter Patient's hospitalId"
         )
         // print when input is invalid
         .setOnInvalidInput("Patient not found!")
         // call back to check whether an id is valid, and if it is, whether is it of a
         // patient's.
         .setValidator(s -> UserManager.isExistentUser(s) && RoleManager.getRoleName(s).equals("Patient"))
         // end input session
         .startPrompt();
   
   	// If the patient exists, but somehow has no medical records yet, we create one.
   	// the addRow implementation for JointTableHandler handles all component tables
   	// at once
      if (!medicalRecordTableHandler.isExistentIdAny(patientId)) {
         medicalRecordTableHandler.addRow(patientId);
      }
   
      return patientId;
   }

   /**
    * Handles updating a medical record based on the provided operation (Add, Update, Delete).
    *
    * @throws Exception If the update operation fails.
    */
   private static void provideUpdateMedicalRecord() throws Exception {
      MedicalRecordModifier targetObject = HospitalManagementSystem.getParentTargetAs(
         	MedicalRecordModifier.class
         );
      String ownerId = targetObject.recordOwner();
      String variableName = targetObject.variableName();
      String newValue = targetObject.newValue();
      Date recordDate = targetObject.recordDate();
   
   	// id may not exist for first Add operations; but let's not assume that it is
   	// only for Add
      if (!medicalRecordTableHandler.isExistentId(ownerId, variableName)) {
         medicalRecordTableHandler.addRow(ownerId);
      }
   
   	// retrieve index of required entry, may be -1
      int targetRecordIndex = medicalRecordTableHandler.findIndexOfValue(
         	ownerId, 
         	variableName,
         	// find entry by date (first occurrence)
         	e -> new MedicalRecord(e).date.equals(recordDate)
         );
   
   	// if targetRecordIndex is -1, this will create an empty record initialized with only recordDate
      MedicalRecord targetRecord = new MedicalRecord(
         	ownerId, 
         	variableName, 
         	targetRecordIndex, 
         	recordDate.getStringDate()
         );
   
   	// prepare modified record
      switch (targetObject.operation()) {
         case "Add" -> {
            targetRecord.addRecord(newValue);
            if (targetRecord.isNewRecord()) {
               medicalRecordTableHandler.addValue(ownerId, variableName, targetRecord.toString());
               return;	// completed write to table
            }
         }
      
         case "Update" -> {
            targetRecord.updateRecord(targetObject.itemIndex(), newValue);
         }
      
         case "Delete" -> {
            targetRecord.removeRecord(targetObject.itemIndex());
            if (targetRecord.isEmpty()) {
               medicalRecordTableHandler.removeValue(ownerId, variableName, targetRecordIndex);
            // delete the whole cell, no more
               return;
            }
         }
      }
   
   	// finalize modification
      medicalRecordTableHandler.overwriteValueInList(
         	ownerId, 
         	variableName, 
         	targetRecordIndex,
         	targetRecord.toString()
         );
   }
}
