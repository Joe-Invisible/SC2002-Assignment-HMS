package hms.user;

import hms.HospitalManagementSystem;
import hms.manager.AppointmentManager;
import hms.manager.MedicationStockManager;
import hms.manager.UserManager;
import hms.utility.PromptFormatter;

public class Administrator extends User {
	@Override
	public void enterUI() throws Exception {
		boolean isLogout = false;
		while (!isLogout) {
			PromptFormatter.printSeparation("Administrator");
			System.out.println("-mhs	Manage Hospital Staff");
			System.out.println("-vhs	View Hospital Staff");
			System.out.println("-vad	View Appointment Details");
			System.out.println("-vms	View Medication Stock");
			System.out.println("-mms	Modify Medicatioin Stock");
			System.out.println("-rrr	Review Replenishment Request");
			System.out.println("-sd	Shutdown HMS");
			super.usersCommonMenu();
			PromptFormatter.printSeparation("");

			String option = PromptFormatter.workingDirectoryPrompt();

			switch (option) {
			case "-mhs" -> {
				HospitalManagementSystem.dispatchCommand(new UserManager.Command("WRITE_STAFF_LIST"));
				break;
			}
			
			case "-vhs" -> {
				HospitalManagementSystem.dispatchCommand(new UserManager.Command("READ_STAFF_LIST"));
				break;
			}
			
			case "-ua" -> {
				HospitalManagementSystem.dispatchCommand(new UserManager.Command("ADD_USER"));
			}

			case "-vad" -> {
				HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("READ_APPOINTMENT_OUTCOME"));
				break;
			}

			case "-vms" -> {
				HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("READ_MEDICINE_LIST"));
				break;
			}
			
			case "-mms" -> {
				HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("WRITE_MEDICINE_LIST"));
				break;
			}
			
			case "-rrr" -> {
				HospitalManagementSystem.dispatchCommand(new MedicationStockManager.Command("REVIEW_REPLENISHMENT_REQUEST"));
			}
			
			case "-sd" -> {
				System.out.println("Shutting down System...");
				PromptFormatter.clearScreen();
				System.exit(0);
			}

			default -> {
				isLogout = super.handleCommonOptions(option);
				break;
			}

			}
		}
	}
}
