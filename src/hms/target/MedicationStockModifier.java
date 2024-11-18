package hms.target;

/**
 * The convention class between AppointmentManager and MedicationStockManager,
 * to communicate the changes in medication prescription statuses and 
 * potential altering of medication stock levels.
 */
public class MedicationStockModifier {
	private String medicineName;
	private int deductionAmount;
	private boolean available;

	/**
	 * Constructs a MedicationStockModifier object
	 * @param medicineName the name of the medicine to be dispensed. This need not be 
	 * those defined within ./res/medicineList.csv.
	 * @param deductionAmount the amount to dispense, if available
	 * @param available whether the medicine in question is available
	 */
	public MedicationStockModifier(String medicineName, int deductionAmount, boolean available) {
		this.medicineName = medicineName;
		this.deductionAmount = deductionAmount;
		this.available = available;
	}

	/**
	 * Gets the medicine name associated with this modifier object
	 * @return the medicine name as String
	 */
	public String getmedicineName() {
		return this.medicineName;
	}

	/**
	 * Gets the amount to dispense the said medicine
	 * @return the amount
	 */
	public int getdeductionAmount(){
		return this.deductionAmount;
	}

	/**
	 * Checks whether the medicine associated with this modifier object is 
	 * available.
	 * @return true if available; false otherwise.
	 */
	public boolean getavalibility(){
		return this.available;
	}

	/**
	 * Indicates whether the medicine associated with this modifier object is 
	 * available
	 * @param exist true if the medicine is available, false otherwise.
	 */
	public void setavalibility(Boolean exist){
		this.available = exist;
	}
	
}
