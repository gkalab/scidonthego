package org.scid.android.twic;

import java.io.File;
import java.io.IOException;

import org.scid.android.Constants;
import org.scid.android.IDownloadCallback;
import org.scid.android.R;
import org.scid.android.Tools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;

public class ImportZipTask extends AsyncTask {
	private Activity activity;
	private ProgressDialog progressDlg;
	private String url;

	@Override
	protected Object doInBackground(Object... params) {
		File result = null;
		this.activity = (Activity) params[0];
		this.progressDlg = (ProgressDialog) params[1];
		this.url = (String) params[2];
		TwicDownloader downloader = new TwicDownloader();
		try {
			result = downloader.getPgnFromZipUrl(Tools.getScidDirectory(), url);
		} catch (IOException e) {
			Tools.showErrorMessage(
					activity,
					activity.getText(R.string.download_error) + " ("
							+ e.getMessage() + ")");
		}
		return result;
	}

	@Override
	protected void onPostExecute(Object result) {
		if (progressDlg != null && progressDlg.isShowing()) {
			progressDlg.dismiss();
		}
		if (result != null) {
			((IDownloadCallback) activity).downloadSuccess((File) result);
		} else {
			((IDownloadCallback) activity).downloadFailure(this.url);
		}
	}
}
