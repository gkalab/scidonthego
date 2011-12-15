package org.scid.android;

final class GameInfo {
	private String title;
	private String details;
	private boolean favorite = false;
	private boolean deleted = false;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}