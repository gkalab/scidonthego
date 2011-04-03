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
		ctrl.startEngine();
		return null;
	}

	@Override
	protected void onPostExecute(Object result) {
		progressDlg.dismiss();
		activity.onFinishStartAnalysis();
	}
}
