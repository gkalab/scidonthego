package org.scid.database;

import org.scid.database.ScidProviderMetaData.ScidMetaData;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class ScidProvider extends ContentProvider {
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
		// TODO Auto-generated method stub
		Cursor result = null;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_GAME_COLLECTION_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			if (selectionArgs == null) {
				result = new ScidCursor(selection);
			} else if (selectionArgs.length == 3) {
				result = searchBoard(selection, 0, selectionArgs);
			} else if (selectionArgs.length == 12) {
				result = searchHeader(selection, 0, selectionArgs);
			}
			break;
		case INCOMING_SINGLE_GAME_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			int startPosition = new Integer(uri.getLastPathSegment());
			if (selectionArgs == null) {
				result = new ScidCursor(selection, startPosition);
			} else if (selectionArgs.length == 3) {
				result = searchBoard(selection, startPosition, selectionArgs);
			} else if (selectionArgs.length == 12) {
				result = searchHeader(selection, startPosition, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return result;
	}

	private Cursor searchBoard(String selection, int startPosition,
			String[] selectionArgs) {
		return new ScidCursor(selection, startPosition, selectionArgs[0],
				selectionArgs[1], new Integer(selectionArgs[2]));
	}

	private Cursor searchHeader(String selection, int startPosition,
			String[] selectionArgs) {
		return new ScidCursor(selection, startPosition, selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
