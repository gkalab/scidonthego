package org.scid.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.AbstractCursor;
import android.os.Bundle;

public class ScidCursor extends AbstractCursor {

	private static final class GameInfo {
		private String event = "";
		private String site = "";
		private String date = "";
		private String round = "";
		private String white = "";
		private String black = "";
		private String result = "";
		private String pgn = "";
		private int id = -1;
		private int currentPly = 0;

		public String toString() {
			StringBuilder info = new StringBuilder(128);
			info.append(white);
			info.append(" - ");
			info.append(black);
			if (date.length() > 0) {
				info.append(' ');
				info.append(date);
			}
			if (round.length() > 0) {
				info.append(' ');
				info.append(round);
			}
			if (event.length() > 0) {
				info.append(' ');
				info.append(event);
			}
			if (site.length() > 0) {
				info.append(' ');
				info.append(site);
			}
			info.append(' ');
			info.append(result);
			return info.toString();
		}

		public String[] getColumns() {
			List<String> resultList = new ArrayList<String>();
			resultList.add("" + id);
			resultList.add(event);
			resultList.add(site);
			resultList.add(date);
			resultList.add(round);
			resultList.add(white);
			resultList.add(black);
			resultList.add(result);
			resultList.add(pgn);
			resultList.add(this.toString());
			resultList.add("" + currentPly);
			return (String[]) resultList.toArray(new String[resultList.size()]);
		}
	}

	private int count;
	private DataBase db;
	private String fileName;
	private GameInfo gi;
	private int startPosition;
	// TODO: check for thread safety
	private static Map<String, Filter> filterMap = new HashMap<String, Filter>();

	public ScidCursor(String fileName) {
		super();
		filterMap.put(fileName, null);
		init(fileName, 0);
	}

	public ScidCursor(String fileName, int startPosition) {
		this(fileName);
		this.startPosition = startPosition;
	}

	private void init(String fileName, int startPosition) {
		this.fileName = fileName;
		this.db = new DataBase();
		this.count = db.getSize(fileName);
		this.startPosition = startPosition;
	}

	public ScidCursor(String fileName, int startPosition,
			String filterOperation, String fen, int searchType) {
		super();
		init(fileName, startPosition);
		searchBoard(fileName, filterOperation, fen, searchType);
	}

	public ScidCursor(String fileName, int startPosition, String[] selectionArgs) {
		super();
		init(fileName, startPosition);
		searchHeader(fileName, selectionArgs);
	}

	private void searchHeader(String fileName, String[] selectionArgs) {
		int[] filter = getFilter(fileName);
		int filterOp = getFilterOperation(fileName, selectionArgs[0]);
		String white = selectionArgs[1];
		String black = selectionArgs[2];
		boolean ignoreColors = Boolean.parseBoolean(selectionArgs[3]);
		boolean result_win_white = Boolean.parseBoolean(selectionArgs[4]);
		boolean result_draw = Boolean.parseBoolean(selectionArgs[5]);
		boolean result_win_black = Boolean.parseBoolean(selectionArgs[6]);
		boolean result_none = Boolean.parseBoolean(selectionArgs[7]);
		String event = selectionArgs[8];
		String site = selectionArgs[9];
		String ecoFrom = selectionArgs[10];
		String ecoTo = selectionArgs[11];

		filterMap.put(fileName, new Filter(db.searchHeader(fileName, white,
				black, ignoreColors, result_win_white, result_draw,
				result_win_black, result_none, event, site, ecoFrom, ecoTo,
				filterOp, filter)));
	}

	@Override
	public Bundle getExtras() {
		Bundle bundle = new Bundle();
		if (filterMap.get(fileName) != null) {
			bundle.putInt("filterSize", filterMap.get(fileName).getSize());
		}
		return bundle;
	}

	private void searchBoard(String fileName, String filterOperation,
			String fen, int searchType) {
		int[] filter = getFilter(fileName);
		int filterOp = getFilterOperation(fileName, filterOperation);
		filterMap.put(fileName, new Filter(db.searchBoard(fileName, fen,
				searchType, filterOp, filter)));
	}

	private int getFilterOperation(String fileName, String filterOperation) {
		int filterOp = 0;
		if (filterMap.get(fileName) != null && filterOperation != null) {
			filterOp = new Integer(filterOperation);
		}
		return filterOp;
	}

	private int[] getFilter(String fileName) {
		Filter fMap = filterMap.get(fileName);
		int[] filter;
		if (fMap == null) {
			filter = new int[0];
		} else {
			filter = fMap.getFilter();
		}
		return filter;
	}

	@Override
	public String[] getColumnNames() {
		return new String[] { "_id", ScidProviderMetaData.ScidMetaData.EVENT,
				ScidProviderMetaData.ScidMetaData.SITE,
				ScidProviderMetaData.ScidMetaData.DATE,
				ScidProviderMetaData.ScidMetaData.ROUND,
				ScidProviderMetaData.ScidMetaData.WHITE,
				ScidProviderMetaData.ScidMetaData.BLACK,
				ScidProviderMetaData.ScidMetaData.RESULT,
				ScidProviderMetaData.ScidMetaData.PGN,
				ScidProviderMetaData.ScidMetaData.SUMMARY,
				ScidProviderMetaData.ScidMetaData.CURRENT_PLY };
	}

	@Override
	public int getCount() {
		return this.count;
	}

	private void setGameInfo(int gameNo) {
		String pgn = this.db.getPGN();
		this.gi = new GameInfo();
		gi.event = this.db.getEvent();
		if (gi.event.equals("?")) {
			gi.event = "";
		}
		gi.site = this.db.getSite();
		if (gi.site.equals("?")) {
			gi.site = "";
		}
		gi.date = this.db.getDate().replaceAll("\\.\\?\\?", "");
		if (gi.date.equals("?") || gi.date.equals("????")) {
			gi.date = "";
		}
		gi.round = this.db.getRound();
		if (gi.round.equals("?")) {
			gi.round = "";
		}
		gi.white = this.db.getWhite();
		gi.black = this.db.getBlack();
		gi.result = this.db.getResult();
		gi.pgn = pgn;
		gi.id = gameNo;
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
		if (filterMap.get(fileName) != null) {
			return this.onFilterMove(oldPosition, newPosition);
		}
		int gameNo = startPosition + newPosition;
		this.db.loadGame(fileName, gameNo);
		setGameInfo(gameNo);
		return true;
	}

	private boolean onFilterMove(int oldPosition, int newPosition) {
		int gameNo = filterMap.get(fileName).getGameNo(
				startPosition + newPosition);
		if (gameNo >= 0) {
			this.db.loadGame(fileName, gameNo);
			setGameInfo(gameNo);
			gi.currentPly = filterMap.get(fileName).getGamePly(
					startPosition + newPosition);
			return true;
		}
		return false;
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
		if (this.gi != null) {
			return new Integer(this.gi.getColumns()[position]).intValue();
		}
		return 0;
	}

	@Override
	public long getLong(int position) {
		if (this.gi != null) {
			return new Long(this.gi.getColumns()[position]).longValue();
		}
		return 0;
	}

	@Override
	public short getShort(int position) {
		if (this.gi != null) {
			return new Short(this.gi.getColumns()[position]).shortValue();
		}
		return 0;
	}

	@Override
	public String getString(int position) {
		if (this.gi != null) {
			return this.gi.getColumns()[position];
		}
		return null;
	}

	@Override
	public boolean isNull(int position) {
		if (this.gi != null) {
			return "".equals(this.gi.getColumns()[position]);
		}
		return true;
	}
}
