package org.scid.database;

import android.net.Uri;
import android.provider.BaseColumns;

public class ScidProviderMetaData {
	public static final String AUTHORITY = "org.scid.database.scidprovider";
	public static final int DATABASE_VERSION = 1;

	private ScidProviderMetaData() {
	}

	public static final class ScidMetaData implements BaseColumns {
		private ScidMetaData() {
		}

		// URI and MIME type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/games");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.scid.game";

		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.scid.game";

		public static final String WHITE = "white";
		public static final String BLACK = "black";
		public static final String SITE = "site";
		public static final String RESULT = "result";
		public static final String ROUND = "round";
		public static final String DATE = "date";
		public static final String EVENT = "event";
		public static final String PGN = "pgn";
		public static final String SUMMARY = "summary";
		public static final String DETAILS = "details";
		public static final String CURRENT_PLY = "current_ply";
		public static final String IS_FAVORITE = "is_favorite";
		public static final String IS_DELETED = "is_deleted";
	}
}
