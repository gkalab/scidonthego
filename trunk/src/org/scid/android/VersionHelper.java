package org.scid.android;

import android.app.Activity;

/**
 * Utility class to hide methods that are not available on SDK versions < 11
 */
public class VersionHelper
{
    static void refreshActionBarMenu(Activity activity)
    {
        activity.invalidateOptionsMenu();
    }
}
