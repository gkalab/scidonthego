package org.scid.android.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.util.Log;

public class PipedProcess {
	private boolean processAlive = false;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	private Process process;

	/** Start process. */
	public final void initialize(String fileName) {
		if (!processAlive) {
			startProcess(fileName);
		}
	}

	/** Shut down process. */
	public final void shutDown() {
		if (processAlive) {
			writeLineToProcess("quit");
			if (process != null) {
				process.destroy();
				Log.d("SCID", "uci process killed");
			}
			processAlive = false;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (process != null) {
			process.destroy();
			Log.d("SCID", "uci process killed in finalize");
		}
		super.finalize();
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
	private final void startProcess(String fileName) {
		ProcessBuilder builder = new ProcessBuilder(
				"/data/data/org.scid.android/" + fileName);
		builder.redirectErrorStream(true);
		try {
			process = builder.start();
			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
			OutputStream stdin = process.getOutputStream();
			InputStream stdout = process.getInputStream();
			reader = new BufferedReader(new InputStreamReader(stdout));
			writer = new BufferedWriter(new OutputStreamWriter(stdin));
			processAlive = true;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Read a line of data from the process. Return as soon as there is a full
	 * line of data to return, or when timeoutMillis milliseconds have passed.
	 */
	private final String readFromProcess(int timeoutMillis) {
		String line = null;
		try {
			if (reader != null) {
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}

	/** Write data to the process. */
	private final void writeToProcess(String data) {
		try {
			if (writer != null) {
				writer.write(data);
				writer.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
