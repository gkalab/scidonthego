package org.scid.database;

import java.io.UnsupportedEncodingException;

import org.scid.android.Progress;

import android.util.Log;

public class DataBaseView {
	private String fileName;
	private int count; // total games in file
	private GameFilter filter;
	private GameInfo gameInfo = new GameInfo(); // TODO: remove and use JNI directly
	private int position, id; // non equal if filter is in effect

	public DataBaseView(String fileName) {
		this.fileName = fileName;
		DataBase.loadFile(fileName); // TODO: errors
		this.count = DataBase.getSize();
		this.position = -1;
		this.id = -1;
	}

	public String getFileName() {
		return fileName;
	}

	/** DataBase is static and thus can be preempted by different DataBaseView
	 * or ScidCursor. This method is used to take the database back. */
	public boolean reloadFile() {
		// TODO: remove this function once DataBase is non-static
		return DataBase.loadFile(fileName)
				&& DataBase.loadGame(id, false);
	}

	public void setFilter(GameFilter filter) {
		this.filter = filter;
		this.position = -1; // must call moveToPosition
		this.id = -1;
	}

	public GameFilter getMatchingHeaders(int filterOperation,
			String white, String black, boolean ignoreColors,
			boolean result_win_white, boolean result_draw,
			boolean result_win_black, boolean result_none,
			String event, String site,
			String ecoFrom, String ecoTo, boolean ecoNone,
			String yearFrom, String yearTo,
			String idFrom, String idTo,
			Progress progress) {
		short[] filterArray = GameFilter.getFilterArray(filter, count);
        if (!DataBase.searchHeader(white, black,
        		ignoreColors, result_win_white, result_draw,
        		result_win_black, result_none, event, site, ecoFrom,
        		ecoTo, ecoNone, yearFrom, yearTo, idFrom, idTo,
        		filterOperation, filterArray, progress)) {
        	Log.e("DBV", "error in searchHeader");
        	return null;
        }
        return new GameFilter(filterArray);
	}

	public GameFilter getMatchingBoards(int filterOperation, String fen,
			int searchType,	Progress progress) {
		short[] filterArray = GameFilter.getFilterArray(filter, count);
		if (!DataBase.searchBoard(fen, searchType, filterOperation, filterArray, progress)) {
        	Log.e("DBV", "error in searchBoard");
        	return null;
        }
		return new GameFilter(filterArray);
	}

	public GameFilter getFavorites(Progress progress) {
		return new GameFilter(DataBase.getFavorites(progress));
	}

	public int getCount() {
		if (filter != null) {
			return filter.getSize();
		} else {
			return this.count;
		}
	}

	public int getTotalGamesInFile() {
		return count;
	}

	private void setGameInfo(boolean onlyHeaders) {
		try {
			gameInfo.setEvent(getSanitizedString(DataBase.getEvent()));
			if (gameInfo.getEvent().equals("?")) {
				gameInfo.setEvent("");
			}
			gameInfo.setSite(getSanitizedString(DataBase.getSite()));
			if (gameInfo.getSite().equals("?")) {
				gameInfo.setSite("");
			}
			String date = DataBase.getDate();
			if (date.endsWith(".??.??")) {
				date = date.substring(0, date.length() - 6);
			} else if (date.endsWith(".??")) {
				date = date.substring(0, date.length() - 3);
			}
			if (date.equals("?") || date.equals("????")) {
				date = "";
			}
			gameInfo.setDate(date);
			gameInfo.setRound(getSanitizedString(DataBase.getRound()));
			if (gameInfo.getRound().equals("?")) {
				gameInfo.setRound("");
			}
			gameInfo.setWhite(getSanitizedString(DataBase.getWhite()));
			gameInfo.setBlack(getSanitizedString(DataBase.getBlack()));
			gameInfo.setResult(DataBase.getResult());
			gameInfo.setPgn(onlyHeaders
					? null
					: new String(DataBase.getPGN(), DataBase.SCID_ENCODING));
		} catch (UnsupportedEncodingException e) {
			Log.e("SCID", "Error converting byte[] to String", e);
		}
		gameInfo.setFavorite(DataBase.isFavorite());
		gameInfo.setDeleted(DataBase.isDeleted());
	}

	public GameInfo getGameInfo(){ // Do not changed the returned value!
		return gameInfo;
	}

	private String getSanitizedString(byte[] value)
			throws UnsupportedEncodingException {
		return Utf8Converter.convertToUTF8(new String(value, DataBase.SCID_ENCODING));
	}

	public boolean moveToPosition(int newPosition, boolean onlyHeaders) {
		int id = (filter != null) ? filter.getGameId(newPosition) : newPosition;
		if (id < 0 || id >= count) {
			return false;
		}
		this.id = id;
		this.position = newPosition;
		DataBase.loadGame(id, onlyHeaders); // TODO: check errors
		setGameInfo(onlyHeaders);
		if (filter != null)
			gameInfo.setCurrentPly(filter.getGamePly(position));
		return true;
	}

	public boolean moveToPosition(int newPosition) {
		return moveToPosition(newPosition, false);
	}

	public boolean moveToFirst() {
		return moveToPosition(0);
	}

	public boolean moveToLast() {
		return moveToPosition(getCount() - 1);
	}

	public int getPosition() {
		return position;
	}

	public void setDeleted(boolean value) {
		if (value != isDeleted()) {
			gameInfo.setDeleted(value);
			DataBase.setDeleted(id, value);
		}
	}

	public boolean isDeleted() {
		return gameInfo.isDeleted();
	}

	public void setFavorite(boolean value) {
		if (value != isFavorite()) {
			gameInfo.setFavorite(value);
			DataBase.setFavorite(id, value);
		}
	}

	public boolean isFavorite() {
		return gameInfo.isFavorite();
	}

	public int getGameId() {
		return id;
	}

	public String getPGN() {
		return gameInfo.getPgn();
	}

	public int getCurrentPly(){
		return gameInfo.getCurrentPly();
	}

	/* nameType is one of NAME_PLAYER, NAME_EVENT, NAME_SITE, NAME_ROUND */
	public static final int NAME_PLAYER = 0, NAME_EVENT = 1, NAME_SITE = 2, NAME_ROUND = 3;
	public int getNamesCount(int nameType) {
		return DataBase.getNamesCount(nameType);
	}
	public String getName(int nameType, int id) {
		return DataBase.getName(nameType, id);
	}
	public int[] getMatchingNames(int nameType, String prefix){
		return DataBase.getMatchingNames(nameType, prefix);
	}

}
