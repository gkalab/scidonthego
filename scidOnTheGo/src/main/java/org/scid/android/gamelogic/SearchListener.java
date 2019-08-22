package org.scid.android.gamelogic;

import java.util.ArrayList;
import java.util.List;


/**
 * Used to get various search information during search
 */
public interface SearchListener {
    void notifyDepth(int depth);
    void notifyCurrMove(Position pos, Move m, int moveNr);
    void notifyPV(Position pos, int depth, int score, int time, int nodes, int nps,
                  boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<String> pv);
    void notifyStats(int nodes, int nps, int time);
	void notifyBookInfo(String bookInfo, List<Move> moveList);
}
