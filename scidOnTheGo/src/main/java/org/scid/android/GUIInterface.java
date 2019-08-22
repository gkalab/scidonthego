package org.scid.android;

import java.util.List;

import org.scid.android.gamelogic.Move;
import org.scid.android.gamelogic.Position;

/** Interface between the gui and the ChessController. */
public interface GUIInterface {

	/** Update the displayed board position. */
	void setPosition(Position pos, String variantInfo,
					 List<Move> variantMoves);

	/** Mark square i as selected. Set to -1 to clear selection. */
	void setSelection(int sq);

	/** Set the status text. */
	void setStatusString(String str);

	/** Update the list of moves. */
	void moveListUpdated();

	/** Update the computer thinking information. */
	void setThinkingInfo(String pvStr, String bookInfo,
						 List<Move> pvMoves, List<Move> bookMoves);

	/**
	 * Ask what to promote a pawn to. Should call reportPromotePiece() when
	 * done.
	 */
	void requestPromotePiece();

	/** Run code on the GUI thread. */
	void runOnUIThread(Runnable runnable);

	/** Report that user attempted to make an invalid move. */
	void reportInvalidMove(Move m);

	/** Report remaining thinking time to GUI. */
	void setGameInformation(String white, String black, String gameNo);

	/** Report a move made that is a candidate for GUI animation. */
	void setAnimMove(Position sourcePos, Move move, boolean forward);

	ScidApplication getScidAppContext();

	void setFromSelection(int fromSq);
}
