package org.scid.android;

import org.scid.android.gamelogic.ChessController;
import org.scid.android.gamelogic.Position;
import org.scid.database.DataBaseView;

import android.app.Application;

public class ScidApplication extends Application {
	private DataBaseView dbv = null;
	private String currentFileName = ""; // always non-null
	private Position position = null;
	private ChessController controller;

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public String getCurrentFileName() {
		return currentFileName;
	}

	/** strip extension and set current file name */
	public void setCurrentFileName(String currentFileName) {
		this.currentFileName = Tools.stripExtension(currentFileName);
	}

	public DataBaseView getDataBaseView() {
		return dbv;
	}

	public void setDataBaseView(DataBaseView dbv) {
		this.dbv = dbv;
	}

	public int getGameId() {
		return (dbv == null) ? -1 : dbv.getGameId();
	}

	public int getNoGames() {
		return (dbv == null) ? 0 : dbv.getCount();
	}

	public boolean isFavorite() {
		return (dbv == null) ? false : dbv.isFavorite();
	}

	public void setFavorite(boolean value) {
		if (dbv != null) // TODO: remove
			dbv.setFavorite(value);
	}

	public boolean isDeleted() {
		return (dbv == null) ? false : dbv.isDeleted();
	}

	public void setDeleted(boolean value) {
		if (dbv != null) // TODO: remove
			dbv.setDeleted(value);
	}

	public void setController(ChessController controller) {
		this.controller = controller;
	}

	public ChessController getController() {
		return this.controller;
	}
}