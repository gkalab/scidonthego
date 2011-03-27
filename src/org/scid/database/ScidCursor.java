package org.scid.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.database.AbstractCursor;
import android.os.Bundle;

public class ScidCursor extends AbstractCursor {

	private int count;

	private DataBase db;

	private String fileName;

	private GameInfo gameInfo;

	private int startPosition;

	private int[] projection;

	private boolean loadPGN = false; // True if projection contains pgn column

	private boolean singleGame = false;

	// TODO: check for thread safety
	private static Map<String, Filter> filterMap = new HashMap<String, Filter>();

	public ScidCursor(String fileName, String[] projection, boolean singleGame) {
		super();
		filterMap.put(fileName, null);
		this.singleGame = singleGame;
		init(fileName, projection, 0);
	}

	public ScidCursor(String fileName, String[] projection, int startPosition,
			boolean singleGame) {
		this(fileName, projection, singleGame);
		this.startPosition = startPosition;
		handleProjection(projection);
	}

	public ScidCursor(String fileName, String[] projection) {
		super();
		this.singleGame = false;
		this.startPosition = 0;
		init(fileName, projection, startPosition);
		getFavorites(fileName);
	}

	public ScidCursor(String fileName, String[] projection, int startPosition,
			String[] selectionArgs, boolean singleGame) {
		super();
		this.singleGame = singleGame;
		init(fileName, projection, startPosition);
		searchHeader(fileName, selectionArgs);
	}

	public ScidCursor(String fileName, String[] projection, int startPosition,
			String filterOperation, String fen, int searchType,
			boolean singleGame) {
		super();
		this.singleGame = singleGame;
		init(fileName, projection, startPosition);
		searchBoard(fileName, filterOperation, fen, searchType);
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
			ScidProviderMetaData.ScidMetaData.IS_FAVORITE };

	private void init(String fileName, String[] projection, int startPosition) {
		this.fileName = fileName;
		this.db = new DataBase();
		this.count = singleGame ? 1 : db.getSize(fileName);
		this.startPosition = startPosition;
		handleProjection(projection);
	}

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
		boolean ecoNone = Boolean.parseBoolean(selectionArgs[12]);
		String yearFrom = selectionArgs[13];
		String yearTo = selectionArgs[14];

		filterMap.put(fileName, new Filter(db.searchHeader(fileName, white,
				black, ignoreColors, result_win_white, result_draw,
				result_win_black, result_none, event, site, ecoFrom, ecoTo,
				ecoNone, yearFrom, yearTo, filterOp, filter)));
	}

	@Override
	public Bundle getExtras() {
		Bundle bundle = new Bundle();
		if (filterMap.get(fileName) != null) {
			bundle.putInt("filterSize", filterMap.get(fileName).getSize());
			bundle.putInt("count", count);
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
		Filter filter = filterMap.get(fileName);
		if (filter != null) {
			return filter.getSize();
		} else {
			return this.count;
		}
	}

	private void setGameInfo(int gameNo, boolean isFavorite) {
		this.gameInfo = new GameInfo();
		gameInfo.setEvent(this.db.getEvent());
		if (gameInfo.getEvent().equals("?")) {
			gameInfo.setEvent("");
		}
		gameInfo.setSite(this.db.getSite());
		if (gameInfo.getSite().equals("?")) {
			gameInfo.setSite("");
		}
		String date = this.db.getDate();
		if (date.endsWith(".??.??")) {
			date = date.substring(0, date.length() - 6);
		} else if (date.endsWith(".??")) {
			date = date.substring(0, date.length() - 3);
		}
		if (date.equals("?") || date.equals("????")) {
			date = "";
		}
		gameInfo.setDate(date);
		gameInfo.setRound(this.db.getRound());
		if (gameInfo.getRound().equals("?")) {
			gameInfo.setRound("");
		}
		gameInfo.setWhite(this.db.getWhite());
		gameInfo.setBlack(this.db.getBlack());
		gameInfo.setResult(this.db.getResult());
		gameInfo.setPgn(loadPGN ? this.db.getPGN() : null);
		gameInfo.setId(gameNo);
		gameInfo.setFavorite(isFavorite);
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
		boolean onlyHeaders = !loadPGN;
		boolean isFavorite = this.db.loadGame(fileName, gameNo, onlyHeaders);
		setGameInfo(gameNo, isFavorite);
		return true;
	}

	private boolean onFilterMove(int oldPosition, int newPosition) {
		int gameNo = filterMap.get(fileName).getGameNo(
				startPosition + newPosition);
		if (gameNo >= 0) {
			boolean onlyHeaders = !loadPGN;
			boolean isFavorite = this.db
					.loadGame(fileName, gameNo, onlyHeaders);
			setGameInfo(gameNo, isFavorite);
			gameInfo.setCurrentPly(filterMap.get(fileName).getGamePly(
					startPosition + newPosition));
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
		if (this.gameInfo != null) {
			return new Integer(this.gameInfo.getColumn(projection[position]))
					.intValue();
		}
		return 0;
	}

	@Override
	public long getLong(int position) {
		if (this.gameInfo != null) {
			return new Long(this.gameInfo.getColumn(projection[position]))
					.longValue();
		}
		return 0;
	}

	@Override
	public short getShort(int position) {
		if (this.gameInfo != null) {
			return new Short(this.gameInfo.getColumn(projection[position]))
					.shortValue();
		}
		return 0;
	}

	@Override
	public String getString(int position) {
		if (this.gameInfo != null) {
			int column = projection[position];
			if (column == getColumnIndex(ScidProviderMetaData.ScidMetaData.IS_FAVORITE)) {
				// TODO remove this hack
				// always re-opens the index file!
				// --> therefore isFavorite cannot be used in the game list
				// because of performance issue
				return "" + db.isFavorite(fileName, this.gameInfo.getId());
			} else {
				return this.gameInfo.getColumn(column);
			}
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
		return null;
	}

	private void getFavorites(String fileName) {
		filterMap.put(fileName, new Filter(db.getFavorites(fileName)));
	}
}
