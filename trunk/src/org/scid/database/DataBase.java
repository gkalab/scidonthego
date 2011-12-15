package org.scid.database;

import android.util.Log;

public class DataBase {
	static {
		System.loadLibrary("jni");
	}

	/** Create a new scid database. */
	public final native void create(String fileName);

	/** 
	 * Load a game from a scid file and set it as the current game.
	 * If reloadIndex = true then reload the index file.
	 * Return true if the game is in Favorites.
	 */
	public final native boolean loadGame(String fileName, int gameNo,
			boolean onlyHeaders, boolean reloadIndex);

	/**
	 * Do a board search and return the found game numbers and plys in an int
	 * array
	 * 
	 * @param fileName
	 *            the file name to search
	 * @param fen
	 *            the FEN position to search for
	 * @param typeOfSearch
	 *            0=exact, 1=pawns, 2=files, 3=any
	 * @param filterOperation
	 *            the type of filter restriction (0=IGNORE, 1=OR, 2=AND)
	 * @param currentFilter
	 *            the current filter
	 * @return int array of found game numbers and ply the result has the format
	 *         [gameNo1, ply1, gameNo2, ply2, ...], so the result is twice as
	 *         large as the found games
	 */
	public final native int[] searchBoard(String fileName, String fen,
			int typeOfSearch, int filterOperation, int[] currentFilter);

	public final native int[] searchHeader(String fileName, String white,
			String black, boolean ignoreColors, boolean result_win_white,
			boolean result_draw, boolean result_win_black, boolean result_none,
			String event, String site, String ecoFrom, String ecoTo,
			boolean includeEcoNone, String yearFrom, String yearTo,
			int filterOperation, int[] currentFilter);

	/** Get the number of games of a scid file. */
	public final native int getSize(String fileName);

	/** Get the complete PGN of the current game. */
	public final native String getPGN();

	/** Get the move list (including the result) of the current game. */
	public final native String getMoves();

	/** Get the header [Result] of the current game. */
	public final native String getResult();

	/** Get the header [White] of the current game. */
	public final native String getWhite();

	/** Get the header [Black] of the current game. */
	public final native String getBlack();

	/** Get the header [Event] of the current game. */
	public final native String getEvent();

	/** Get the header [Site] of the current game. */
	public final native String getSite();

	/** Get the header [Date] of the current game. */
	public final native String getDate();

	/** Get the header [Round] of the current game. */
	public final native String getRound();

	public void callback(int progress) {
		Log.d("GAME", "Processed up to game number: " + progress);
	}

	/** Import a pgn file and create a scid database. */
	public final native String importPgn(String fileName);

	/** Set the favorite flag on a game. */
	public final native void setFavorite(String fileName, int gameNo,
			boolean isFavorite);
	
	/** Return true if the game is marked as favorite. */
	public final native boolean isFavorite(String fileName, int gameNo);

	/** Return the favorites as a filter. */
	public final native int[] getFavorites(String fileName);
	
	/** Save the game with the game number. */
	public final native String saveGame(String fileName, int gameNo, String pgn);
}
