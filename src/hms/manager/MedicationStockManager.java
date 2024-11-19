package hms.manager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.type.NullType;

import hms.HospitalManagementSystem;
import hms.exception.TableMismatchException;
import hms.exception.UndefinedVariableException;
import hms.exception.UserNotFoundException;
import hms.target.MedicationStockModifier;
import hms.utility.PromptFormatter;
import hms.utility.PromptFormatter.InputSession;
import hms.utility.TableHandler;
import hms.utility.TableHandler.TableQuery;

/**
 * Manages the medication stock in the hospital management system. Provides functionalities to view,
 * modify, replenish, and verify the stock of medicines.
 */
public class MedicationStockManager extends HospitalResourceManager {
   private static MedicationStockManager msmInstance = null;
   private static TableHandler medicineTableHandler;

   private static final int AUTO_REPLENISHMENT_QUANTITY = 100;
	// Table Variable
   private static final String MEDICINE_NAME = "Medicine Name";
   private static final String INITIAL_STOCK = "Initial Stock";
   private static final String LOW_STOCK_LEVEL_ALERT = "Low Stock Level Alert";
   private static final String REPLENISHMENT_REQUEST = "Replenishment Request";

   /**
    * Private constructor to initialize the MedicationStockManager.
    * Initializes the medicineTableHandler with the path to the medicine list and variable names.
    * 
    * @throws Exception if there is an error in initializing the table handler
    */
   private MedicationStockManager() throws Exception {
      medicineTableHandler = new TableHandler("./res/medicineList.csv",
         	Arrays.asList(MEDICINE_NAME, INITIAL_STOCK, LOW_STOCK_LEVEL_ALERT, REPLENISHMENT_REQUEST), 0);
   
      msmInstance = this;
   }

   /**
    * Initializes and returns the singleton instance of MedicationStockManager.
    * 
    * @return the singleton instance of MedicationStockManager, or null if already initialized.
    * @throws Exception if there is an error during initialization
    */
   public static MedicationStockManager MedicationStockManagerInit() throws Exception {
   	// allows only a single instance per process
      if (msmInstance == null) {
         msmInstance = new MedicationStockManager();
         return msmInstance;
      }
   
      return null;
   }

   /**
    * Command class to handle operations related to medication stock management.
    */
   public static class Command extends HospitalResourceManager.Command {
      public Command(String action) {
         super(action);
         super.managerDescription = "Dispensary";
      }
   
      public void invoke(String hospitalId) throws Exception {
         super.setIssuerId(hospitalId);
         if (!RoleManager.checkIdHasPermission(hospitalId, super.getCommand()))
            super.rejectCommand();
      	// The following part of the control flow is only entered when user has enough
      	// permission
      
         switch (super.getCommand()) {
            case "READ_MEDICINE_LIST" -> {
               viewMedicineList();
            }
         
            case "WRITE_MEDICINE_LIST" -> {
               promptModifyMedicineList();
            }
         
            case "WRITE_MEDICATION_STOCK_REPLENISHMENT_REQUEST" -> {
               submitReplenishmentRequest();
            }
         
            case "CHECK_FOR_MEDICINE" ->{
               verifyMedicineExistent();
            }
         
            case "UPDATE_STOCK_VALUE" ->{
               updateStockValue();
            }
         
            case "REVIEW_REPLENISHMENT_REQUEST" -> {
               promptReviewReplenishmentRequest();
            }
         
            default -> super.reportUndefinedCommand();
         }
      }
   }

   /**
    * Displays the current list of medicines and their stock details.
    */
   private static void viewMedicineList() { // argument is the doctor's id
   
      PromptFormatter.printSeparation("Medication List");
   
      List<List<String>> medicineTable = medicineTableHandler.getData();
      PromptFormatter.printTable(medicineTable, medicineTableHandler.format.getVariableNames());
      PromptFormatter.printSeparation("");
   
   }

   /**
    * Checks if a medicine exists in the current stock.
    * 
    * @param medicineId the medicine ID to check
    * @return true if the medicine exists, false otherwise
    */
   private static boolean isExistentMedicine(String medicinId) {
      return medicineTableHandler.isExistentId(medicinId);
   }
	
   private static String localGetFromListNoThrow(List<String> row, String variableName) {
      try {
         return medicineTableHandler.getFromList(row, variableName);
      } catch (Exception e) { 
         System.out.println(row + variableName);
      }
   	
      return null;
   }
	
   /**
    * Prompts for the addition of a new medicine to the inventory.
    * 
    * @throws IOException if an input/output error occurs
    * @throws TableMismatchException if the data format does not match the table structure
    */
   private static void promptAddNewMedicineType() throws IOException, TableMismatchException {
      String medicineName = new InputSession<String>("Please enter medication's Name")
         	// print when input is invalid
         	.setOnInvalidInput("Medicine already exists!")
         	// call back to check whether an id is valid, and if it is, whether is it of a
         	// patient's.
         	.setValidator(s -> !MedicationStockManager.isExistentMedicine(s))
         	// end input session
         	.startPrompt();
      if (medicineName == null) 
         return;
   	
      String initialStockLevel = new PromptFormatter.InputSession<Integer>("Please enter initial stock level for " + medicineName)
         	.setConverter(Integer::parseInt)
         	.getStringInput();
      if (initialStockLevel == null) 
         return;
   	
      medicineTableHandler.addRow(Arrays.asList(medicineName, initialStockLevel, "20", "Fulfilled"));
   }
	
   /**
    * Prompts the user to modify the medicine list, with options to add, remove, or update stock level.
    * 
    * @throws Exception if there is an error processing the modification
    */
   private static void promptModifyMedicineList() throws Exception {
      String decision = new PromptFormatter.Poll(
         	Arrays.asList("Add Medicine", "Remove Medicine", "Update Stock Level"),
         	"Select Operation"
         ).pollUntilValid().getAnswerString();
   	
      if(decision == null) return;

      if (decision.equals("Add Medicine")) {
         promptAddNewMedicineType();
         return;
      }
   	
      String medicineName = new InputSession<String>("Please enter medication's Name")
         	// print when input is invalid
         	.setOnInvalidInput("Medicine not found!")
         	// call back to check whether an id is valid, and if it is, whether is it of a
         	// patient's.
         	.setValidator(s -> MedicationStockManager.isExistentMedicine(s))
         	// end input session
         	.startPrompt();
   	
      if (medicineName == null) 
         return;
   	
      medicineTableHandler.readRow(medicineName);
   	
      PromptFormatter.printSeparation("Medicine Profile");
      PromptFormatter.getJustifiedLines(Arrays.asList(
         	PromptFormatter.formatFullColumnSubtable(
         			medicineTableHandler.format.getVariableNames(), 
         			Arrays.asList(medicineTableHandler.readRow(medicineName)),
         			medicineTableHandler
         	).getFirst().split("\n")
         )
         ).forEach(s -> System.out.println(s));
      PromptFormatter.printSeparation("");
   	
      if (decision.equals("Remove Medicine")) {
         medicineTableHandler.removeRow(medicineName);
         System.out.println("Removed " + medicineName);
         return;
      }
   	
      String newStockLevel = new PromptFormatter.InputSession<Integer>("Please enter new stock level for " + medicineName)
         	.setConverter(Integer::parseInt)
         	.getStringInput();
      if (newStockLevel == null) 
         return;
   	
      medicineTableHandler.updateVariable(medicineName, INITIAL_STOCK, newStockLevel);
   	
      System.out.println("Updated stock level of " + medicineName + " to " + newStockLevel);
   }
	
   /**
    * Prompts the user to review pending replenishment requests for medicines.
    * The user can approve or reject replenishment requests.
    * 
    * @throws Exception if there is an error during the replenishment review process
    */
   private static void promptReviewReplenishmentRequest() throws Exception {
      List<List<String>> pendingReplenishment = medicineTableHandler.new TableQuery(medicineTableHandler.ALL_COLUMNS)
         .where(REPLENISHMENT_REQUEST)
         .matches("Pending")
         .yield();
   	
      if (TableQuery.isEmptyResult(pendingReplenishment)) {
         System.out.println("<No pending replenishment requests>");
         return;
      }
   	
      PromptFormatter.Poll selectRequest = new PromptFormatter.Poll(
         	pendingReplenishment.stream().map(l -> localGetFromListNoThrow(l, MEDICINE_NAME)).toList(), 
         	pendingReplenishment.stream().map(l -> "Current stock level: " + localGetFromListNoThrow(l, INITIAL_STOCK)).toList(), 
         	"Select Replenishment Request"
         );
   	
      PromptFormatter.Poll decide = new PromptFormatter.Poll(
         	Arrays.asList("Reject", "Approve"),
         	"Select Operation"
         );
   	
      List<String> answers = new PromptFormatter.NestedPolls(Arrays.asList(selectRequest, decide)).pollNestedUntilValid().getAnswerStrings();
      if (answers == null) 
         return;
   	
      String medicineName = answers.get(0);
      String decision = answers.get(1);
   	
      if (decision.equals("Approve")) {
         int currentStock = Integer.parseInt(medicineTableHandler.readVariable(medicineName, INITIAL_STOCK));
         medicineTableHandler.updateVariable(medicineName, INITIAL_STOCK, String.valueOf(currentStock + AUTO_REPLENISHMENT_QUANTITY));
      }
   	
      medicineTableHandler.updateVariable(medicineName, REPLENISHMENT_REQUEST, decision.equals("Approve") ? "Fulfilled" : "Rejected");
   	
      System.out.println("Replenishment request " + decision + "d.");
   }
	
   /**
    * Submits a request for replenishing the stock of a specific medicine.
    * 
    * @throws IOException if an input/output error occurs
    */
   private static void submitReplenishmentRequest()
   		throws IOException, UndefinedVariableException, UserNotFoundException {
      String medicineId = new InputSession<String>("Please enter medication's Name to replenish")
         	// print when input is invalid
         	.setOnInvalidInput("Medicine not found!")
         	// call back to check whether an id is valid, and if it is, whether is it of a
         	// patient's.
         	.setValidator(s -> MedicationStockManager.isExistentMedicine(s))
         	// end input session
         	.startPrompt();
      medicineTableHandler.updateVariable(medicineId, REPLENISHMENT_REQUEST, "Pending");
      System.out.println(medicineId + "'s replenishment has been requested.");
      viewMedicineList();
   }
	
   /**
    * Verifies if a given medicine exists in the csv.
    */
   private static void verifyMedicineExistent() throws Exception {
   	
      MedicationStockModifier targetObject = HospitalManagementSystem
         	.getParentTargetAs(MedicationStockModifier.class);
   	
      String medicine = targetObject.getmedicineName();
      if (isExistentMedicine(medicine)) {
         if (Integer.parseInt(medicineTableHandler.readVariable(medicine, INITIAL_STOCK)) > targetObject
         		.getdeductionAmount()) {
            targetObject.setavalibility(true);
         } else {
            targetObject.setavalibility(false);
         }
      } else {
         targetObject.setavalibility(false);
      }
   }

   /**
    * Updates the stock value of a specified medicine in the inventory.
    * 
    * @throws IOException if an input/output error occurs
    */
   private static void updateStockValue() throws Exception {
      MedicationStockModifier targetObject = HospitalManagementSystem.getParentTargetAs(
         MedicationStockModifier.class
         );
      String medicine = targetObject.getmedicineName();
      int initialStock = Integer.parseInt(medicineTableHandler.readVariable(medicine, INITIAL_STOCK));
      int deductionQty = targetObject.getdeductionAmount();
      String newStock = String.valueOf(initialStock-deductionQty);
      medicineTableHandler.updateVariable(medicine,INITIAL_STOCK,newStock);
   }
}
