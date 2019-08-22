package org.scid.android;

import java.io.File;
import java.io.IOException;

import android.os.AsyncTask;

public class DownloadTask extends AsyncTask {
	private IDownloadCallback callback;

	@Override
	protected Object doInBackground(Object... params) {
		File result = null;
		this.callback = (IDownloadCallback) params[0];
		String path = (String) params[1];
		try {
			result = Tools.downloadFile(path);
		} catch (IOException e) {
			callback.downloadFailure(e.getMessage());
		}
		return result;
	}

	@Override
	protected void onPostExecute(Object result) {
		if (result != null) {
			callback.downloadSuccess((File) result);
		}
	}
}
