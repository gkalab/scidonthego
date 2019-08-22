package org.scid.android.engine;

/**
 * Class for defining an engine configuration. Hopefully someday to include
 * engine parameters.
 */
public class EngineConfig {
	private String name;
	private String executablePath;
	private int versionCode;
	private String packageName;

	EngineConfig(String name, String executablePath, String packageName, int versionCode) {
		this.name = name;
		this.executablePath = executablePath;
		this.packageName = packageName;
		this.versionCode = versionCode;
	}

	public String getName() {
		return name;
	}

	String getExecutablePath() {
		return executablePath;
	}

	int getVersionCode() {
		return versionCode;
	}

	String getPackageName() {
		return packageName;
	}
}
