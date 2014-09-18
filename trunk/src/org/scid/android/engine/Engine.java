package org.scid.android.engine;

public class Engine implements Comparable<Engine> {

	private String fileName;

	private String name;

	public Engine(String fileName) {
		this.fileName = fileName;
	}

	public Engine(String name, String fileName) {
		this.name = name;
		this.fileName = fileName;
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
}
