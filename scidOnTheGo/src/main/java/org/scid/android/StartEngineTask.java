package org.scid.android;

import android.os.AsyncTask;

import org.scid.android.engine.EngineConfig;
import org.scid.android.gamelogic.ChessController;

public class StartEngineTask extends AsyncTask {
	private ScidAndroidActivity activity;
	private ChessController ctrl;

	@Override
	protected Object doInBackground(Object... params) {
		this.activity = (ScidAndroidActivity) params[0];
		this.ctrl = (ChessController) params[1];
		EngineConfig engineConfig = (EngineConfig) params[2];
		ctrl.startEngine(engineConfig);
		return null;
	}

	@Override
	protected void onPostExecute(Object result) {
		activity.onFinishStartAnalysis();
	}
}
