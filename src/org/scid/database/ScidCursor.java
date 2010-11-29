package org.scid.database;

import java.util.ArrayList;
import java.util.HashMap;
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

		public String getColumn(int position) {
		    switch (position) {
		    case 0: return "" + id;
		    case 1: return event;
		    case 2: return site;
		    case 3: return date;
		    case 4: return round;
		    case 5: return white;
		    case 6: return black;
		    case 7: return result;
		    case 8: return pgn;
		    case 9: return this.toString();
		    case 10: return "" + currentPly;
		    default: return null;
		    }
		}
	}

	private int count;
	private DataBase db;
	private String fileName;
	private GameInfo gi;
	private int startPosition;
	private int[] projection;
	boolean wantPGN = false; // True if projection contains pgn column
	boolean singleGame = false;
	// TODO: check for thread safety
	private static Map<String, Filter> filterMap = new HashMap<String, Filter>();

	public ScidCursor(String fileName, String[] projection, boolean singleGame) {
		super();
		filterMap.put(fileName, null);
        this.singleGame = singleGame;
		init(fileName, projection, 0);
	}

	public ScidCursor(String fileName, String[] projection, int startPosition, boolean singleGame) {
		this(fileName, projection, singleGame);
		this.startPosition = startPosition;
		handleProjection(projection);
	}

	private final static String[] columns = new String[] {
            "_id",
            ScidProviderMetaData.ScidMetaData.EVENT,
            ScidProviderMetaData.ScidMetaData.SITE,
            ScidProviderMetaData.ScidMetaData.DATE,
            ScidProviderMetaData.ScidMetaData.ROUND,
            ScidProviderMetaData.ScidMetaData.WHITE,
            ScidProviderMetaData.ScidMetaData.BLACK,
            ScidProviderMetaData.ScidMetaData.RESULT,
            ScidProviderMetaData.ScidMetaData.PGN,
            ScidProviderMetaData.ScidMetaData.SUMMARY,
            ScidProviderMetaData.ScidMetaData.CURRENT_PLY
    };
	
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
	        for (int i = 0; i < columns.length; i++)
	            this.projection[i] = i;
	    } else {
	        ArrayList<Integer> proj = new ArrayList<Integer>();
	        for (String p : projection) {
	            int idx = 0;
	            for (int i = 0; i < columns.length; i++)
	                if (columns[i].equals(p)) {
	                    idx = i;
	                    break;
		        }
	            proj.add(idx);
	        }
	        this.projection = new int[proj.size()];
	        for (int i = 0; i < proj.size(); i++)
	            this.projection[i] = proj.get(i);
	    }
	    wantPGN = false;
	    for (int p : this.projection)
	        if (p == 8) {
	            wantPGN = true;
	            break;
	        }
	}

	public ScidCursor(String fileName, String[] projection, int startPosition,
			String filterOperation, String fen, int searchType, boolean singleGame) {
		super();
        this.singleGame = singleGame;
		init(fileName, projection, startPosition);
		searchBoard(fileName, filterOperation, fen, searchType);
	}

	public ScidCursor(String fileName, String[] projection, int startPosition, 
	                  String[] selectionArgs, boolean singleGame) {
		super();
        this.singleGame = singleGame;
		init(fileName, projection, startPosition);
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
        boolean ecoNone = Boolean.parseBoolean(selectionArgs[12]);

		filterMap.put(fileName, new Filter(db.searchHeader(fileName, white,
				black, ignoreColors, result_win_white, result_draw,
				result_win_black, result_none, event, site, ecoFrom, ecoTo,
				ecoNone, filterOp, filter)));
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
        String[] ret = new String[projection.length];
        int idx = 0;
        for (int i : projection)
            ret[idx++] = columns[i];
        return ret;
	}

	@Override
	public int getCount() {
		return this.count;
	}

	private void setGameInfo(int gameNo) {
		this.gi = new GameInfo();
		gi.event = this.db.getEvent();
		if (gi.event.equals("?")) {
			gi.event = "";
		}
		gi.site = this.db.getSite();
		if (gi.site.equals("?")) {
			gi.site = "";
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
		gi.date = date;
		gi.round = this.db.getRound();
		if (gi.round.equals("?")) {
			gi.round = "";
		}
		gi.white = this.db.getWhite();
		gi.black = this.db.getBlack();
		gi.result = this.db.getResult();
		gi.pgn = wantPGN ? this.db.getPGN() : null;
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
        boolean onlyHeaders = !wantPGN;
		this.db.loadGame(fileName, gameNo, onlyHeaders);
		setGameInfo(gameNo);
		return true;
	}

	private boolean onFilterMove(int oldPosition, int newPosition) {
		int gameNo = filterMap.get(fileName).getGameNo(
				startPosition + newPosition);
		if (gameNo >= 0) {
		    boolean onlyHeaders = !wantPGN;
			this.db.loadGame(fileName, gameNo, onlyHeaders);
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
			return new Integer(this.gi.getColumn(projection[position])).intValue();
		}
		return 0;
	}

	@Override
	public long getLong(int position) {
		if (this.gi != null) {
			return new Long(this.gi.getColumn(projection[position])).longValue();
		}
		return 0;
	}

	@Override
	public short getShort(int position) {
		if (this.gi != null) {
			return new Short(this.gi.getColumn(projection[position])).shortValue();
		}
		return 0;
	}

	@Override
	public String getString(int position) {
		if (this.gi != null) {
			return this.gi.getColumn(projection[position]);
		}
		return null;
	}

	@Override
	public boolean isNull(int position) {
		if (this.gi != null) {
			return "".equals(this.gi.getColumn(projection[position]));
		}
		return true;
	}
}
