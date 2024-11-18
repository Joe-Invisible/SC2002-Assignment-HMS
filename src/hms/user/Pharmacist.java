package hms.user;

import hms.HospitalManagementSystem;
import hms.manager.AppointmentManager;
import hms.manager.MedicationStockManager;
import hms.utility.PromptFormatter;

public class Pharmacist extends User {

	@Override
	public void enterUI() throws Exception {

		boolean isLogout = false;
		while (!isLogout) {
			PromptFormatter.printSeparation("Pharmacist");
			System.out.println("-vao\tView Appointment Outcome Record");
			System.out.println("-ups\tUpdate Prescription Status");
			System.out.println("-vmi\tView Medication Inventory");
			System.out.println("-srr\tSubmit Replenishment Request");
			super.usersCommonMenu();
			PromptFormatter.printSeparation("");
			
			String option = PromptFormatter.workingDirectoryPrompt();
			
			switch (option) {
			case "-vao" -> {
				HospitalManagementSystem
						.dispatchCommand(new AppointmentManager.Command("READ_APPOINTMENT_OUTCOME"));
				break;
			}

			case "-ups" -> {
				HospitalManagementSystem
						.dispatchCommand(new AppointmentManager.Command("WRITE_PRESCRIPTION_STATUS"));
			}

			case "-vmi" -> {
				HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("READ_MEDICINE_LIST"));
			}

			case "-srr" -> {
				HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("WRITE_MEDICATION_STOCK_REPLENISHMENT_REQUEST"));
			}

			default -> {
				isLogout = super.handleCommonOptions(option);
				break;
			}

			}
		}
	}

}
