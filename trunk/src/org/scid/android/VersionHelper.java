package org.scid.android;

import android.app.Activity;

public class VersionHelper
{
    static void refreshActionBarMenu(Activity activity)
    {
        activity.invalidateOptionsMenu();
    }
}
