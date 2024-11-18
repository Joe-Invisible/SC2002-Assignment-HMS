package hms.target;

import hms.utility.Date;

/**
 * The convention record class between AppointmentManager and MedicalRecordManager
 * to communicate the change in the patients' medical records.
 */
public record MedicalRecordModifier(
		/**
		 * One of Add/Update/Delete, representing the intended 
		 * action towards a particular medical record
		 */
		String operation, 
		/**
		 * One of Diagnoses/Treatments/Medications, representing
		 * the type of medical record that will be modified.
		 */
		String variableName, 
		/**
		 * The hospitalId of the owner (patient) of this medical 
		 * record
		 */
		String recordOwner,
		/**
		 * The index position of the affected item, if applicable, 
		 * within the record.
		 */
		int itemIndex, 
		/**
		 * Date the record was made. This is the date when the appointment
		 * expires.
		 */
		Date recordDate,
		/**
		 * The new value of the item, if applicable; this field is otherwise
		 * null
		 */
		String newValue
) {  }
