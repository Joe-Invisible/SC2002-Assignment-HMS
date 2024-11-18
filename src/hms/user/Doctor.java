package hms.user;

import hms.HospitalManagementSystem;
import hms.manager.MedicalRecordManager;
import hms.manager.AppointmentManager;
import hms.utility.PromptFormatter;

public class Doctor extends User {

   @Override
   public void enterUI() throws Exception {
      boolean isLogout = false;
      while (!isLogout) {
         PromptFormatter.printSeparation("Doctor");
         System.out.println("-vmr	View Patient Medical Records");
         System.out.println("-umr	Update Patient Medical Records");
         System.out.println("-vps	View Personal Schedule");
         System.out.println("-sa	Set Availability for Appointments");
         System.out.println("-ada	Accept or Decline Appointment Requests");
         System.out.println("-vua	View Upcoming Appointments");
         System.out.println("-rao	Record Appointment Outcome");
         super.usersCommonMenu();
         PromptFormatter.printSeparation("");
      	
         String option = PromptFormatter.workingDirectoryPrompt();
      	
         switch (option) {
            case "-vmr" -> {
               HospitalManagementSystem.dispatchCommand(new MedicalRecordManager.Command("READ_ANY_MEDICAL_RECORD"));
               break;
            }
         
            case "-umr" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("WRITE_ANY_MEDICAL_RECORD"));
               break;
            }
         
            case "-vps" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("READ_PERSONAL_APPOINTMENT"));
               break;
            }
         
            case "-sa" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("WRITE_PERSONAL_APPOINTMENT"));
               break;
            }
            
            case "-ada" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("WRITE_APPOINTMENT_REQUESTS"));
               break;
            }
         
            case "-vua" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("READ_UPCOMING_APPOINTMENTS"));
               break;
            }
            
            case "-rao" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("WRITE_APPOINTMENT_OUTCOME"));
               break;
            }
         
            default -> {
               isLogout = super.handleCommonOptions(option);
               break;
            }
         
         }
      }
   }

}
