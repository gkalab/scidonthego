package org.scid.android;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;

/**
 * Utility class to hide methods that are not available on SDK versions < 11
 */
public class VersionHelper {
	static Set<IClipboardChangedListener> clipChangedListeners = new HashSet<IClipboardChangedListener>();
	OnPrimaryClipChangedListener clipBoardListener = new OnPrimaryClipChangedListener() {

		@Override
		public void onPrimaryClipChanged() {
			for (IClipboardChangedListener listener : clipChangedListeners) {
				listener.clipboardChanged();
			}
		}
	};

	static void registerClipChangedListener(IClipboardChangedListener listener) {
		clipChangedListeners.add(listener);
	}

	static void refreshActionBarMenu(Activity activity) {
		activity.invalidateOptionsMenu();
	}

	public static void removeIconFromActionbar(Activity activity) {
		activity.getActionBar().setDisplayShowHomeEnabled(false);
	}
}
