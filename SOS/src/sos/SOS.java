package sos;

import java.util.*;

/**
 * This class contains the simulated operating system (SOS). Realistically it
 * would run on the same processor (CPU) that it is managing but instead it uses
 * the real-world processor in order to allow a focus on the essentials of
 * operating system design using a high level programming language.
 * 
 * Authors include: Stephen Robinson and Camden McKone
 * 
 */

public class SOS implements CPU.TrapHandler {
	// ======================================================================
	// Member variables
	// ----------------------------------------------------------------------

	/**
	 * This flag causes the SOS to print lots of potentially helpful status
	 * messages
	 **/
	public static final boolean m_verbose = true;

	/**
	 * The CPU the operating system is managing.
	 **/
	private CPU m_CPU = null;

	/**
	 * The RAM attached to the CPU.
	 **/
	private RAM m_RAM = null;

	/**
	 * The devices
	 */
	private Vector<DeviceInfo> m_devices;

	/**
	 * a Vector of all the Program objects (not processes!) that are available
	 * to the operating system. While this is unrealistic, doing it this way
	 * means we can put off implementing a file system for the foreseeable
	 * future.
	 */
	Vector<Program> m_programs = new Vector<Program>();

	/**
	 * This variable contains the position where the next program will be loaded
	 * (when the createProcess method is called). Each time a process is
	 * created, the value of this m_nextLoadPos is increased by the size of that
	 * program’s address space. (In a later assignment, we’ll implement a more
	 * intelligent memory management module for SOS.)
	 */
	int m_nextLoadPos = 0;

	/**
	 * Each time a process is created it must be assigned a unique id. This
	 * variable specifies the id that will be assigned to the next process that
	 * is loaded (via the createProcess method). Each time this variable is
	 * used, it should be incremented. (Ponder: Real OSes have to do something
	 * more complex than this. What is the inherent weakness of this approach?)
	 */

	int m_nextProcessID = 1001;

	/**
	 * This is a list of all the processes that are currently loaded into RAM
	 * and in one of the major states (Ready, Running or Blocked). In other
	 * words, this is the process table for SOS.
	 */

	Vector<ProcessControlBlock> m_processes = new Vector<ProcessControlBlock>();

	/**
	 * You should already have this member variable, but now its initial value
	 * should no longer be a dummy object containing this variable as a
	 * reference to the process that is currently in the Running state. It will
	 * be important from here on out.
	 */
	ProcessControlBlock m_currProcess = null;

	// ======================================================================
	// Constants
	// ----------------------------------------------------------------------

	// These constants define the system calls this OS can currently handle
	public static final int SYSCALL_EXIT = 0; /* exit the current program */
	public static final int SYSCALL_OUTPUT = 1; /* outputs a number */
	public static final int SYSCALL_GETPID = 2; /* get current process id */
	public static final int SYSCALL_OPEN = 3; /* access a device */
	public static final int SYSCALL_CLOSE = 4; /* release a device */
	public static final int SYSCALL_READ = 5; /* get input from device */
	public static final int SYSCALL_WRITE = 6; /* send output to device */
	public static final int SYSCALL_COREDUMP = 9; /* print process state and exit */

	// The error values.
	public static final int DEVICE_NOT_FOUND = -1;
	public static final int NOT_SHAREABLE = -2;
	public static final int ALREADY_OPENED = -3;
	public static final int NOT_OPENED = -4;
	public static final int READ_ONLY = -5;
	public static final int WRITE_ONLY = -6;

	public static final int SYSCALL_EXEC = 7; /* spawn a new process */
	public static final int SYSCALL_YIELD = 8; /*
												 * yield the CPU to another
												 * process
												 */

	/*
	 * ======================================================================
	 * Constructors & Debugging
	 * ----------------------------------------------------------------------
	 */

	/**
	 * The constructor does nothing special
	 */
	public SOS(CPU c, RAM r) {
		// Init member list
		m_CPU = c;
		m_CPU.registerTrapHandler(this);
		m_RAM = r;
		m_currProcess = new ProcessControlBlock(42);
		m_devices = new Vector<DeviceInfo>(0);
	}// SOS ctor

	/**
	 * Does a System.out.print as long as m_verbose is true
	 **/
	public static void debugPrint(String s) {
		if (m_verbose) {
			System.out.print(s);
		}
	}

	/**
	 * Does a System.out.println as long as m_verbose is true
	 **/
	public static void debugPrintln(String s) {
		if (m_verbose) {
			System.out.println(s);
		}
	}

	/*
	 * ======================================================================
	 * Memory Block Management Methods
	 * ----------------------------------------------------------------------
	 */

	// None yet!

	/*
	 * ======================================================================
	 * Device Management Methods
	 * ----------------------------------------------------------------------
	 */

	/**
	 * registerDevice
	 * 
	 * adds a new device to the list of devices managed by the OS
	 * 
	 * @param dev
	 *            the device driver
	 * @param id
	 *            the id to assign to this device
	 * 
	 */
	public void registerDevice(Device dev, int id) {
		m_devices.add(new DeviceInfo(dev, id));
	}// registerDevice

	/*
	 * ======================================================================
	 * Process Management Methods
	 * ----------------------------------------------------------------------
	 */

	/**
	 * printProcessTable **DEBUGGING**
	 * 
	 * prints all the processes in the process table
	 */
	private void printProcessTable() {
		debugPrintln("");
		debugPrintln("Process Table (" + m_processes.size() + " processes)");
		debugPrintln("======================================================================");
		for (ProcessControlBlock pi : m_processes) {
			debugPrintln("    " + pi);
		}// for
		debugPrintln("----------------------------------------------------------------------");

	}// printProcessTable

	/**
	 * removeCurrentProcess
	 * 
	 * removes the current process from the process table and arrange for a new
	 * process to get assigned to be the current process
	 */
	public void removeCurrentProcess() {
		// %%%You will implement this method
		int id = m_currProcess.getProcessId();
		int base = m_CPU.getBASE();
		debugPrintln("removing process with id "+id +" at "+base);
		ProcessControlBlock toRemove = m_currProcess;
		m_processes.remove(toRemove);
		scheduleNewProcess();

		
	}// removeCurrentProcess

	/**
	 * getRandomProcess
	 * 
	 * selects a non-Blocked process at random from the ProcessTable.
	 * 
	 * @return a reference to the ProcessControlBlock struct of the selected
	 *         process -OR- null if no non-blocked process exists
	 */
	ProcessControlBlock getRandomProcess() {
		// Calculate a random offset into the m_processes list
		int offset = ((int) (Math.random() * 2147483647)) % m_processes.size();

		// Iterate until a non-blocked process is found
		ProcessControlBlock newProc = null;
		for (int i = 0; i < m_processes.size(); i++) {
			newProc = m_processes.get((i + offset) % m_processes.size());
			if (!newProc.isBlocked()) {
				return newProc;
			}
		}// for

		return null; // no processes are Ready
	}// getRandomProcess

	/**
	 * scheduleNewProcess
	 * 
	 * gets a random process to be the new current process or exits the program
	 * if there are no more processes
	 */
	public void scheduleNewProcess() {
		// %%%You will implement this method
		if(m_processes.size()==0)
		{
			System.out.println("No more processes to run. Stopping.");
			System.exit(0);
		}
		ProcessControlBlock newProcess = getRandomProcess();
		if (newProcess == null) {
			System.out.println("THIS SHOULDN'T BE HAPPENING YET");
			System.exit(0);
		}
		ProcessControlBlock old = m_currProcess;
		old.save(m_CPU);
		m_currProcess = newProcess;
		int id = m_currProcess.getProcessId();
		debugPrintln("Switched to process with id " + id);
		m_currProcess.restore(m_CPU);
		
		
	}// scheduleNewProcess

	/**
	 * addProgram
	 * 
	 * registers a new program with the simulated OS that can be used when the
	 * current process makes an Exec system call. (Normally the program is
	 * specified by the process via a filename but this is a simulation so the
	 * calling process doesn't actually care what program gets loaded.)
	 * 
	 * @param prog
	 *            the program to add
	 * 
	 */
	public void addProgram(Program prog) {
		m_programs.add(prog);
	}// addProgram

	/*
	 * ======================================================================
	 * Program Management Methods
	 * ----------------------------------------------------------------------
	 */

	/**
	 * createProcess
	 * 
	 * Creates one process for the CPU.
	 * 
	 * @param prog
	 *            The program class to be loaded into memory.
	 * @param allocSize
	 *            The amount of memory to allocate for the program.
	 */
	public void createProcess(Program prog, int allocSize) {
		//final int base = 4; // This is just an arbitrary base, hardcoded for now
		//int size = prog.getSize();

		if (m_nextLoadPos + allocSize >= m_RAM.getSize()) {
			System.out.println("NO RAM");
			System.exit(0);
			return;
		}
		if(m_currProcess!=null)
		{
			m_currProcess.save(m_CPU);
		}

		m_CPU.setBASE(m_nextLoadPos);
		m_CPU.setLIM(m_nextLoadPos + allocSize);
		m_CPU.setPC(4); // We are going to use a logical (not physical) PC
		m_CPU.setSP(allocSize);

		int[] progArray = prog.export();

		for (int progAddr = 0; progAddr < progArray.length; ++progAddr) {
			int loc = m_CPU.getBASE() + progAddr+4;//m_nextLoadPos;
			m_RAM.write(loc, progArray[progAddr]);
		}
		debugPrintln("Installed program of size " + allocSize
				+ " at position " + m_nextLoadPos);
		m_nextLoadPos = m_nextLoadPos+ allocSize;
		ProcessControlBlock newProcess = new ProcessControlBlock(
				m_nextProcessID);
		m_nextProcessID++;
		m_processes.add(newProcess);
		m_currProcess = newProcess;
		printProcessTable();

		//m_CPU.setSP(allocSize); // Stack starts at the bottom and grows up.

	}// createProcess

	/*
	 * ======================================================================
	 * Interrupt Handlers
	 * ----------------------------------------------------------------------
	 */

	/**
	 * interruptIllegalMemoryAccess
	 * 
	 * Handles Illegal Memory Access interrupts.
	 * 
	 * @param addr
	 *            The address which was attempted to be accessed
	 */
	public void interruptIllegalMemoryAccess(int addr) {
		System.out.println("Error: Illegal Memory Access at addr " + addr);
		System.out.println("NOW YOU DIE!!!");
		System.exit(0);
	}

	/**
	 * interruptDivideByZero
	 * 
	 * Handles Divide by Zero interrupts.
	 */
	public void interruptDivideByZero() {
		System.out.println("Error: Divide by Zero");
		System.out.println("NOW YOU DIE!!!");
		System.exit(0);
	}

	/**
	 * interruptIllegalInstruction
	 * 
	 * Handles Illegal Instruction interrupts.
	 * 
	 * @param instr
	 *            The instruction which caused the interrupt
	 */
	public void interruptIllegalInstruction(int[] instr) {
		System.out.println("Error: Illegal Instruction:");
		System.out.println(instr[0] + ", " + instr[1] + ", " + instr[2] + ", "
				+ instr[3]);
		System.out.println("NOW YOU DIE!!!");
		System.exit(0);
	}

	/*
	 * ======================================================================
	 * System Calls
	 * ----------------------------------------------------------------------
	 */

	/**
	 * syscallExit
	 * 
	 * Exits from the current process.
	 */
	private void syscallExit() {
		removeCurrentProcess();
		
	}

	/**
	 * syscallOutput
	 * 
	 * Outputs the top number from the stack.
	 */
	private void syscallOutput() {
		System.out.println("OUTPUT: " + m_CPU.popStack());
	}

	/**
	 * syscallGetPID
	 * 
	 * Pushes the PID to the stack.
	 */
	private void syscallGetPID() {
		int pid = m_currProcess.getProcessId(); // just for now we will use a
												// constant pid.
		m_CPU.pushStack(pid);
	}

	/**
	 * syscallCoreDump
	 * 
	 * Prints the registers and top three stack items, then exits the process.
	 */
	private void syscallCoreDump() {

		System.out.println("\n\nCORE DUMP!");

		m_CPU.regDump();

		System.out.println("Top three stack items:");
		for (int i = 0; i < 3; ++i) {
			if (m_CPU.validMemory(m_CPU.getSP() + 1 + m_CPU.getBASE())) {
				System.out.println(m_CPU.popStack());
			} else {
				System.out.println(" -- NULL -- ");
			}
		}
		syscallExit();
	}

	/**
	 * Pops the device number off the calling process' stack and retrieves the
	 * associated DeviceInfo object via that device number, then indicates that
	 * the process is currently using the device.
	 */
	public void syscallOpen() {
		// Retrive deviceInfo from stack
		int deviceNumber = m_CPU.popStack();
		if (deviceFound(deviceNumber) == null) {
			m_CPU.pushStack(DEVICE_NOT_FOUND);
			return;
		}
		DeviceInfo info = deviceFound(deviceNumber);
		boolean currentlyUsed = info.containsProcess(m_currProcess);
		// if the device is currently being used, we can't open it.
		if (currentlyUsed) {
			m_CPU.pushStack(ALREADY_OPENED);
			return;
		}
		Device dev = info.getDevice();
		boolean currentlyUsedByOther = !info.unused();
		boolean notSharable = !dev.isSharable();
		if (notSharable && currentlyUsedByOther) {
			System.out.println("blocking");
			m_currProcess.block(m_CPU, dev, SYSCALL_OPEN, 100);
		}


		// Indicate that the process is currently using the device
		info.addProcess(m_currProcess);

		// Success
		m_CPU.pushStack(0);

	}

	/**
	 * Removes device from process
	 */
	public void syscallClose() {

		// Retrieve associated device info, and unassign device to a process
		int deviceNumber = m_CPU.popStack();
		if (deviceFound(deviceNumber) == null) {
			m_CPU.pushStack(DEVICE_NOT_FOUND);
			return;
		}
		DeviceInfo info = deviceFound(deviceNumber);
		// check if the device is opened. if it isn't, we can't close it.
		if (!info.containsProcess(m_currProcess)) {
			m_CPU.pushStack(NOT_OPENED);
			return;
		}
		// remove the device from the process
		info.removeProcess(m_currProcess);
		Device dev = info.getDevice();
		while (true) {
			ProcessControlBlock toUnblock = selectBlockedProcess(dev,
					SYSCALL_OPEN, 100);
			if (toUnblock == null) {
				break;
			}
			int id = toUnblock.getProcessId();
			System.out.println("Moving process with id "+ id + " from blocked to ready state.");
			toUnblock.unblock();
		}
		// Close operation has completed successfully.
		// 0 signifies successful completion
		m_CPU.pushStack(0);
	}

	/**
	 * Reads from a device
	 */
	public void syscallRead() {
		// pop data for syscall
		int address = m_CPU.popStack();
		int deviceInfo = m_CPU.popStack();
		// read data from device and push to CPU's stack
		// if (!deviceFound(deviceInfo)) {
		// return;
		// }
		if (deviceFound(deviceInfo) == null) {
			m_CPU.pushStack(DEVICE_NOT_FOUND);
			return;
		}
		DeviceInfo info = deviceFound(deviceInfo);
		// check if the device has been opened. if not, we can't read it.
		if (!info.containsProcess(m_currProcess)) {
			m_CPU.pushStack(NOT_OPENED);
			return;
		}
		Device dev = info.getDevice();
		// check if the device is write-only. if it is, we can't read it.
		if (!dev.isReadable()) {
			m_CPU.pushStack(WRITE_ONLY);
		}
		int data = dev.read(address);

		// write data
		m_CPU.pushStack(data);

		// return success
		m_CPU.pushStack(0);
	}

	/**
	 * Writes to a device
	 */
	public void syscallWrite() {
		// pop arguments off stack
		int data = m_CPU.popStack();
		int address = m_CPU.popStack();
		int deviceInfo = m_CPU.popStack();
		// if (!deviceFound(deviceInfo)) {
		// return;
		// }
		// retrieve device information, get the device, and write.
		if (deviceFound(deviceInfo) == null) {
			m_CPU.pushStack(DEVICE_NOT_FOUND);
			return;
		}
		DeviceInfo info = deviceFound(deviceInfo);
		// check if the device is currently open. if it isn't, we can't write to
		// it.
		if (!info.containsProcess(m_currProcess)) {
			m_CPU.pushStack(NOT_OPENED);
			return;
		}
		Device dev = info.getDevice();
		// check if the device is read-only. if it is, we can't write to it.
		if (!dev.isWriteable()) {
			m_CPU.pushStack(READ_ONLY);
			return;
		}
		info.getDevice().write(address, data);

		// return success
		m_CPU.pushStack(0);
	}

	/**
	 * syscallExec
	 * 
	 * creates a new process. The program used to create that process is chosen
	 * semi-randomly from all the programs that have been registered with the OS
	 * via {@link #addProgram}. Limits are put into place to ensure that each
	 * process is run an equal number of times. If no programs have been
	 * registered then the simulation is aborted with a fatal error.
	 * 
	 */
	private void syscallExec() {
		// If there is nothing to run, abort. This should never happen.
		if (m_programs.size() == 0) {
			System.err.println("ERROR!  syscallExec has no programs to run.");
			System.exit(-1);
		}

		// find out which program has been called the least and record how many
		// times it has been called
		int leastCallCount = m_programs.get(0).callCount;
		for (Program prog : m_programs) {
			if (prog.callCount < leastCallCount) {
				leastCallCount = prog.callCount;
			}
		}

		// Create a vector of all programs that have been called the least
		// number
		// of times
		Vector<Program> cands = new Vector<Program>();
		for (Program prog : m_programs) {
			cands.add(prog);
		}

		// Select a random program from the candidates list
		Random rand = new Random();
		int pn = rand.nextInt(m_programs.size());
		Program prog = cands.get(pn);

		// Determine the address space size using the default if available.
		// Otherwise, use a multiple of the program size.
		int allocSize = prog.getDefaultAllocSize();
		if (allocSize <= 0) {
			allocSize = prog.getSize() * 2;
		}

		// Load the program into RAM
		createProcess(prog, allocSize);

		// Adjust the PC since it's about to be incremented by the CPU
		m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);

	}// syscallExec

	/**
	 * sysCallYield
	 * 
	 * moves the current process from the running state to the ready state
	 */
	private void syscallYield() {
		scheduleNewProcess();
		
	}// syscallYield

	/**
	 * selectBlockedProcess
	 * 
	 * select a process to unblock that might be waiting to perform a given
	 * action on a given device. This is a helper method for system calls and
	 * interrupts that deal with devices.
	 * 
	 * @param dev
	 *            the Device that the process must be waiting for
	 * @param op
	 *            the operation that the process wants to perform on the device.
	 *            Use the SYSCALL constants for this value.
	 * @param addr
	 *            the address the process is reading from. If the operation is a
	 *            Write or Open then this value can be anything
	 * 
	 * @return the process to unblock -OR- null if none match the given criteria
	 */
	public ProcessControlBlock selectBlockedProcess(Device dev, int op, int addr) {
		ProcessControlBlock selected = null;
		for (ProcessControlBlock pi : m_processes) {
			if (pi.isBlockedForDevice(dev, op, addr)) {
				selected = pi;
				break;
			}
		}// for

		return selected;
	}// selectBlockedProcess

	/**
	 * systemCall
	 * 
	 * Occurs when TRAP is encountered in child process.
	 */
	public void systemCall() {
		switch (m_CPU.popStack()) {
		case SYSCALL_EXIT:
			syscallExit();
			break;
		case SYSCALL_OUTPUT:
			syscallOutput();
			break;
		case SYSCALL_GETPID:
			syscallGetPID();
			break;
		case SYSCALL_COREDUMP:
			syscallCoreDump();
			break;

		case SYSCALL_OPEN:
			syscallOpen();
			break;
		case SYSCALL_CLOSE:
			syscallClose();
			break;
		case SYSCALL_READ:
			syscallRead();
			break;
		case SYSCALL_WRITE:
			syscallWrite();
			break;

		case SYSCALL_EXEC:
			syscallExec();
			break;

		case SYSCALL_YIELD:
			syscallYield();
			break;
		}
	}

	/*
	 * A helper method to find out whether the device ID is a valid one.
	 * 
	 * @param idx - the device number to look for in m_devices
	 * 
	 * @return - false if the idx can't possibly be in m_devices, true otherwise
	 */
	public DeviceInfo deviceFound(int idx) {
		for (DeviceInfo info : m_devices) {
			if (info != null) {
				if (info.getId() == idx) {
					return info;
				}
			}
		}
		return null;
	}

	// ===========================================================
	// INNER CLASSES
	// ===========================================================
	/**
	 * class ProcessControlBlock
	 * 
	 * This class contains information about a currently active process.
	 */
	private class ProcessControlBlock {
		/**
		 * a unique id for this process
		 */
		private int processId = 0;

		/**
		 * These are the process' current registers. If the process is in the
		 * "running" state then these are out of date
		 */
		private int[] registers = null;

		/**
		 * If this process is blocked a reference to the Device is stored here
		 */
		private Device blockedForDevice = null;

		/**
		 * If this process is blocked a reference to the type of I/O operation
		 * is stored here (use the SYSCALL constants defined in SOS)
		 */
		private int blockedForOperation = -1;

		/**
		 * If this process is blocked reading from a device, the requested
		 * address is stored here.
		 */
		private int blockedForAddr = -1;

		/**
		 * constructor
		 * 
		 * @param pid
		 *            a process id for the process. The caller is responsible
		 *            for making sure it is unique.
		 */
		public ProcessControlBlock(int pid) {
			this.processId = pid;
		}

		/**
		 * @return the current process' id
		 */
		public int getProcessId() {
			return this.processId;
		}

		/**
		 * save
		 * 
		 * saves the current CPU registers into this.registers
		 * 
		 * @param cpu
		 *            the CPU object to save the values from
		 */
		public void save(CPU cpu) {
			int[] regs = cpu.getRegisters();
			this.registers = new int[CPU.NUMREG];
			for (int i = 0; i < CPU.NUMREG; i++) {
				this.registers[i] = regs[i];
			}
		}// save

		/**
		 * restore
		 * 
		 * restores the saved values in this.registers to the current CPU's
		 * registers
		 * 
		 * @param cpu
		 *            the CPU object to restore the values to
		 */
		public void restore(CPU cpu) {
			int[] regs = cpu.getRegisters();
			for (int i = 0; i < CPU.NUMREG; i++) {
				regs[i] = this.registers[i];
			}

		}// restore

		/**
		 * block
		 * 
		 * blocks the current process to wait for I/O. The caller is responsible
		 * for calling {@link CPU#scheduleNewProcess} after calling this method.
		 * 
		 * @param cpu
		 *            the CPU that the process is running on
		 * @param dev
		 *            the Device that the process must wait for
		 * @param op
		 *            the operation that the process is performing on the
		 *            device. Use the SYSCALL constants for this value.
		 * @param addr
		 *            the address the process is reading from (for SYSCALL_READ)
		 * 
		 */
		public void block(CPU cpu, Device dev, int op, int addr) {
			blockedForDevice = dev;
			blockedForOperation = op;
			blockedForAddr = addr;

		}// block

		/**
		 * unblock
		 * 
		 * moves this process from the Blocked (waiting) state to the Ready
		 * state.
		 * 
		 */
		public void unblock() {
			blockedForDevice = null;
			blockedForOperation = -1;
			blockedForAddr = -1;

		}// block

		/**
		 * isBlocked
		 * 
		 * @return true if the process is blocked
		 */
		public boolean isBlocked() {
			return (blockedForDevice != null);
		}// isBlocked

		/**
		 * isBlockedForDevice
		 * 
		 * Checks to see if the process is blocked for the given device,
		 * operation and address. If the operation is not an open, the given
		 * address is ignored.
		 * 
		 * @param dev
		 *            check to see if the process is waiting for this device
		 * @param op
		 *            check to see if the process is waiting for this operation
		 * @param addr
		 *            check to see if the process is reading from this address
		 * 
		 * @return true if the process is blocked by the given parameters
		 */
		public boolean isBlockedForDevice(Device dev, int op, int addr) {
			if ((blockedForDevice == dev) && (blockedForOperation == op)) {
				if (op == SYSCALL_OPEN) {
					return true;
				}

				if (addr == blockedForAddr) {
					return true;
				}
			}// if

			return false;
		}// isBlockedForDevice

		/**
		 * toString **DEBUGGING**
		 * 
		 * @return a string representation of this class
		 */
		public String toString() {
			String result = "Process id " + processId + " ";
			if (isBlocked()) {
				result = result + "is BLOCKED: ";
			} else if (this == m_currProcess) {
				result = result + "is RUNNING: ";
			} else {
				result = result + "is READY: ";
			}

			if (registers == null) {
				result = result + "<never saved>";
				return result;
			}

			for (int i = 0; i < CPU.NUMGENREG; i++) {
				result = result + ("r" + i + "=" + registers[i] + " ");
			}// for
			result = result + ("PC=" + registers[CPU.PC] + " ");
			result = result + ("SP=" + registers[CPU.SP] + " ");
			result = result + ("BASE=" + registers[CPU.BASE] + " ");
			result = result + ("LIM=" + registers[CPU.LIM] + " ");

			return result;
		}// toString

		/**
		 * compareTo
		 * 
		 * compares this to another ProcessControlBlock object based on the BASE
		 * addr register. Read about Java's Collections class for info on how
		 * this method can be quite useful to you.
		 */
		public int compareTo(ProcessControlBlock pi) {
			return this.registers[CPU.BASE] - pi.registers[CPU.BASE];
		}

	}// class ProcessControlBlock

	/**
	 * class DeviceInfo
	 * 
	 * This class contains information about a device that is currently
	 * registered with the system.
	 */
	private class DeviceInfo {
		/** every device has a unique id */
		private int id;
		/** a reference to the device driver for this device */
		private Device device;
		/** a list of processes that have opened this device */
		private Vector<ProcessControlBlock> procs;

		/**
		 * constructor
		 * 
		 * @param d
		 *            a reference to the device driver for this device
		 * @param initID
		 *            the id for this device. The caller is responsible for
		 *            guaranteeing that this is a unique id.
		 */
		public DeviceInfo(Device d, int initID) {
			this.id = initID;
			this.device = d;
			d.setId(initID);
			this.procs = new Vector<ProcessControlBlock>();
		}

		/** @return the device's id */
		public int getId() {
			return this.id;
		}

		/** @return this device's driver */
		public Device getDevice() {
			return this.device;
		}

		/** Register a new process as having opened this device */
		public void addProcess(ProcessControlBlock pi) {
			procs.add(pi);
		}

		/** Register a process as having closed this device */
		public void removeProcess(ProcessControlBlock pi) {
			procs.remove(pi);
		}

		/** Does the given process currently have this device opened? */
		public boolean containsProcess(ProcessControlBlock pi) {
			return procs.contains(pi);
		}

		/** Is this device currently not opened by any process? */
		public boolean unused() {
			return procs.size() == 0;
		}

	}// class DeviceInfo

	/*
	 * ======================================================================
	 * Device Management Methods
	 * ----------------------------------------------------------------------
	 */

};// class SOS
