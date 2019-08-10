package org.scid.android;

public interface Progress {
	boolean isCancelled();
	void publishProgress(int value);
}
