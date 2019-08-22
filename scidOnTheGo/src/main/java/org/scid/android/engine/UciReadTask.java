package org.scid.android.engine;

import java.util.ArrayList;

import org.scid.android.gamelogic.Game;
import org.scid.android.gamelogic.Move;
import org.scid.android.gamelogic.Position;
import org.scid.android.gamelogic.SearchListener;
import org.scid.android.gamelogic.TextIO;

import android.os.AsyncTask;
import android.util.Log;

public class UciReadTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = UciReadTask.class.getSimpleName();
	private PipedProcess engineProcess;
	private boolean depthModified = false;
	private boolean currMoveModified = false;
	private boolean pvModified = false;
	private boolean statsModified = false;
	private int statCurrDepth = 0;
	private int statPVDepth = 0;
	private int statScore = 0;
	private boolean statIsMate = false;
	private boolean statUpperBound = false;
	private boolean statLowerBound = false;
	private int statTime = 0;
	private int statNodes = 0;
	private int statNps = 0;
	private ArrayList<String> statPV = new ArrayList<>();
	private String statCurrMove = "";
	private int statCurrMoveNr = 0;
	private SearchListener listener;
	private Position currentPosition;
	private Game game;

	public UciReadTask(PipedProcess engineProcess, SearchListener callback) {
		super();
		this.engineProcess = engineProcess;
		this.listener = callback;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Thread.currentThread().setName("UciReadTask");
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
		long lastPublishedTime = System.currentTimeMillis();
		while (!isCancelled()) {
			String line = engineProcess.readLineFromProcess();
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// ignore
			}
			if (line != null && line.length() > 0 && listener != null) {
				String[] tokens = tokenize(line);
				if (tokens[0].equals("info")) {
					parseInfoCmd(tokens);
				}
			}
			if (!this.isCancelled()
					&& System.currentTimeMillis() > (lastPublishedTime + 500)) {
				publishProgress();
				lastPublishedTime = System.currentTimeMillis();
				engineProcess.analyzeNext();
			}
		}
		engineProcess.writeLineToProcess("quit");
		Log.d(TAG, "UciReadTask cancelled");
		try {
			// wait 2 sec for engine to shut down
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// ignore
		}
		if (this.engineProcess != null) {
			this.engineProcess.shutDown();
		}
		Thread.currentThread().setName("UciReadTask cancelled");
		return null;
	}

	/** Notify GUI about search statistics. */
	@Override
	protected void onProgressUpdate(Void... params) {
		Position pos = currentPosition;
		if (listener == null || pos == null || game == null)
			return;
		if (pos.equals(new Position(game.currPos()))) {
			if (depthModified) {
				listener.notifyDepth(statCurrDepth);
				depthModified = false;
			}
			if (currMoveModified) {
				Move m = TextIO.UCIstringToMove(statCurrMove);
				listener.notifyCurrMove(pos, m, statCurrMoveNr);
				currMoveModified = false;
			}
			if (pvModified) {
				if (statPV.size() > 0) {
					listener.notifyPV(pos, statPVDepth, statScore, statTime,
							statNodes, statNps, statIsMate, statUpperBound,
							statLowerBound, statPV);
				}
				pvModified = false;
			}
			if (statsModified) {
				listener.notifyStats(statNodes, statNps, statTime);
				statsModified = false;
			}
		}
	}

	private void clearInfo() {
		depthModified = false;
		currMoveModified = false;
		pvModified = false;
		statsModified = false;
	}

	@Override
	protected void onCancelled() {
		clearInfo();
	}

	private void parseInfoCmd(String[] tokens) {
		try {
			int nTokens = tokens.length;
			int i = 1;
			while (i < nTokens - 1) {
				String is = tokens[i++];
				switch (is) {
					case "depth":
						statCurrDepth = Integer.parseInt(tokens[i++]);
						depthModified = true;
						break;
					case "seldepth":
					case "cpuload":
						break;
					case "currmove":
						statCurrMove = tokens[i++];
						currMoveModified = true;
						break;
					case "currmovenumber":
						statCurrMoveNr = Integer.parseInt(tokens[i++]);
						currMoveModified = true;
						break;
					case "time":
						statTime = Integer.parseInt(tokens[i++]);
						statsModified = true;
						break;
					case "nodes":
						statNodes = Integer.parseInt(tokens[i++]);
						statsModified = true;
						break;
					case "nps":
						statNps = Integer.parseInt(tokens[i++]);
						statsModified = true;
						break;
					case "pv":
						statPV.clear();
						while (i < nTokens && !tokens[i].equals("cpuload"))
							statPV.add(tokens[i++]);
						pvModified = true;
						statPVDepth = statCurrDepth;
						break;
					case "score":
						statIsMate = tokens[i++].equals("mate");
						statScore = Integer.parseInt(tokens[i++]);
						statUpperBound = false;
						statLowerBound = false;
						if (tokens[i].equals("upperbound")) {
							statUpperBound = true;
							i++;
						} else if (tokens[i].equals("lowerbound")) {
							statLowerBound = true;
							i++;
						}
						pvModified = true;
						break;
				}
			}
		} catch (NumberFormatException nfe) {
			// Ignore
		} catch (ArrayIndexOutOfBoundsException aioob) {
			// Ignore
		}
	}

	/** Convert a string to tokens by splitting at whitespace characters. */
	private String[] tokenize(String cmdLine) {
		return cmdLine.trim().split("\\s+");
	}

	public void setCurrentPosition(Game game, Position currentPosition) {
		this.game = game;
		this.currentPosition = currentPosition;
	}
}
