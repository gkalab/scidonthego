package org.scid.android;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;

public class ImportTwicTask extends AsyncTask {
	private Activity activity;
	private ProgressDialog progressDlg;

	@Override
	protected Object doInBackground(Object... params) {
		String result = null;
		this.activity = (Activity) params[0];
		this.progressDlg = (ProgressDialog) params[1];
		String zipUrl = (String) params[2];
		TwicDownloader downloader = new TwicDownloader();
		File pgnFile = downloader.getPgnFromZipUrl(Environment
				.getExternalStorageDirectory()
				+ File.separator + "scid", zipUrl);
		if (pgnFile != null) {
			result = pgnFile.getName();
		}
		return result;
	}

	@Override
	protected void onPostExecute(Object result) {
		progressDlg.dismiss();
		String resultText = (String) result;
		if (result != null) {
			activity.setResult(activity.RESULT_OK, (new Intent())
					.setAction(resultText));
			activity.finish();
		} else {
			activity.setResult(activity.RESULT_CANCELED);
		}
	}
}
