package org.scid.database;

import org.scid.database.ScidProviderMetaData.ScidMetaData;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class ScidProvider extends ContentProvider {
	private DataBase db = new DataBase();

	private static final int BOARDSEARCH_SELECTION_ARGS_LENGTH = 3;
	// Provide a mechanism to identify all the incoming uri patterns.
	private static final UriMatcher sUriMatcher;
	private static final int INCOMING_GAME_COLLECTION_URI_INDICATOR = 1;

	private static final int INCOMING_SINGLE_GAME_URI_INDICATOR = 2;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(ScidProviderMetaData.AUTHORITY, "games",
				INCOMING_GAME_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(ScidProviderMetaData.AUTHORITY, "games/#",
				INCOMING_SINGLE_GAME_URI_INDICATOR);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case INCOMING_GAME_COLLECTION_URI_INDICATOR:
			return ScidMetaData.CONTENT_TYPE;

		case INCOMING_SINGLE_GAME_URI_INDICATOR:
			return ScidMetaData.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_GAME_COLLECTION_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			if (selectionArgs == null) {
				result = new ScidCursor(selection, projection, false);
			} else if (selectionArgs.length == BOARDSEARCH_SELECTION_ARGS_LENGTH) {
				result = searchBoard(selection, projection, 0, selectionArgs,
						false);
			} else if (selectionArgs.length > BOARDSEARCH_SELECTION_ARGS_LENGTH) {
				result = searchHeader(selection, projection, 0, selectionArgs,
						false);
			}
			break;
		case INCOMING_SINGLE_GAME_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			int startPosition = new Integer(uri.getLastPathSegment());
			if (selectionArgs == null) {
				result = new ScidCursor(selection, projection, startPosition,
						true);
			} else if (selectionArgs.length == 3) {
				result = searchBoard(selection, projection, startPosition,
						selectionArgs, true);
			} else if (selectionArgs.length == 13) {
				result = searchHeader(selection, projection, startPosition,
						selectionArgs, true);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return result;
	}

	private Cursor searchBoard(String selection, String[] projection,
			int startPosition, String[] selectionArgs, boolean singleGame) {
		return new ScidCursor(selection, projection, startPosition,
				selectionArgs[0], selectionArgs[1], new Integer(
						selectionArgs[2]), singleGame);
	}

	private Cursor searchHeader(String selection, String[] projection,
			int startPosition, String[] selectionArgs, boolean singleGame) {
		return new ScidCursor(selection, projection, startPosition,
				selectionArgs, singleGame);
	}

	/**
	 * Update a game in the scid database. The last path segment must specify
	 * the real game number in the database (not the one in the current filter)
	 * 
	 * currently only sets the favorite flag of a game
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int result = 0;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_SINGLE_GAME_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			int gameNo = new Integer(uri.getLastPathSegment());
			// save favorite flag
			if (values.containsKey("isFavorite")) {
				db.setFavorite(selection, gameNo, values
						.getAsBoolean("isFavorite"));
				result = 1;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return result;
	}

}
