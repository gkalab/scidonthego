package org.scid.android;

import org.scid.android.gamelogic.ChessController;

import android.app.ProgressDialog;
import android.os.AsyncTask;

public class StartEngineTask extends AsyncTask {
	private ScidAndroidActivity activity;
	private ProgressDialog progressDlg;
	private ChessController ctrl;

	@Override
	protected Object doInBackground(Object... params) {
		this.activity = (ScidAndroidActivity) params[0];
		this.progressDlg = (ProgressDialog) params[1];
		this.ctrl = (ChessController) params[2];
		String engineFileName = (String) params[3];
		ctrl.startEngine(engineFileName);
		return null;
	}

	@Override
	protected void onPostExecute(Object result) {
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
		activity.onFinishStartAnalysis();
	}
}
