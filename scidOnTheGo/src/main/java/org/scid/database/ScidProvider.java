package org.scid.database;

import org.scid.database.ScidProviderMetaData.ScidMetaData;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class ScidProvider extends ContentProvider {
	/** The current cursor */
	private ScidCursor cursor;
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
		// not implemented
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// not implemented
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		ScidCursor result = null;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_GAME_COLLECTION_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			result = new ScidCursor(selection, projection, false);
			break;
		case INCOMING_SINGLE_GAME_URI_INDICATOR:
			if (selection == null) {
				throw new IllegalArgumentException(
						"The scid file name must be specified as the selection.");
			}
			int startPosition = Integer.parseInt(uri.getLastPathSegment());
			result = new ScidCursor(selection, projection, startPosition, true);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		this.cursor = result;
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// not implemented
		return 0;
	}
}
