package org.scid.android;

final class GameInfo {
	private String title;
	private String details;
	private boolean favorite = false;
	private boolean deleted = false;

	public GameInfo(org.scid.database.GameInfo info){
		details = info.getDetails();
		title = info.getWhite()	+ " - " + info.getBlack();
		favorite = info.isFavorite();
		deleted = info.isDeleted();
	}

	public String getTitle() {
		return title;
	}

	public String getDetails() {
		return details;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public boolean isDeleted() {
		return deleted;
	}
}