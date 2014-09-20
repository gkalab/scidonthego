package org.scid.android.gamelogic;

import java.util.ArrayList;

import org.scid.android.engine.PipedProcess;

import android.os.AsyncTask;
import android.util.Log;

public class AnalysisTask extends AsyncTask {
	private PipedProcess engine;
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
	private ArrayList<String> statPV = new ArrayList<String>();
	private String statCurrMove = "";
	private int statCurrMoveNr = 0;
	private int seldepth = 0;
	private int cpuload;
	private SearchListener listener = null;
	private boolean finished = false;
	private boolean shouldStop = false;

	public boolean isFinished() {
		return finished;
	}

	@Override
	protected Object doInBackground(Object... params) {
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
		this.engine = (PipedProcess) params[0];
		if (this.engine != null) {
			this.listener = (SearchListener) params[1];
			Position prevPos = (Position) params[2];
			ArrayList<Move> mList = (ArrayList<Move>) params[3];
			Position currPos = (Position) params[4];

			// If no legal moves, there is nothing to analyze
			ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos);
			moves = MoveGen.removeIllegal(currPos, moves);
			if (moves.size() == 0)
				return null;

			StringBuilder posStr = new StringBuilder();
			posStr.append("position fen ");
			posStr.append(TextIO.toFEN(prevPos));
			int nMoves = mList.size();
			if (nMoves > 0) {
				posStr.append(" moves");
				for (int i = 0; i < nMoves; i++) {
					posStr.append(" ");
					posStr.append(TextIO.moveToUCIString(mList.get(i)));
				}
			}
			engine.writeLineToProcess(posStr.toString());
			String goStr = String.format("go infinite");
			engine.writeLineToProcess(goStr);
			monitorEngine(currPos);
			finished = true;
		}
		return null;
	}

	private final void clearInfo() {
		depthModified = false;
		currMoveModified = false;
		pvModified = false;
		statsModified = false;
	}

	@Override
	protected void onCancelled() {
		clearInfo();
	}

	/**
	 * Monitor and report search info.
	 */
	private final void monitorEngine(Position pos) {
		// Monitor engine response
		clearInfo();
		boolean stopSent = false;
		boolean infoReceived = false;
		while (true) {
			while (true) {
				if (isCancelled() && !shouldStop) {
					shouldStop = true;
				}
				if (shouldStop && !stopSent && infoReceived) {
					engine.writeLineToProcess("stop");
					stopSent = true;
				}
				String s = engine.readLineFromProcess();
				if (s == null || s.length() == 0) {
					break;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				String[] tokens = tokenize(s);
				if (tokens[0].equals("info")) {
					parseInfoCmd(tokens);
					infoReceived = true;
					break;
				} else if (tokens[0].equals("bestmove")) {
					Log.d("SCID", "bestmove received. shouldStop=" + shouldStop
							+ ", stopSent=" + stopSent);
					return;
				}
			}
			if (!this.isCancelled()) {
				publishProgress(pos);
				try {
					Thread.sleep(100); // 10 GUI updates per second is enough
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/** Notify GUI about search statistics. */
	@Override
	protected void onProgressUpdate(Object... params) {
		Position pos = (Position) params[0];
		if (listener == null)
			return;
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
			ArrayList<Move> moves = new ArrayList<Move>();
			int nMoves = statPV.size();
			for (int i = 0; i < nMoves; i++)
				moves.add(TextIO.UCIstringToMove(statPV.get(i)));
			listener.notifyPV(pos, statPVDepth, statScore, statTime, statNodes,
					statNps, statIsMate, statUpperBound, statLowerBound, moves);
			pvModified = false;
		}
		if (statsModified) {
			listener.notifyStats(statNodes, statNps, statTime);
			statsModified = false;
		}
	}

	private final void parseInfoCmd(String[] tokens) {
		try {
			int nTokens = tokens.length;
			int i = 1;
			while (i < nTokens - 1) {
				String is = tokens[i++];
				if (is.equals("depth")) {
					statCurrDepth = Integer.parseInt(tokens[i++]);
					depthModified = true;
				} else if (is.equals("seldepth")) {
					seldepth = Integer.parseInt(tokens[i++]);
				} else if (is.equals("cpuload")) {
					cpuload = Integer.parseInt(tokens[i++]);
				} else if (is.equals("currmove")) {
					statCurrMove = tokens[i++];
					currMoveModified = true;
				} else if (is.equals("currmovenumber")) {
					statCurrMoveNr = Integer.parseInt(tokens[i++]);
					currMoveModified = true;
				} else if (is.equals("time")) {
					statTime = Integer.parseInt(tokens[i++]);
					statsModified = true;
				} else if (is.equals("nodes")) {
					statNodes = Integer.parseInt(tokens[i++]);
					statsModified = true;
				} else if (is.equals("nps")) {
					statNps = Integer.parseInt(tokens[i++]);
					statsModified = true;
				} else if (is.equals("pv")) {
					statPV.clear();
					while (i < nTokens && !tokens[i].equals("cpuload"))
						statPV.add(tokens[i++]);
					pvModified = true;
					statPVDepth = statCurrDepth;
				} else if (is.equals("score")) {
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
				}
			}
		} catch (NumberFormatException nfe) {
			// Ignore
		} catch (ArrayIndexOutOfBoundsException aioob) {
			// Ignore
		}
	}

	/** Convert a string to tokens by splitting at whitespace characters. */
	private final String[] tokenize(String cmdLine) {
		return cmdLine.trim().split("\\s+");
	}
}
