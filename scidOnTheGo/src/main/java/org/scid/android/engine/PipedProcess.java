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
	private static final String TAG = PipedProcess.class.getSimpleName();
	private EngineConfig engineConfig;
	private boolean processAlive = false;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	private Process process;
	private String nextAnalyzeCommand;

	/** Start process. */
	final void initialize(EngineConfig engineConfig) {
		if (!processAlive) {
			this.engineConfig = engineConfig;
			Log.d(TAG, "process not alive, starting " + engineConfig.getName());
			startProcess(engineConfig);
		}
	}

	public EngineConfig getEngineConfig() {
		return engineConfig;
	}

	private void writeAnalyzeCommands(String cmd) {
		writeLineToProcess("stop");
		writeLineToProcess("isready");
		writeLineToProcess(cmd);
		writeLineToProcess("go infinite");
	}

	synchronized void analyzeNext() {
		if (this.nextAnalyzeCommand != null) {
			writeAnalyzeCommands(nextAnalyzeCommand);
			this.nextAnalyzeCommand = null;
		}
	}

	/**
	 * TODO: use a delayed queue instead (e.g. DelayQueue) or FutureTask?
	 */
	public synchronized void setNextAnalyzeCommand(String nextAnalyzeCommand) {
		this.nextAnalyzeCommand = nextAnalyzeCommand;
	}

	/** Shut down process. */
	final void shutDown() {
		if (processAlive && !isTerminated()) {
			if (process != null) {
				process.destroy();
				Log.d(TAG, "uci process killed");
			}
			processAlive = false;
		}
	}

	private boolean isTerminated() {
		boolean terminated = true;
		if (process != null) {
			try {
				int exitValue = process.exitValue();
				Log.d(TAG, "exitValue=" + exitValue);
				processAlive = false;
			} catch (IllegalThreadStateException e) {
				terminated = false;
			}
		}
		return terminated;
	}

	@Override
	protected void finalize() throws Throwable {
		if (processAlive && process != null && !isTerminated()) {
			process.destroy();
			Log.d(TAG, "uci process killed in finalize");
		}
		super.finalize();
	}

	/**
	 * Read a line from the process.
	 * 
	 * @return The line, without terminating newline characters, or empty string
	 *         if no data available, or null if I/O error.
	 */
	final synchronized String readLineFromProcess() {
		String ret = null;
		try {
			ret = readFromProcess();
		} catch (IOException e) {
			Log.e(TAG, "Error reading from process");
		}
		// if (ret != null && ret.length() > 0) {
		// Log.d("SCID", "Engine -> GUI: " + ret);
		// }
		return ret;
	}

	/**
	 * Write a line to the process. \n will be added automatically.
	 */
	final synchronized void writeLineToProcess(String data) {
		// Log.d("SCID", "GUI -> Engine: " + data);
		try {
			writeToProcess(data + "\n");
		} catch (IOException e) {
			Log.e(TAG, "Error writing to process: " + data, e);
			isTerminated();
		}
	}

	/** Start the child process. */
	private void startProcess(EngineConfig engineConfig) {
		ProcessBuilder builder = new ProcessBuilder(
				engineConfig.getExecutablePath());
		builder.redirectErrorStream(true);
		try {
			Log.d(TAG, "starting process");
			process = builder.start();
			Log.d(TAG, "getting output stream");
			OutputStream stdout = process.getOutputStream();
			Log.d(TAG, "getting input stream");
			InputStream stdin = process.getInputStream();
			Log.d(TAG, "initializing readers");
			reader = new BufferedReader(new InputStreamReader(stdin));
			writer = new BufferedWriter(new OutputStreamWriter(stdout));
			processAlive = true;
			Log.d(TAG, "process is now alive");
		} catch (IOException e) {
			Log.e(TAG, "Error initializing engine " + engineConfig.getName(), e);
		}
	}

	/**
	 * Read a line of data from the process. Return as soon as there is a full
	 * line of data to return, or when timeoutMillis milliseconds have passed.
	 */
	private String readFromProcess() throws IOException {
		String line = null;
		if (processAlive && reader != null && reader.ready()) {
			line = reader.readLine();
		}
		return line;
	}

	/**
	 * Write data to the process.
	 */
	private void writeToProcess(String data) throws IOException {
		if (processAlive && writer != null) {
			writer.write(data);
			writer.flush();
		}
	}
}
