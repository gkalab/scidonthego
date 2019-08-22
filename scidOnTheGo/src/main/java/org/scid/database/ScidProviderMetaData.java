package org.scid.database;

import android.net.Uri;
import android.provider.BaseColumns;

public class ScidProviderMetaData {
	static final String AUTHORITY = "org.scid.database.scidprovider";
	public static final int DATABASE_VERSION = 1;

	private ScidProviderMetaData() {
	}

	public static final class ScidMetaData implements BaseColumns {
		private ScidMetaData() {
		}

		// URI and MIME type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/games");

		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.scid.game";

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.scid.game";

		static final String WHITE = "white";
		static final String BLACK = "black";
		static final String SITE = "site";
		static final String RESULT = "result";
		static final String ROUND = "round";
		static final String DATE = "date";
		static final String EVENT = "event";
		public static final String PGN = "pgn";
		static final String SUMMARY = "summary";
		static final String DETAILS = "details";
		static final String CURRENT_PLY = "current_ply";
		static final String IS_FAVORITE = "is_favorite";
		static final String IS_DELETED = "is_deleted";

		static final String[] columns = new String[] {
			_ID, EVENT, SITE, DATE, ROUND, WHITE, BLACK,
			RESULT, PGN, SUMMARY, CURRENT_PLY, DETAILS,
			IS_FAVORITE, IS_DELETED
		};
	}
}
