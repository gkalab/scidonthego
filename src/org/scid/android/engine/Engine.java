package org.scid.android.engine;

public class Engine implements Comparable<Engine> {

	private String fileName;

	private String name;

	private String packageName = null;

	private int versionCode = 0;

	public Engine(String fileName) {
		this.fileName = fileName;
	}

	public Engine(String name, String fileName, String packageName,
			int versionCode) {
		this.name = name;
		this.fileName = fileName;
		this.packageName = packageName;
		this.versionCode = versionCode;
	}

	public String getFileName() {
		return fileName;
	}

	public String getName() {
		return name;
	}

	@Override
	public int compareTo(Engine otherEngine) {
		return fileName.compareTo(otherEngine.getFileName());
	}

	public String getPackageName() {
		return packageName;
	}

	public int getVersionCode() {
		return versionCode;
	}
}
