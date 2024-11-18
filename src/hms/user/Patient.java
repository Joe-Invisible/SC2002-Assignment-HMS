package hms.user;

import hms.HospitalManagementSystem;
import hms.manager.AppointmentManager;
import hms.manager.MedicalRecordManager;
import hms.utility.PromptFormatter;

public class Patient extends User {

   @Override
   public void enterUI() throws Exception {
   
      boolean isLogout = false;
      while (!isLogout) {
         PromptFormatter.printSeparation("Patient");
         System.out.println("-vmr\tView Medical Records");
         System.out.println("-vaa\tView Available Appointment Slots");
         System.out.println("-sa\tSchedule an Appointment");
         System.out.println("-ra\tReschedule an Appointment");
         System.out.println("-ca\tCancel an Appointment");
         System.out.println("-vsa\tView Scheduled Appointments");
         System.out.println("-vpa\tView Past Appointment Outcome Records");
         super.usersCommonMenu();
         PromptFormatter.printSeparation("");
         String option = PromptFormatter.workingDirectoryPrompt();
      	
         switch (option) {
            case "-vaa" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("READ_AVAILABLE_APPOINTMENT"));
            }
            
            case "-vmr" -> {
               HospitalManagementSystem.dispatchCommand(new MedicalRecordManager.Command("READ_PERSONAL_MEDICAL_RECORD"));
            }
            
            case "-sa" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("PATIENT_SCHEDULE_APPOINTMENT"));
            }
            
            case "-vsa" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("VIEW_PATIENT_APPOINTMENT"));
            }
            
            case "-vpa" -> {
            	HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("READ_PERSONAL_APPOINTMENT_OUTCOME"));
            }
            
            case "-ra" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("RESCHEDULE_PATIENT_APPOINTMENT"));
            }
            
            case "-ca" -> {
               HospitalManagementSystem.dispatchCommand(new AppointmentManager.Command("CANCEL_PATIENT_APPOINTMENT"));
            }
         
            default -> {
               isLogout = super.handleCommonOptions(option);
            }
         
         }
      }
   }
}