package org.scid.database;

import java.io.UnsupportedEncodingException;

import org.scid.android.Progress;

import android.util.Log;

public class DataBaseView {
	private String fileName;
	private int count; // total games in file
	private GameFilter filter;
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

	/** returns whether the id was preserved
	 * (that is current game is present in the new filter) */
	public boolean setFilter(GameFilter filter, boolean preserveId) {
		this.filter = filter;
		boolean wasPreserved;
		if (preserveId) {
			int position = (filter == null) ? id : filter.getPosition(id);
			wasPreserved = (position >= 0);
			moveToPosition(wasPreserved ? position : 0);
		} else {
			wasPreserved = false;
		}
		return wasPreserved;
	}

	public GameFilter getMatchingHeaders(int filterOperation, SearchHeaderRequest request,
			Progress progress) {
		short[] filterArray = GameFilter.getFilterArray(filter, count);
        if (!DataBase.searchHeader(request, filterOperation, filterArray, progress)) {
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

	private static String getSanitizedString(byte[] value) {
        try {
            String s = Utf8Converter.convertToUTF8(new String(value, DataBase.SCID_ENCODING));
            return s.equals("?") ? "" : s;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
	}

	public boolean moveToPosition(int newPosition, boolean onlyHeaders) {
		int id = (filter != null) ? filter.getGameId(newPosition) : newPosition;
		if (id < 0 || id >= count) {
			return false;
		}
		this.id = id;
		this.position = newPosition;
		DataBase.loadGame(id, onlyHeaders); // TODO: check errors
		return true;
	}

	public boolean moveToPosition(int newPosition) {
		return moveToPosition(newPosition, false);
	}

	public boolean moveToId(int id) {
		return moveToPosition(filter == null ? id : filter.getPosition(id), false);
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

	/** returns negative value if id is not present */
	public int getPosition(int id) {
		return (filter == null) ? id : filter.getPosition(id);
	}

    public int getResult() { return DataBase.getResult(); }
    public int getWhiteElo() { return DataBase.getWhiteElo(); }
    public int getBlackElo() { return DataBase.getBlackElo(); }
    public String getWhite() { return getSanitizedString(DataBase.getWhite()); }
    public String getBlack() { return getSanitizedString(DataBase.getBlack()); }
    public String getEvent() { return getSanitizedString(DataBase.getEvent()); }
    public String getSite()  { return getSanitizedString(DataBase.getSite());  }
    public String getRound() { return getSanitizedString(DataBase.getRound()); }
    public String getDate() {
        String date = DataBase.getDate();
        if (date.endsWith(".??.??")) {
            date = date.substring(0, date.length() - 6);
        } else if (date.endsWith(".??")) {
            date = date.substring(0, date.length() - 3);
        }
        if (date.equals("?") || date.equals("????")) {
            date = "";
        }
        return date;
    }

	public int getId() {
		return id;
	}

	public void setDeleted(boolean value) {
		DataBase.setDeleted(value);
	}

	public boolean isDeleted() {
		return DataBase.isDeleted();
	}

	public void setFavorite(boolean value) {
		DataBase.setFavorite(value);
	}

	public boolean isFavorite() {
		return DataBase.isFavorite();
	}

	public int getGameId() {
		return id;
	}

	public String getPGN() {
		return getSanitizedString(DataBase.getPGN());
	}

	public int getCurrentPly(){
		return (filter != null) ? filter.getGamePly(position) : 0;
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
