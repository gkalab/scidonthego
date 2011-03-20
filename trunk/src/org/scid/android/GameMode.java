package org.scid.android;

public class GameMode {
	private final boolean playerWhite;
	private final boolean playerBlack;
	private final boolean analysisMode;
	private final boolean studyMode;
	private int mode;

	public static final int PLAYER_WHITE = 1;
	public static final int PLAYER_BLACK = 2;
	public static final int TWO_PLAYERS = 3;
	public static final int ANALYSIS = 4;
	public static final int TWO_COMPUTERS = 5;
	public static final int STUDY_MODE = 6;

	public GameMode(int modeNr) {
		this.mode = modeNr;
		switch (modeNr) {
		case PLAYER_WHITE:
		default:
			playerWhite = true;
			playerBlack = false;
			analysisMode = false;
			studyMode = false;
			break;
		case PLAYER_BLACK:
			playerWhite = false;
			playerBlack = true;
			analysisMode = false;
			studyMode = false;
			break;
		case TWO_PLAYERS:
			playerWhite = true;
			playerBlack = true;
			analysisMode = false;
			studyMode = false;
			break;
		case ANALYSIS:
			playerWhite = true;
			playerBlack = true;
			analysisMode = true;
			studyMode = false;
			break;
		case TWO_COMPUTERS:
			playerWhite = false;
			playerBlack = false;
			analysisMode = false;
			studyMode = false;
			break;
		case STUDY_MODE:
			playerWhite = true;
			playerBlack = true;
			analysisMode = false;
			studyMode = true;
			break;
		}
	}

	public final boolean playerWhite() {
		return playerWhite;
	}

	public final boolean playerBlack() {
		return playerBlack;
	}

	public final boolean analysisMode() {
		return analysisMode;
	}

	public final boolean studyMode() {
		return studyMode;
	}

	public final boolean humansTurn(boolean whiteMove) {
		return (whiteMove ? playerWhite : playerBlack) || analysisMode;
	}

	@Override
	public boolean equals(Object o) {
		if ((o == null) || (o.getClass() != this.getClass()))
			return false;
		GameMode other = (GameMode) o;
		if (playerWhite != other.playerWhite)
			return false;
		if (playerBlack != other.playerBlack)
			return false;
		if (analysisMode != other.analysisMode)
			return false;
		if (studyMode != other.studyMode)
			return false;
		return true;
	}

	public int getMode() {
		return mode;
	}
}
