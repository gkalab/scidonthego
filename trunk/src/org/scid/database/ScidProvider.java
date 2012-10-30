package org.scid.database;

import org.scid.database.ScidProviderMetaData.ScidMetaData;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class ScidProvider extends ContentProvider {

	/**
	 * The current cursor
	 */
	private Cursor cursor;

	private static final int BOARDSEARCH_SELECTION_ARGS_LENGTH = 3;
	// Provide a mechanism to identify all the incoming uri patterns.
	private static final UriMatcher sUriMatcher;
	private static final int INCOMING_GAME_COLLECTION_URI_INDICATOR = 1;

	private static final int INCOMING_SINGLE_GAME_URI_INDICATOR = 2;

	private static final int GET_FAVORITES_SELECTION_ARGS_LENGTH = 0;
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
				result = searchBoard(selection, projection, 0, selectionArgs, false);
			} else if (selectionArgs.length > BOARDSEARCH_SELECTION_ARGS_LENGTH) {
				result = searchHeader(selection, projection, 0, selectionArgs, false);
			} else if (selectionArgs.length == GET_FAVORITES_SELECTION_ARGS_LENGTH) {
				result = getFavorites(selection, projection);
			}
			break;
		case INCOMING_SINGLE_GAME_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			int startPosition = Integer.parseInt(uri.getLastPathSegment());
			if (selectionArgs == null) {
				result = new ScidCursor(selection, projection, startPosition, true);
			} else if (selectionArgs.length == 3) {
				result = searchBoard(selection, projection, startPosition, selectionArgs, true);
			} else if (selectionArgs.length == 13) {
				result = searchHeader(selection, projection, startPosition, selectionArgs, true);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		this.cursor = result;
		return result;
	}

	private static Cursor getFavorites(String selection, String[] projection) {
		return new ScidCursor(selection, projection);
	}

	private static Cursor searchBoard(String selection, String[] projection,
			int startPosition, String[] selectionArgs, boolean singleGame) {
		return new ScidCursor(selection, projection, startPosition,
				selectionArgs[0], selectionArgs[1], Integer.parseInt(selectionArgs[2]), singleGame);
	}

	private static Cursor searchHeader(String selection, String[] projection,
			int startPosition, String[] selectionArgs, boolean singleGame) {
		return new ScidCursor(selection, projection, startPosition,
				selectionArgs, singleGame);
	}

	/**
	 * Update a game in the scid database. The last path segment must specify
	 * the real game number in the database (not the one in the current filter)
	 *
	 * currently only sets the favorite flag or the delete flag of a game
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
			int gameNo = Integer.parseInt(uri.getLastPathSegment());
			// save favorite flag
			if (values.containsKey("isFavorite")) {
				DataBase.setFavorite(selection, gameNo, values
						.getAsBoolean("isFavorite"));
				setCursorValue("isFavorite", values.getAsBoolean("isFavorite"));
				result = 1;
			}
			if (values.containsKey("isDeleted")) {
				DataBase.setDeleted(selection, gameNo, values
						.getAsBoolean("isDeleted"));
				setCursorValue("isDeleted", values.getAsBoolean("isDeleted"));
				result = 1;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return result;
	}

	private void setCursorValue(String key, boolean value) {
		if (this.cursor != null) {
			Bundle bundle = new Bundle();
			bundle.putBoolean(key, value);
			cursor.respond(bundle);
		}
	}
}
