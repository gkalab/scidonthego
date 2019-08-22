package org.scid.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;

class ColorTheme {
	private static ColorTheme inst = null;

	/** Get singleton instance. */
	static ColorTheme instance() {
		if (inst == null)
			inst = new ColorTheme();
		return inst;
	}

	final static int DARK_SQUARE = 0;
	final static int BRIGHT_SQUARE = 1;
	final static int SELECTED_SQUARE = 2;
	final static int CURSOR_SQUARE = 3;
	final static int DARK_PIECE = 4;
	final static int BRIGHT_PIECE = 5;
	final static int CURRENT_MOVE = 6;
	final static int ARROW_0 = 7;
	final static int ARROW_1 = 8;
	final static int ARROW_2 = 9;
	final static int ARROW_3 = 10;
	final static int ARROW_4 = 11;
	final static int ARROW_5 = 12;
	private final static int numColors = 13;

	private int[] colorTable = new int[numColors];

	private static final String[] prefNames = { "darkSquare", "brightSquare",
			"selectedSquare", "cursorSquare", "darkPiece", "brightPiece",
			"currentMove", "arrow0", "arrow1", "arrow2", "arrow3", "arrow4",
			"arrow5" };
	private static final String prefPrefix = "colors_";

	private final static String[][] themeColors = {
			{"#FF80A0A0", "#FFD0E0D0", "#FFFF0000", "#FF00FF00", "#FF000000",
					"#FFFFFFFF", "#FFAFC4D4", "#A01F1FFF", "#A0FF1F1F",
					"#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F"},
			{"#B58863", "#F0D9B5", "#FFFF0000", "#FF00FF00", "#FF000000",
					"#FFFFFFFF", "#FFAFC4D4", "#A01F1FFF", "#A0FF1F1F",
					"#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F"},
			{"#FF83A5D2", "#FFFFFFFA", "#FF3232D1", "#FF5F5FFD", "#FF282828",
					"#FFF0F0F0", "#FFAFC4D4", "#A01F1FFF", "#A01FFF1F",
					"#501F1FFF", "#501FFF1F", "#1E1F1FFF", "#281FFF1F"},
			{"#FF769656", "#FFEEEED2", "#FFFF0000", "#FF0000FF", "#FF000000",
					"#FFFFFFFF", "#FFAFC4D4", "#A01F1FFF", "#A0FF1F1F",
					"#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F"}};

	final void readColors(SharedPreferences preferences) {
		for (int i = 0; i < numColors; i++) {
			String prefName = prefPrefix + prefNames[i];
			String defaultColor = themeColors[0][i];
			String colorString = preferences.getString(prefName, defaultColor);
			try {
				colorTable[i] = Color.parseColor(colorString);
			} catch (IllegalArgumentException e) {
				colorTable[i] = 0;
			}
		}
	}

	final void setTheme(SharedPreferences preferences, int themeType) {
		Editor editor = preferences.edit();
		for (int i = 0; i < numColors; i++)
			editor.putString(prefPrefix + prefNames[i],
					themeColors[themeType][i]);
		editor.commit();
		readColors(preferences);
	}

	final int getColor(int colorType) {
		return colorTable[colorType];
	}
}
