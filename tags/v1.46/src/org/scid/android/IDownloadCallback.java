package org.scid.android;

import java.io.File;

/**
 * Callback interface. Will be called upon success or failure of a download.
 */
public interface IDownloadCallback {
	/**
	 * The download was successful.
	 * @param downloadedFile
	 */
	void downloadSuccess(File downloadedFile);

	/**
	 * Failure with the exception message
	 * @param message
	 */
	void downloadFailure(String message);
}
