package org.scid.android;

import org.scid.android.gamelogic.ChessController;
import org.scid.android.gamelogic.Position;

import android.app.Application;
import android.database.Cursor;
import android.os.Bundle;

public class ScidApplication extends Application {
	private Cursor gamesCursor = null;
	private String currentFileName = "";
	private Position position = null;
	private int currentGameNo = -1;
	private int noGames = 0;
	private boolean isFavorite = false;
	private ChessController controller;
	private boolean isDeleted = false;

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public String getCurrentFileName() {
		return currentFileName;
	}

	public void setCurrentFileName(String currentFileName) {
		this.currentFileName = currentFileName;
	}

	public Cursor getGamesCursor() {
		return gamesCursor;
	}

	public void setGamesCursor(Cursor gamesCursor) {
		this.gamesCursor = gamesCursor;
	}

	public int getCurrentGameNo() {
		return this.currentGameNo;
	}

	public int getNoGames() {
		return this.noGames;
	}

	public void setCurrentGameNo(int currentGameNo) {
		this.currentGameNo = currentGameNo;
	}

	public void setNoGames(Cursor cursor) {
		this.noGames = cursor.getCount();
		Bundle extras = cursor.getExtras();
		if (extras != Bundle.EMPTY) {
			int count = extras.getInt("count");
			if (count > 0) {
				this.noGames = count;
			}
		}
	}

	public boolean isFavorite() {
		return this.isFavorite;
	}

	/**
	 * Set the current game as a favorite (true) or not (false)
	 * 
	 * @param isFavorite
	 */
	public void setFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}

	public boolean isDeleted() {
		return this.isDeleted;
	}

	/**
	 * Set the current game to deleted (true) or not (false)
	 * 
	 * @param isDeleted
	 */
	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public void setController(ChessController controller) {
		this.controller = controller;
	}

	public ChessController getController() {
		return this.controller;
	}
}