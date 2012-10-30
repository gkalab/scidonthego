package org.scid.database;

import java.io.UnsupportedEncodingException;

import android.util.Log;

public class DataBaseView {
	/**
	 * Scids encoding seems to be CP1252 under Windows and under Linux
	 */
	private static final String SCID_ENCODING = "CP1252";
	private String fileName;
	private int count; // total games in file
	private Filter filter;
	private GameInfo gameInfo = new GameInfo();
	private int position, id; // non equal if filter is in effect
	private boolean reloadIndex = true;
	private int generation; // incremented on every change
	private boolean loadPGN = true;

	private DataBaseView(String fileName){
		this.fileName = fileName;
		this.count = DataBase.getSize(fileName);
		this.position = 0;
		this.id = 0;
	}

	public static DataBaseView getAll(String fileName) {
		return new DataBaseView(fileName);
	}

	public static DataBaseView getFavorites(DataBaseView dbv) {
		DataBaseView result = new DataBaseView(dbv.fileName);
		result.filter = new Filter(DataBase.getFavorites(dbv.fileName));
		return result;
	}

	public static DataBaseView getMatchingHeaders(DataBaseView dbv, int filterOperation,
			String white, String black, boolean ignoreColors,
			boolean result_win_white, boolean result_draw,
			boolean result_win_black, boolean result_none,
			String event, String site,
			String ecoFrom, String ecoTo, boolean ecoNone,
			String yearFrom, String yearTo) {
		DataBaseView result = new DataBaseView(dbv.fileName);
        result.filter = new Filter(DataBase.searchHeader(dbv.fileName,
        		white, black,
        		ignoreColors, result_win_white, result_draw,
        		result_win_black, result_none, event, site, ecoFrom,
        		ecoTo, ecoNone, yearFrom, yearTo, filterOperation, dbv.getFilterArray()));
		return result;
	}

	public static DataBaseView getMatchingBoards(DataBaseView dbv, int filterOperation,
			String fen, int searchType) {
		DataBaseView result = new DataBaseView(dbv.fileName);
		result.filter = new Filter(DataBase.searchBoard(
				dbv.fileName, fen, searchType,
				filterOperation, dbv.getFilterArray()));
		return result;
	}

	private int[] getFilterArray() {
		return (filter == null) ? new int[0] : filter.getFilter();
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

	private void setGameInfo(boolean isFavorite) {
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
			gameInfo.setPgn(loadPGN ? new String(DataBase.getPGN(),
					SCID_ENCODING) : null);
		} catch (UnsupportedEncodingException e) {
			Log.e("SCID", "Error converting byte[] to String", e);
		}
		gameInfo.setFavorite(isFavorite);
		gameInfo.setDeleted(DataBase.isDeleted());
	}

	public GameInfo getGameInfo(){ // Do not changed the returned value!
		return gameInfo;
	}

	private String getSanitizedString(byte[] value)
			throws UnsupportedEncodingException {
		return Utf8Converter.convertToUTF8(new String(value, SCID_ENCODING));
	}

	public boolean moveToPosition(int newPosition) {
		int id = (filter != null) ? filter.getGameNo(newPosition) : newPosition;
		if (id < 0 || id >= count) {
			return false;
		}
		this.id = id;
		this.position = newPosition;
		boolean onlyHeaders = !loadPGN;
		boolean isFavorite = DataBase.loadGame(fileName, id, onlyHeaders, this.reloadIndex);
		setGameInfo(isFavorite);
		if (filter != null)
			gameInfo.setCurrentPly(filter.getGamePly(position));
		reloadIndex = false;
		return true;
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

	public void setLoadPGN(boolean value) {
		loadPGN = value;
	}

	public void forceReloadIndex() {
		reloadIndex = true;
		++generation;
	}

	public int getGenerationCounter() {
		return generation;
	}

	public void setDeleted(boolean value) {
		if (value != isDeleted()) {
			gameInfo.setDeleted(value);
			DataBase.setDeleted(fileName, id, value);
			forceReloadIndex();
		}
	}

	public boolean isDeleted() {
		return gameInfo.isDeleted();
	}

	public void setFavorite(boolean value) {
		if (value != isFavorite()) {
			gameInfo.setFavorite(value);
			DataBase.setFavorite(fileName, id, value);
			forceReloadIndex();
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

}
