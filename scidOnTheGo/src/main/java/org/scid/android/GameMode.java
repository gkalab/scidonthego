package org.scid.android;

public class GameMode {
	private int mode;

	static final int REVIEW_MODE = 0;
	static final int ANALYSIS_MODE = 1;
	static final int STUDY_MODE = 2;

	GameMode(int mode) {
		this.mode = mode;
	}

	public final boolean analysisMode() {
		return mode == ANALYSIS_MODE;
	}

	public final boolean studyMode() {
		return mode == STUDY_MODE;
	}

	@Override
	public boolean equals(Object o) {
		if ((o == null) || (o.getClass() != this.getClass()))
			return false;
		GameMode other = (GameMode) o;
		return mode == other.mode;
	}

	public int getMode() {
		return mode;
	}
}
