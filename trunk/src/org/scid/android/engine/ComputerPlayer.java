/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.scid.android.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.util.Log;

/**
 * A computer algorithm player.
 * 
 * @author petero
 */
public class ComputerPlayer {
	public static String engineName = "";

	static PipedProcess process = null;
	int timeLimit;

	@SuppressLint("NewApi")
	public ComputerPlayer(EngineConfig engineConfig) {
		process = new PipedProcess();
		Log.d("SCID", "engine: initialize");
		process.initialize(engineConfig);
		Log.d("SCID", "engine: write uci");
		process.writeLineToProcess("uci");
		Log.d("SCID", "engine: read uci options");
		readUCIOptions();
		Log.d("SCID", "engine: finish read uci options");
		Log.d("SCID", "engine: setting options");
		//process.writeLineToProcess("setoption name Hash value 16");
		process.writeLineToProcess("setoption name Ponder value false");
		// disable multiple threads for now: internal engine (Stockfish 5)
		// crashes
		// int nThreads = getNumCPUs();
		// if (nThreads > 8)
		// nThreads = 8;
		// process.writeLineToProcess(String.format("setoption name Threads value %d",
		// nThreads));
		Log.d("SCID", "engine: writing ucinewgame");
		process.writeLineToProcess("ucinewgame");
		syncReady();
		timeLimit = 0;
	}

	private static int getNumCPUs() {
		try {
			FileReader fr = new FileReader("/proc/stat");
			BufferedReader inBuf = new BufferedReader(fr);
			String line;
			int nCPUs = 0;
			while ((line = inBuf.readLine()) != null) {
				if ((line.length() >= 4) && line.startsWith("cpu")
						&& Character.isDigit(line.charAt(3)))
					nCPUs++;
			}
			inBuf.close();
			if (nCPUs < 1)
				nCPUs = 1;
			return nCPUs;
		} catch (IOException e) {
			return 1;
		}
	}

	private void readUCIOptions() {
		synchronized (process) {
			long startTime = System.currentTimeMillis();
			while (true) {
				String s = process.readLineFromProcess();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// do nothing
				}
				if (s != null && s.length() > 0) {
					Log.d("SCID", "read UCI option: " + s);
					String[] tokens = tokenize(s);
					if (tokens[0].equals("uciok") || tokens[0].equals("info"))
						break;
					else if (tokens[0].equals("id")) {
						if (tokens[1].equals("name")) {
							engineName = "";
							for (int i = 2; i < tokens.length; i++) {
								if (engineName.length() > 0)
									engineName += " ";
								engineName += tokens[i];
							}
						}
					}
				} else if (System.currentTimeMillis() - startTime > 15000) {
					// no reaction from uci engine --> retry uci command
					process.writeLineToProcess("uci");
					startTime = System.currentTimeMillis();
				}
			}
		}
	}

	/** Convert a string to tokens by splitting at whitespace characters. */
	private final String[] tokenize(String cmdLine) {
		cmdLine = cmdLine.trim();
		return cmdLine.split("\\s+");
	}

	private final void syncReady() {
		synchronized (process) {
			process.writeLineToProcess("isready");
			Log.d("SCID", "waiting for readyok");
			long start = System.currentTimeMillis();
			int retries = 3;
			while (retries > 0) {
				String s = process.readLineFromProcess();
				if (s != null && s.equals("readyok"))
					break;
				if ((System.currentTimeMillis() - start) > 5000) {
					Log.i("SCID", "no reaction from engine - retrying...");
					process.writeLineToProcess("isready");
					start = System.currentTimeMillis();
					retries--;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}
		Log.d("SCID", "readyok received");
	}

	public PipedProcess getEngine() {
		return process;
	}

	/** Stop the engine process. */
	public final void shutdownEngine() {
		synchronized (process) {
			if (process != null) {
				process.writeLineToProcess("quit");
				try {
					// wait 2 sec for engine to shut down
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// ignore
				}
				if (process != null) {
					process.shutDown();
				}
				process = null;
			}
		}
	}
}
