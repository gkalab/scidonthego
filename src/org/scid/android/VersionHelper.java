package org.scid.android;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;

/**
 * Utility class to hide methods that are not available on SDK versions < 11
 */
public class VersionHelper {
	static Set<ClipboardChangedListener> clipChangedListeners = new HashSet<ClipboardChangedListener>();
	OnPrimaryClipChangedListener clipBoardListener = new OnPrimaryClipChangedListener() {

		@Override
		public void onPrimaryClipChanged() {
			for (ClipboardChangedListener listener : clipChangedListeners) {
				listener.clipboardChanged();
			}
		}
	};

	static void registerClipChangedListener(ClipboardChangedListener listener) {
		clipChangedListeners.add(listener);
	}

	static void refreshActionBarMenu(Activity activity) {
		activity.invalidateOptionsMenu();
	}
}
