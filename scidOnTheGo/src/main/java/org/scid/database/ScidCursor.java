package org.scid.database;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.scid.database.ScidProviderMetaData.ScidMetaData;

import android.database.AbstractCursor;
import android.os.Bundle;
import android.util.Log;

public class ScidCursor extends AbstractCursor {
	private int count;

	private String fileName;

	private GameInfo gameInfo;

	private int startPosition;

	private int[] projection;

	private boolean loadPGN = false; // True if projection contains pgn column

	private boolean singleGame = false;

	private boolean reloadIndex = true;

	public ScidCursor(String fileName, String[] projection, boolean singleGame) {
		super();
		this.singleGame = singleGame;
		init(fileName, projection, 0);
	}

	public ScidCursor(String fileName, String[] projection, int startPosition,
			boolean singleGame) {
		this(fileName, projection, singleGame);
		this.startPosition = startPosition;
		handleProjection(projection);
	}

//	private final static String[] columns = new String[] { "_id",
//			ScidProviderMetaData.ScidMetaData.EVENT,
//			ScidProviderMetaData.ScidMetaData.SITE,
//			ScidProviderMetaData.ScidMetaData.DATE,
//			ScidProviderMetaData.ScidMetaData.ROUND,
//			ScidProviderMetaData.ScidMetaData.WHITE,
//			ScidProviderMetaData.ScidMetaData.BLACK,
//			ScidProviderMetaData.ScidMetaData.RESULT,
//			ScidProviderMetaData.ScidMetaData.PGN,
//			ScidProviderMetaData.ScidMetaData.SUMMARY,
//			ScidProviderMetaData.ScidMetaData.CURRENT_PLY,
//			ScidProviderMetaData.ScidMetaData.DETAILS,
//			ScidProviderMetaData.ScidMetaData.IS_FAVORITE,
//			ScidProviderMetaData.ScidMetaData.IS_DELETED };

	private void init(String fileName, String[] projection, int startPosition) {
		this.fileName = fileName;
		DataBase.loadFile(fileName);
		this.count = singleGame ? 1 : DataBase.getSize();
		this.startPosition = startPosition;
		handleProjection(projection);
	}

	private void handleProjection(String[] projection) {
		if (projection == null) {
			this.projection = new int[ScidMetaData.columns.length];
			for (int i = 0; i < ScidMetaData.columns.length; i++) {
				this.projection[i] = i;
			}
		} else {
			ArrayList<Integer> proj = new ArrayList<Integer>();
			for (String p : projection) {
				int idx = 0;
				for (int i = 0; i < ScidMetaData.columns.length; i++) {
					if (ScidMetaData.columns[i].equals(p)) {
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
	public String[] getColumnNames() {
		String[] ret = new String[projection.length];
		int idx = 0;
		for (int i : projection) {
			ret[idx++] = ScidMetaData.columns[i];
		}
		return ret;
	}

	@Override
	public int getCount() {
		return count;
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
			if (date == null) {
				date = "";
			} else if (date.endsWith(".??.??")) {
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
			String[] results = {"*","1-0","0-1","1/2"};
			gameInfo.setResult(results[DataBase.getResult()]);
			byte[] dbPgn = DataBase.getPGN();
			if (dbPgn != null) {
				gameInfo.setPgn(loadPGN ? new String(DataBase.getPGN(),
						DataBase.SCID_ENCODING) : null);
			}
		} catch (UnsupportedEncodingException e) {
			Log.e("SCID", "Error converting byte[] to String", e);
		}
		gameInfo.setId(gameNo);
		gameInfo.setFavorite(isFavorite);
		gameInfo.setDeleted(DataBase.isDeleted());
	}

	private String getSanitizedString(byte[] value)
			throws UnsupportedEncodingException {
		if (value == null) {
			return "";
		} else {
			try {
				return Utf8Converter.convertToUTF8(new String(value, DataBase.SCID_ENCODING));
			} catch (UnsupportedEncodingException e) {
				return "";
			}
		}
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
		int gameNo = startPosition + newPosition;
		boolean onlyHeaders = !loadPGN;
		boolean isFavorite = DataBase.loadGame(gameNo,	onlyHeaders);
		setGameInfo(gameNo, isFavorite);
		this.reloadIndex = false;
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
