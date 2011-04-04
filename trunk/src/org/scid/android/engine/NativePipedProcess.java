package org.scid.android.engine;


public class NativePipedProcess {
	static {
		System.loadLibrary("jni");
	}

	private boolean processAlive;

	NativePipedProcess() {
		processAlive = false;
	}

	/** Start process. */
	public final void initialize(String fileName) {
		if (!processAlive) {
			startProcess(fileName);
			processAlive = true;
		}
	}

	/** Shut down process. */
	public final void shutDown() {
		if (processAlive) {
			writeLineToProcess("quit");
			processAlive = false;
		}
	}

	/**
	 * Read a line from the process.
	 * 
	 * @param timeoutMillis
	 *            Maximum time to wait for data
	 * @return The line, without terminating newline characters, or empty string
	 *         if no data available, or null if I/O error.
	 */
	public final String readLineFromProcess(int timeoutMillis) {
		String ret = readFromProcess(timeoutMillis);
		if (ret == null)
			return null;
		if (ret.length() > 0) {
			// Log.d("SCID", "Engine -> GUI: " + ret);
		}
		return ret;
	}

	/** Write a line to the process. \n will be added automatically. */
	public final synchronized void writeLineToProcess(String data) {
		// Log.d("SCID", "GUI -> Engine: " + data);
		writeToProcess(data + "\n");
	}

	/** Start the child process. */
	private final native void startProcess(String fileName);

	/**
	 * Read a line of data from the process. Return as soon as there is a full
	 * line of data to return, or when timeoutMillis milliseconds have passed.
	 */
	private final native String readFromProcess(int timeoutMillis);

	/** Write data to the process. */
	private final native void writeToProcess(String data);
}
