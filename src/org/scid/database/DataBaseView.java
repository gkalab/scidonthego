package org.scid.database;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import android.database.AbstractCursor;
import android.os.Bundle;
import android.util.Log;

public class DataBaseView extends AbstractCursor {

	/**
	 * Scids encoding seems to be CP1252 under Windows and under Linux
	 */
	private static final String SCID_ENCODING = "CP1252";

	private int count;

	private String fileName;

	private GameInfo gameInfo;

	private int startPosition;

	private int[] projection;

	private boolean loadPGN = false; // True if projection contains pgn column

	private boolean reloadIndex = true;

	private Filter filter;

	private DataBaseView(String fileName){
		super();
		this.fileName = fileName;
		this.count = DataBase.getSize(fileName);
		this.startPosition = 0;
		handleProjection(null);
	}

	public static DataBaseView getAll(String fileName) {
		return new DataBaseView(fileName);
	}

	public static DataBaseView getFavorites(String fileName) {
		DataBaseView dbv = new DataBaseView(fileName);
		dbv.filter = new Filter(DataBase.getFavorites(fileName));
		return dbv;
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

	private final static String[] columns = new String[] { "_id",
			ScidProviderMetaData.ScidMetaData.EVENT,
			ScidProviderMetaData.ScidMetaData.SITE,
			ScidProviderMetaData.ScidMetaData.DATE,
			ScidProviderMetaData.ScidMetaData.ROUND,
			ScidProviderMetaData.ScidMetaData.WHITE,
			ScidProviderMetaData.ScidMetaData.BLACK,
			ScidProviderMetaData.ScidMetaData.RESULT,
			ScidProviderMetaData.ScidMetaData.PGN,
			ScidProviderMetaData.ScidMetaData.SUMMARY,
			ScidProviderMetaData.ScidMetaData.CURRENT_PLY,
			ScidProviderMetaData.ScidMetaData.DETAILS,
			ScidProviderMetaData.ScidMetaData.IS_FAVORITE,
			ScidProviderMetaData.ScidMetaData.IS_DELETED };

	private void handleProjection(String[] projection) {
		if (projection == null) {
			this.projection = new int[columns.length];
			for (int i = 0; i < columns.length; i++) {
				this.projection[i] = i;
			}
		} else {
			ArrayList<Integer> proj = new ArrayList<Integer>();
			for (String p : projection) {
				int idx = 0;
				for (int i = 0; i < columns.length; i++) {
					if (columns[i].equals(p)) {
						idx = i;
						break;
					}
				}
				proj.add(idx);
			}
			this.projection = new int[proj.size()];
			for (int i = 0; i < proj.size(); i++) {
				this.projection[i] = proj.get(i);
			}
		}
		loadPGN = false;
		for (int p : this.projection) {
			if (p == 8) {
				loadPGN = true;
				break;
			}
		}
	}

	@Override
	public Bundle getExtras() {
		Bundle bundle = new Bundle();
		if (filter != null) {
			bundle.putInt("filterSize", filter.getSize());
			bundle.putInt("count", count);
		}
		return bundle;
	}

	@Override
	public String[] getColumnNames() {
		String[] ret = new String[projection.length];
		int idx = 0;
		for (int i : projection) {
			ret[idx++] = columns[i];
		}
		return ret;
	}

	/**
	 * Return the number of games in the cursor. If the there's a current filter
	 * only return the number of games in the filter.
	 *
	 * @see android.database.AbstractCursor#getCount()
	 */
	@Override
	public int getCount() {
		if (filter != null) {
			return filter.getSize();
		} else {
			return this.count;
		}
	}

	private void setGameInfo(int gameNo, boolean isFavorite) {
		this.gameInfo = new GameInfo();
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
		gameInfo.setId(gameNo);
		gameInfo.setFavorite(isFavorite);
		gameInfo.setDeleted(DataBase.isDeleted());
	}

	private String getSanitizedString(byte[] value)
			throws UnsupportedEncodingException {
		return Utf8Converter.convertToUTF8(new String(value, SCID_ENCODING));
	}

	/**
	 * @param oldPosition
	 *            the position that we're moving from
	 * @param newPosition
	 *            the position that we're moving to
	 * @return true if the move is successful, false otherwise
	 */
	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		boolean result = true;
		if (filter != null) {
			result = this.onFilterMove(oldPosition, newPosition);
		} else {
			int gameNo = startPosition + newPosition;
			boolean onlyHeaders = !loadPGN;
			boolean isFavorite = DataBase.loadGame(fileName, gameNo,
					onlyHeaders, this.reloadIndex);
			setGameInfo(gameNo, isFavorite);
		}
		this.reloadIndex = false;
		return result;
	}

	private boolean onFilterMove(int oldPosition, int newPosition) {
		boolean result = false;
		int gameNo = filter.getGameNo(startPosition + newPosition);
		if (gameNo >= 0) {
			boolean onlyHeaders = !loadPGN;
			boolean isFavorite = DataBase.loadGame(fileName, gameNo,
					onlyHeaders, this.reloadIndex);
			setGameInfo(gameNo, isFavorite);
			gameInfo.setCurrentPly(filter.getGamePly(startPosition + newPosition));
			result = true;
		}
		return result;
	}

	@Override
	public double getDouble(int arg0) {
		return 0;
	}

	@Override
	public float getFloat(int arg0) {
		return 0;
	}

	@Override
	public int getInt(int position) {
		if (this.gameInfo != null) {
			return Integer.parseInt(this.gameInfo.getColumn(projection[position]));
		}
		return 0;
	}

	@Override
	public long getLong(int position) {
		if (this.gameInfo != null) {
			return Long.parseLong(this.gameInfo.getColumn(projection[position]));
		}
		return 0;
	}

	@Override
	public short getShort(int position) {
		if (this.gameInfo != null) {
			return Short.parseShort(this.gameInfo.getColumn(projection[position]));
		}
		return 0;
	}

	@Override
	public String getString(int position) {
		if (this.gameInfo != null) {
			int column = projection[position];
			return this.gameInfo.getColumn(column);
		}
		return null;
	}

	@Override
	public boolean isNull(int position) {
		if (this.gameInfo != null) {
			return "".equals(this.gameInfo.getColumn(projection[position]));
		}
		return true;
	}

	@Override
	public Bundle respond(Bundle extras) {
		if (extras.containsKey("loadPGN")) {
			this.loadPGN = extras.getBoolean("loadPGN");
		}
		if (extras.containsKey("reloadIndex")) {
			this.reloadIndex = extras.getBoolean("reloadIndex");
		}
		if (extras.containsKey("isDeleted")) {
			this.gameInfo.setDeleted(extras.getBoolean("isDeleted"));
		}
		if (extras.containsKey("isFavorite")) {
			this.gameInfo.setFavorite(extras.getBoolean("isFavorite"));
		}
		return null;
	}
}
