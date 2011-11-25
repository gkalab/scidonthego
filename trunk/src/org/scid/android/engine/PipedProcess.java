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
	private EngineConfig engineConfig;
	private boolean processAlive = false;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	private Process process;

	/** Start process. */
	public final void initialize(EngineConfig engineConfig) {
		if (!processAlive) {
			this.engineConfig = engineConfig;
			Log.d("SCID", "process not alive, starting " + engineConfig.getName());
			startProcess(engineConfig);
		}
	}

	public EngineConfig getEngineConfig() {
		return engineConfig;
	}

	public boolean isAlive() {
		return processAlive;
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
		if (processAlive && process != null) {
			process.destroy();
			Log.d("SCID", "uci process killed in finalize");
		}
		super.finalize();
	}

	/**
	 * Read a line from the process.
	 * 
	 * @return The line, without terminating newline characters, or empty string
	 *         if no data available, or null if I/O error.
	 * @throws IOException
	 */
	public final synchronized String readLineFromProcess() {
		String ret = null;
		try {
			ret = readFromProcess();
		} catch (IOException e) {
			Log.e("SCID", "Error reading from process");
			e.printStackTrace();
		}
		if (ret != null && ret.length() > 0) {
			// Log.d("SCID", "Engine -> GUI: " + ret);
		}
		return ret;
	}

	/**
	 * Write a line to the process. \n will be added automatically.
	 * 
	 * @throws IOException
	 */
	public final synchronized void writeLineToProcess(String data) {
		// Log.d("SCID", "GUI -> Engine: " + data);
		try {
			writeToProcess(data + "\n");
		} catch (IOException e) {
			Log.e("SCID", "Error writing to process: " + data, e);
			processAlive = false;
		}
	}

	/** Start the child process. */
	private final void startProcess(EngineConfig engineConfig) {
		ProcessBuilder builder = new ProcessBuilder(engineConfig.getExecutablePath());
		builder.redirectErrorStream(true);
		try {
			Log.d("SCID", "starting process");
			process = builder.start();
			Log.d("SCID", "getting output stream");
			OutputStream stdout = process.getOutputStream();
			Log.d("SCID", "getting input stream");
			InputStream stdin = process.getInputStream();
			Log.d("SCID", "initializing readers");
			reader = new BufferedReader(new InputStreamReader(stdin));
			writer = new BufferedWriter(new OutputStreamWriter(stdout));
			processAlive = true;
			Log.d("SCID", "process is now alive");
		} catch (IOException e) {
			Log.e("SCID", "Error initializing engine " + engineConfig.getName(), e);
		}
	}

	/**
	 * Read a line of data from the process. Return as soon as there is a full
	 * line of data to return, or when timeoutMillis milliseconds have passed.
	 * 
	 * @throws IOException
	 */
	private final String readFromProcess() throws IOException {
		String line = null;
		if (processAlive && reader != null && reader.ready()) {
			line = reader.readLine();
		}
		return line;
	}

	/**
	 * Write data to the process.
	 * 
	 * @throws IOException
	 */
	private final void writeToProcess(String data) throws IOException {
		if (processAlive && writer != null) {
			writer.write(data);
			writer.flush();
		}
	}
}
