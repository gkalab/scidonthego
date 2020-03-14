package org.scid.android.engine;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.scid.android.R;
import org.scid.android.Tools;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to manage UCI chess engines.
 */
public class EngineManager {
	private static final String INTERNAL_ENGINE_NAME = "Stockfish";
	private static final String INTERNAL_ENGINE_FILE_NAME = "libstockfish.so";
	private static final String ENGINE_DATA_FILE = "chessengines.xml";
	private static EngineConfig defaultEngine;

	private Context context;
	private String currentEngineName;

	// Current list of engines (doesn't include the built-in engine)
	private Map<String, EngineConfig> engines;
	private List<EngineChangeListener> changeListeners = new ArrayList<>();
	// Lock object to manage access to engines and changeListeners
	private final Object managerLock = new Object();

	public class EngineChangeEvent {
		public static final int ADD_ENGINE = 1;
		public static final int REMOVE_ENGINE = 2;

		private String engineName;
		private int changeType;
		private boolean success;

		EngineChangeEvent(String engineName, int changeType,
						  boolean success) {
			this.engineName = engineName;
			this.changeType = changeType;
			this.success = success;
		}

		public String getEngineName() {
			return engineName;
		}

		public int getChangeType() {
			return changeType;
		}

		public boolean getSuccess() {
			return success;
		}
	}

	public interface EngineChangeListener {
		void engineChanged(EngineChangeEvent event);
	}

	public EngineManager(Context context) {
		this.context = context;
		// Establish the default engine
		defaultEngine = new EngineConfig(INTERNAL_ENGINE_NAME,
				new File(context.getApplicationInfo().nativeLibraryDir, INTERNAL_ENGINE_FILE_NAME)
						.getAbsolutePath(), null, 0);
	}

	public void addEngineChangeListener(EngineChangeListener listener) {
		synchronized (managerLock) {
			if (listener != null) {
				changeListeners.add(listener);
			}
		}
	}

	public void removeEngineChangeListener(EngineChangeListener listener) {
		synchronized (managerLock) {
			int index = changeListeners.indexOf(listener);
			if (index >= 0) {
				changeListeners.remove(index);
			}
		}
	}

	/**
	 * Returns the EngineConfig for the default built-in engine.
	 *
	 * @return The default EngineConfig.
	 */
	public EngineConfig getDefaultEngine() {
		return defaultEngine;
	}

	/**
	 * Returns the EngineConfig for the currently selected engine. This is
	 * expected to stay in sync with the Analysis Engine preference.
	 *
	 * @return The current EngineConfig.
	 */
	public EngineConfig getCurrentEngine() {
		Map<String, EngineConfig> enginesList = getEnginesList();
		if (!enginesList.isEmpty() && currentEngineName != null) {
			EngineConfig engine = enginesList.get(currentEngineName);
			if (engine != null) {
				return engine;
			}
		}
		return getDefaultEngine();
	}

	/**
	 * Convenience method to get the name of the current engine.
	 *
	 * @return The current engine's name.
	 */
	public String getCurrentEngineName() {
		EngineConfig engine = getCurrentEngine();
		return engine.getName();
	}

	/**
	 * Returns a list of the names in the current list of Engines. The name of
	 * the built-in engine can be optionally included in the list.
	 *
	 * @param includeBuiltIn
	 *            If true, the name of the default engine will be included.
	 * @return Returns the list of engine names.
	 */
	public String[] getEngineNames(boolean includeBuiltIn) {
		ArrayList<String> engineNames = new ArrayList<>();
		if (includeBuiltIn) {
			engineNames.add(defaultEngine.getName());
		}
		engineNames.addAll(getEnginesList().keySet());
		Collections.sort(engineNames);
		String[] names = new String[engineNames.size()];
		return engineNames.toArray(names);
	}

	/**
	 * Sets the name of the current engine to use. It is expected that this name
	 * will stay in sync with the Analysis Engine preference. The name will be
	 * ignored if it does not match the built-in engine name of a name in the
	 * current list of engines.
	 *
	 * @param engineName
	 *            Engine name to be set.
	 */
	public void setCurrentEngineName(String engineName) {
		if (defaultEngine.getName().equals(engineName)
				|| getEnginesList().get(engineName) != null) {
			currentEngineName = engineName;
		}
	}

	/**
	 * Adds a new engine to the list of engines. This method is also responsible
	 * for copying the executable to internal storage and setting its
	 * permissions. The copying is done using an async CopyExecutableTask. The
	 * engine will not be added until after the copy succeeds. If the executable
	 * is already present in internal storage, the addition occurs immediately.
	 *
	 * @param engineName
	 *            The name of the engine.
	 * @param executable
	 *            The name of the executable file for the engine.
	 * @param enginePackage
	 *            The name of the package of the engine. Can be null.
	 * @param engineVersion
	 *            The version of the engine. Can be 0 if there is no version
	 *            information.
	 * @return Returns true if an engine by the same name doesn't already exist.
	 */
	public boolean addEngine(String engineName, String executable,
			String enginePackage, int engineVersion) {
		EngineConfig engine = getEnginesList().get(engineName);
		if (engine == null) {
			File engineFile = new File(context.getFilesDir(), executable);
			String engineAbsPath = engineFile.getAbsolutePath();
			boolean engineExists = engineFile.exists();
			if (!engineExists) {
				File scidFileDir = new File(Tools.getScidDirectory());
				File externFile = new File(scidFileDir, executable);
				if (externFile.exists()) {
					// Copy the engine file and add when done.
					new CopyExecutableTask(engineName, enginePackage,
							engineVersion).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, externFile, engineFile);
				} else {
					addOpenExchangeEngine(executable, engineName);
				}
				return true;
			} else {
				saveToConfiguration(engineName, engineAbsPath, enginePackage,
						engineVersion);
				Toast.makeText(context,
						context.getString(R.string.engine_added, engineName),
						Toast.LENGTH_SHORT).show();
				return true;
			}
		} else {
			notifyListeners(new EngineChangeEvent(engineName,
					EngineChangeEvent.ADD_ENGINE, true));
			Toast.makeText(context,
					context.getString(R.string.add_engine_exists, engineName),
					Toast.LENGTH_LONG).show();
		}
		return false;
	}

	private void saveToConfiguration(String engineName, String engineAbsPath,
									 String packageName, int versionCode) {
		Map<String, EngineConfig> enginesList = new TreeMap<>(
				getEnginesList());
		enginesList.put(engineName, new EngineConfig(engineName, engineAbsPath,
				packageName, versionCode));
		synchronized (managerLock) {
			engines = enginesList;
		}
		saveEngineData(enginesList);
		notifyListeners(new EngineChangeEvent(engineName,
				EngineChangeEvent.ADD_ENGINE, true));
	}

	private void addOpenExchangeEngine(String executable, String engineName) {
		ChessEngineResolver resolver = new ChessEngineResolver(context);
		List<ChessEngine> openEngines = resolver.resolveEngines();
		boolean found = false;
		for (ChessEngine openEngine : openEngines) {
		    found = true;
			if (openEngine.getFileName().equals(executable)) {
				saveToConfiguration(engineName, openEngine.getEnginePath(),
                        openEngine.getPackageName(),
                        openEngine.getVersionCode());
				break;
			}
		}
		if (!found) {
			// Shouldn't occur
			Log.e("SCID", "Executable not found: " + executable);
		}
	}

	/**
	 * Removes the specified engine from the engine list, if it exists.
	 *
	 * @param engineName
	 *            Name of the engine to remove.
	 * @return Returns the EngineConfig for the removed engine or null if it was
	 *         not found.
	 */
	public EngineConfig removeEngine(String engineName) {
		Map<String, EngineConfig> enginesList = new TreeMap<>(
				getEnginesList());
		EngineConfig removedConf = enginesList.remove(engineName);
		if (removedConf != null) {
			// If removing the current engine, clear the current engine name
			if (removedConf.getName().equals(currentEngineName)) {
				currentEngineName = null;
			}
			// Determine if the executable is still used
			String executablePath = removedConf.getExecutablePath();
			boolean stillUsed = false;
			for (Map.Entry<String, EngineConfig> entry : enginesList.entrySet()) {
				if (entry.getValue().getExecutablePath().equals(executablePath)) {
					stillUsed = true;
				}
			}
			synchronized (managerLock) {
				engines = enginesList;
			}
			saveEngineData(enginesList);
			notifyListeners(new EngineChangeEvent(engineName,
					EngineChangeEvent.REMOVE_ENGINE, true));
			Toast.makeText(context,
					context.getString(R.string.engine_removed, engineName),
					Toast.LENGTH_SHORT).show();

			// If executable is no longer used, delete it from
			// internal storage
			if (!stillUsed) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				File _executable = new File(
						removedConf.getExecutablePath());
				// Delete the executable
				_executable.delete();
			}
			if (Log.isLoggable("SCID", Log.INFO))
				Log.i("SCID", "Removed engine " + engineName);
		} else {
			notifyListeners(new EngineChangeEvent(engineName,
					EngineChangeEvent.REMOVE_ENGINE, false));
		}
		return removedConf;
	}

	/**
	 * Detects if an engine name is already in use in the engine list.
	 *
	 * @param name
	 *            Engine name to check.
	 * @return Returns true if an engine by that name is found.
	 */
	public boolean isNameUsed(String name) {
		return getEnginesList().containsKey(name);
	}

	/**
	 * Detects if the executable path is in use by any engines in the current
	 * list.
	 *
	 * @param executablePath
	 *            Executable path to check.
	 * @return Returns true if at least one engine uses the specified executable
	 *         path.
	 */
	public boolean isExecutableUsed(String executablePath) {
		for (EngineConfig engineConf : engines.values()) {
			if (engineConf.getExecutablePath().equals(executablePath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Writes the specified engine list to the data file.
	 *
	 * @param enginesList
	 *            List of engines to persist.
	 */
	private void saveEngineData(Map<String, EngineConfig> enginesList) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter(1024);
		FileWriter fw = null;
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "engines");
			for (EngineConfig engine : enginesList.values()) {
				serializer.startTag(null, "engine");
				serializer.attribute(null, "name", engine.getName());
				serializer.attribute(null, "path", engine.getExecutablePath());
				serializer.attribute(
						null,
						"package",
						engine.getPackageName() == null ? "" : engine
								.getPackageName());
				serializer.attribute(null, "version",
						"" + engine.getVersionCode());
				serializer.endTag(null, "engine");
			}
			serializer.endTag(null, "engines");
			serializer.endDocument();
			if (Log.isLoggable("SCID", Log.DEBUG))
				Log.d("SCID", "Engine XML: " + writer.toString());

			File engineDataFile = new File(context.getFilesDir(),
					ENGINE_DATA_FILE);
			fw = new FileWriter(engineDataFile);
			fw.write(writer.toString());
			fw.close();
		} catch (Exception e) {
			Log.e("SCID", e.getMessage(), e);
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					Log.e("SCID", e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Returns the current list of engines, loading them from the data file if
	 * necessary.
	 *
	 * @return Returns the current list of engines.
	 */
	private Map<String, EngineConfig> getEnginesList() {
		if (engines == null) {
			loadEngineData();
		}
		return engines;
	}

	/**
	 * Reads the data file to load the current list of engines.
	 */
	private void loadEngineData() {
		TreeMap<String, EngineConfig> list = new TreeMap<>();
		File engineDataFile = new File(context.getFilesDir(), ENGINE_DATA_FILE);
		if (engineDataFile.exists()) {
			FileReader fr = null;
			try {
				fr = new FileReader(engineDataFile);
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(fr);
				int eventType = parser.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						readEngine(parser, list);
					}
					eventType = parser.next();
				}
			} catch (IOException e) {
				Log.e("SCID", e.getMessage(), e);
			} catch (XmlPullParserException e) {
				Log.e("SCID", e.getMessage(), e);
			} finally {
				// Ensure file reader is closed.
				if (fr != null) {
					try {
						fr.close();
					} catch (IOException e) { /* Ignore */
					}
				}
			}
		}
		synchronized (managerLock) {
			engines = list;
		}
	}

	private void readEngine(XmlPullParser parser,
			TreeMap<String, EngineConfig> list) {
		if (parser.getName().equalsIgnoreCase("engine")) {
			String engineName = parser.getAttributeValue(null, "name");
			String enginePath = parser.getAttributeValue(null, "path");
			if (engineName != null && engineName.length() > 0
					&& list.get(engineName) == null && enginePath != null
					&& enginePath.length() > 0) {
				String enginePackage = parser
						.getAttributeValue(null, "package");
				String engineVersion = parser
						.getAttributeValue(null, "version");
				int version = 0;
				if (engineVersion != null && engineVersion.length() > 0) {
					version = Integer.parseInt(engineVersion);
				}
				File engineFile = new File(enginePath);
				if (engineFile.exists()) {
					list.put(engineName, new EngineConfig(engineName,
							engineFile.getAbsolutePath(), enginePackage,
							version));
				}
			}
		}
	}

	private void notifyListeners(EngineChangeEvent event) {
		List<EngineChangeListener> listeners;
		synchronized (managerLock) {
			listeners = new ArrayList<>(changeListeners);
		}
		// Notify listeners, if any
		if (listeners.size() > 0) {
			for (EngineChangeListener engineChangeListener : listeners) {
				engineChangeListener.engineChanged(event);
			}
		}
	}

	/**
	 * Task to copy a chess engine executable file to internal storage and set
	 * its permissions. It is also responsible for displaying a progress dialog
	 * and updating the engine list if the copy succeeds.
	 */
	private class CopyExecutableTask extends AsyncTask<File, Integer, Boolean>
			implements OnCancelListener, OnClickListener {
		private String engineName;
		private ProgressDialog progDlg;
		private File destFile;
		private String errorMsg;
		private String enginePackage;
		private int engineVersion;

		CopyExecutableTask(String engineName, String enginePackage,
						   int engineVersion) {
			this.engineName = engineName;
			this.enginePackage = enginePackage;
			this.engineVersion = engineVersion;
		}

		@Override
		protected Boolean doInBackground(File... params) {
			File srcFile = params[0];
			destFile = params[1];
			Boolean result = Boolean.FALSE;

			InputStream istream = null;
			FileOutputStream fout = null;
			boolean canceled = false;
			try {
				istream = new FileInputStream(srcFile);
				fout = new FileOutputStream(destFile);
				byte[] b = new byte[4096];
				int cnt = 0;
				while (!canceled && (cnt = istream.read(b)) != -1) {
					fout.write(b, 0, cnt);
					canceled = isCancelled();
				}
				istream.close();
				fout.close();
				if (!canceled) {
					if (Log.isLoggable("SCID", Log.INFO))
						Log.i("SCID", srcFile.getName() + " copied to "
								+ context.getFilesDir().getPath());
					result = Boolean.TRUE;
				}
			} catch (IOException e) {
				errorMsg = e.getLocalizedMessage();
				Log.e("SCID", errorMsg, e);
			} catch (SecurityException se) {
				errorMsg = se.getLocalizedMessage();
				Log.e("SCID", errorMsg, se);
			} finally {
				// Ensure streams are closed should an exception occur.
				if (fout != null) {
					try {
						fout.close();
					} catch (IOException e) { /* Ignore */
					}
				}
				if (istream != null) {
					try {
						istream.close();
					} catch (IOException e) { /* Ignore */
					}
				}
			}
			if (!canceled && result) {
				String absPath = destFile.getAbsolutePath();
				try {
					String[] cmd = {"chmod", "744", absPath};
					Process process = Runtime.getRuntime().exec(cmd);
					try {
						process.waitFor();
						if (Log.isLoggable("SCID", Log.DEBUG))
							Log.d("SCID", "chmod 744 " + absPath);
					} catch (InterruptedException e) {
						Log.e("SCID", e.getMessage(), e);
					}
				} catch (IOException e) {
					errorMsg = e.getLocalizedMessage();
					Log.e("SCID", errorMsg, e);
					result = Boolean.FALSE;
				}
				Map<String, EngineConfig> enginesList = new TreeMap<>(
						getEnginesList());
				enginesList.put(engineName, new EngineConfig(engineName,
						absPath, enginePackage, engineVersion));
				synchronized (managerLock) {
					engines = enginesList;
				}
				saveEngineData(enginesList);
			}
			return result;
		}

		@Override
		protected void onPreExecute() {
			progDlg = new ProgressDialog(context);
			progDlg.setTitle(context.getString(R.string.engine_copy_title));
			progDlg.setMessage(context.getString(R.string.engine_copy_msg,
					engineName));
			progDlg.setCancelable(true);
			progDlg.setOnCancelListener(this);
			progDlg.setButton(DialogInterface.BUTTON_NEGATIVE,
					context.getString(android.R.string.cancel), this);
			progDlg.show();
		}

		@Override
		protected void onCancelled() {
			if (destFile != null && destFile.exists()) {
				destFile.delete();
			}
			progDlg.dismiss();
			notifyListeners(new EngineChangeEvent(engineName,
					EngineChangeEvent.ADD_ENGINE, false));
			Toast.makeText(
					context,
					context.getString(R.string.engine_copy_canceled, engineName),
					Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			progDlg.dismiss();
			if (result) {
				notifyListeners(new EngineChangeEvent(engineName,
						EngineChangeEvent.ADD_ENGINE, true));
				Toast.makeText(
						context,
						context.getString(R.string.engine_added_copied,
								engineName), Toast.LENGTH_LONG).show();
			} else {
				notifyListeners(new EngineChangeEvent(engineName,
						EngineChangeEvent.ADD_ENGINE, false));
				Toast.makeText(
						context,
						context.getString(R.string.engine_copy_failed,
								(errorMsg != null ? errorMsg : "")),
						Toast.LENGTH_LONG).show();
			}
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			cancel(false);
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			cancel(false);
		}
	}
}
