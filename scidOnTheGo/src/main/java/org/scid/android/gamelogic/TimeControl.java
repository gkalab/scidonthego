package org.scid.android.gamelogic;

public class TimeControl {
	private long timeControl;
	private int movesPerSession;
	private long increment;

	private long whiteBaseTime;
	private long blackBaseTime;

	int currentMove;
	boolean whiteToMove;

	private long elapsed; // Accumulated elapsed time for this move.
	private long timerT0; // Time when timer started. 0 if timer is stopped.


	/** Constructor. Sets time control to "game in 5min". */
	TimeControl() {
		setTimeControl(5 * 60 * 1000, 0, 0);
		reset();
	}

	public final void reset() {
		currentMove = 1;
		whiteToMove = true;
		elapsed = 0;
		timerT0 = 0;
	}

	/** Set time control to "moves" moves in "time" milliseconds, + inc milliseconds per move. */
	final void setTimeControl(long time, int moves, long inc) {
		timeControl = time;
		movesPerSession = moves;
		increment = inc;
	}

	final void setCurrentMove(int move, boolean whiteToMove, long whiteBaseTime, long blackBaseTime) {
		currentMove = move;
		this.whiteToMove = whiteToMove;
		this.whiteBaseTime = whiteBaseTime;
		this.blackBaseTime = blackBaseTime;
		timerT0 = 0;
		elapsed = 0;
	}

	private boolean clockRunning() {
		return timerT0 != 0;
	}

	final void startTimer(long now) {
		if (!clockRunning()) {
			timerT0 = now;
		}
	}

	final void stopTimer(long now) {
		if (clockRunning()) {
			long currElapsed = now - timerT0;
			timerT0 = 0;
			if (currElapsed > 0) {
				elapsed += currElapsed;
			}
		}
	}

	/** Compute new remaining time after a move is made. */
	final int moveMade(long now) {
		stopTimer(now);
		long remaining = getRemainingTime(whiteToMove, now);
		remaining += increment;
		if (getMovesToTC() == 1) {
			remaining += timeControl;
		}
		elapsed = 0;
		return (int)remaining;
	}

	/** Get remaining time */
	private int getRemainingTime(boolean whiteToMove, long now) {
		long remaining = whiteToMove ? whiteBaseTime : blackBaseTime;
		if (whiteToMove == this.whiteToMove) { 
			remaining -= elapsed;
			if (timerT0 != 0) {
				remaining -= now - timerT0;
			}
		}
		return (int)remaining;
	}

	final int getInitialTime() {
		return (int)timeControl;
	}

	private int getMovesToTC() {
		if (movesPerSession <= 0)
			return 0;
		int nextTC = 1;
		while (nextTC <= currentMove)
			nextTC += movesPerSession;
		return nextTC - currentMove;
	}
}
