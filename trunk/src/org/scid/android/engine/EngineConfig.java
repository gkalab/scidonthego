package org.scid.android.engine;

/**
 * Class for defining an engine configuration.  Hopefully someday to include
 * engine parameters.
 */
public class EngineConfig {
	private String name;
	private String executablePath;

	public EngineConfig(String name, String executablePath) {
		this.name = name;
		this.executablePath = executablePath;
	}

	public String getName() {
		return name;
	}

	public String getExecutablePath() {
		return executablePath;
	}
}
