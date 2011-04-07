/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.scid.android.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.scid.android.gamelogic.Move;
import org.scid.android.gamelogic.Pair;
import org.scid.android.gamelogic.Position;
import org.scid.android.gamelogic.SearchListener;

import android.util.Log;

/**
 * A computer algorithm player.
 * 
 * @author petero
 */
public class ComputerPlayer {
	public static String engineName = "";

	static PipedProcess npp = null;
	SearchListener listener;
	int timeLimit;
	Book book;
	private boolean newGame = false;

	public ComputerPlayer(String enginefileName) {
		if (npp == null) {
			npp = new PipedProcess();
			Log.d("SCID", "engine: initialize");
			npp.initialize(enginefileName);
			Log.d("SCID", "engine: write uci");
			npp.writeLineToProcess("uci");
			Log.d("SCID", "engine: read uci options");
			readUCIOptions();
			Log.d("SCID", "engine: finish read uci options");
			int nThreads = getNumCPUs();
			if (nThreads > 8)
				nThreads = 8;
			Log.d("SCID", "engine: setting options");
			npp.writeLineToProcess("setoption name Hash value 16");
			npp.writeLineToProcess("setoption name Ponder value false");
			npp.writeLineToProcess(String.format(
					"setoption name Threads value %d", nThreads));
			Log.d("SCID", "engine: writing ucinewgame");
			npp.writeLineToProcess("ucinewgame");
			syncReady();
		}
		listener = null;
		timeLimit = 0;
		book = new Book();
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

	public final void setListener(SearchListener listener) {
		this.listener = listener;
	}

	public final void setBookFileName(String bookFileName) {
		book.setBookFileName(bookFileName);
	}

	private void readUCIOptions() {
		synchronized (npp) {
			long startTime = System.currentTimeMillis();
			while (true) {
				String s = npp.readLineFromProcess();
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
					npp.writeLineToProcess("uci");
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
		synchronized (npp) {
			npp.writeLineToProcess("isready");
			Log.d("SCID", "waiting for readyok");
			long start = System.currentTimeMillis();
			while (true) {
				String s = npp.readLineFromProcess();
				if (s != null && s.equals("readyok"))
					break;
				if ((System.currentTimeMillis() - start) > 5000) {
					npp.writeLineToProcess("isready");
					start = System.currentTimeMillis();
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

	/** Clear transposition table. */
	public final void clearTT() {
		newGame = true;
	}

	public boolean isNewGame() {
		return newGame;
	}

	public PipedProcess getEngine() {
		return npp;
	}

	public final void maybeNewGame() {
		if (newGame) {
			newGame = false;
			synchronized (npp) {
				npp.writeLineToProcess("ucinewgame");
			}
			syncReady();
		}
	}

	/** Stop the engine process. */
	public final void shutdownEngine() {
		synchronized (npp) {
			if (npp != null) {
				npp.shutDown();
				npp = null;
			}
		}
	}

	public final Pair<String, ArrayList<Move>> getBookHints(Position pos) {
		Pair<String, ArrayList<Move>> bi = book.getAllBookMoves(pos);
		return new Pair<String, ArrayList<Move>>(bi.first, bi.second);
	}
}
