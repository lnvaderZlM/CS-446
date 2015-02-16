package sos;
/**
 * Simulates a non-sharable, read-only device.
 * When read from, it generates a different number every time.
 * @author Micah, Nathan
 *
 */
public class KeyboardDevice implements Device {
	
	//generates a random number to be the device id
	private int m_id = (int) (Math.random()*100);
	/**
	 * getId
	 * getter method for m_id, the device id
	 * @return - m_id
	 */
	public int getId() {
		return m_id;
	}

    /**
     * setId
     *
     * sets the device id of this device
     *
     * @param id the new id
     */
	public void setId(int id) {
		m_id = id;
	}

	/**
	 * returns whether or not the device is sharable or not.
	 * @return false, because this device isn't sharable
	 */
	public boolean isSharable() {
		return false;
	}

	/**
	 * returns whether or not the device is available or not.
	 * @return true all the time for now.
	 */
	public boolean isAvailable() {
		return true;
	}

	/**
	 * returns whether or not the device can be read from.
	 * @return true all the time for now.
	 */
	public boolean isReadable() {
		return true;
	}

	/**
	 * returns whether or not the device can be written to.
	 * @return false all the time for now.
	 */
	public boolean isWriteable() {
		return false;
	}

    /**
     * read
     *
     * not implemented
     * 
     */
	public int read(int addr) {
		int randomNum = (int) (Math.random()*100000);
		return randomNum;
	}

    /**
     * write
     *
     * method records a request for service from the device and as such is
     * analagous to setting a value in a register on the device's controller.
     * As a result, the function does not check to make sure that the
     * device is ready for this request (that's the OS's job).
     */
	public void write(int addr, int data) {
		//shouldn't get called, since this is read-only
		System.out.println("herp");
		return;
	}

}
