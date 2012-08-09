package org.scid.android;

import android.app.Activity;
import android.content.res.Configuration;

public class ScreenTools {
	/**
	 * Return true if layout-large is used. Code copied from
	 * android.content.res.Configuration.isLayoutSizeAtLeast because it's not
	 * available on versions < HONEYCOMB
	 * 
	 * @param activity
	 * @return
	 */
	public static boolean isLargeScreenLayout(Activity activity) {
		Configuration c = activity.getResources().getConfiguration();
		int cur = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		if (cur == Configuration.SCREENLAYOUT_SIZE_UNDEFINED)
			return false;
		return cur >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	public static boolean isTabletLayout(Activity activity) {
		return activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
				&& isLargeScreenLayout(activity);
	}

}
