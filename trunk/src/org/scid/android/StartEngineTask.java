package org.scid.android;

import java.io.File;

import org.scid.android.engine.EngineConfig;
import org.scid.android.engine.EngineManager;
import org.scid.android.gamelogic.ChessController;

import android.os.AsyncTask;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

public class StartEngineTask extends AsyncTask {
	private ScidAndroidActivity activity;
	private ChessController ctrl;

	@Override
	protected Object doInBackground(Object... params) {
		this.activity = (ScidAndroidActivity) params[0];
		this.ctrl = (ChessController) params[1];
		EngineConfig engineConfig = (EngineConfig) params[2];
		ensureEngine(engineConfig.getName(),
				new File(engineConfig.getExecutablePath()).getName(),
				engineConfig.getPackageName(), engineConfig.getVersionCode());
		ctrl.startEngine(engineConfig);
		return null;
	}

	private void ensureEngine(String engineName, String engineFileName,
			String enginePackage, int engineVersion) {
		String pkg = enginePackage;
		ChessEngineResolver resolver = new ChessEngineResolver(this.activity);
		if (enginePackage == null || enginePackage.length() == 0) {
			// check if there's an engine provided with the same engineFileName
			for (ChessEngine engine : resolver.resolveEngines()) {
				if (engine.getFileName().equals(engineFileName)) {
					pkg = engine.getPackageName();
					break;
				}
			}
		}
		if (pkg != null && pkg.length() > 0) {
			int newVersion = resolver.ensureEngineVersion(engineFileName, pkg,
					engineVersion, activity.getFilesDir());
			File engineFile = new File(activity.getFilesDir(),engineFileName);
			if (newVersion > engineVersion && engineFile.exists()) {
				new EngineManager(activity)
						.saveToConfiguration(engineName,
								engineFile.getAbsolutePath(), pkg,
								newVersion);
			}
		}
	}

	@Override
	protected void onPostExecute(Object result) {
		activity.onFinishStartAnalysis();
	}
}
