package org.scid.database;

public class GameInfo {
	private String event = "";
	private String site = "";
	private String date = "";
	private String round = "";
	private String white = "";
	private String black = "";
	private String result = "";
	private String pgn = "";
	private int id = -1;
	private int currentPly = 0;
	private Boolean isFavorite = false;

	public String toString() {
		StringBuilder info = new StringBuilder();
		info.append(white);
		info.append(" - ");
		info.append(black);
		if (date.length() > 0) {
			info.append(' ');
			info.append(date);
		}
		if (round.length() > 0) {
			info.append(' ');
			info.append(round);
		}
		if (event.length() > 0) {
			info.append(' ');
			info.append(event);
		}
		if (site.length() > 0) {
			info.append(' ');
			info.append(site);
		}
		info.append(' ');
		info.append(result);
		return info.toString();
	}

	public String getColumn(int position) {
		switch (position) {
		case 0:
			return "" + id;
		case 1:
			return event;
		case 2:
			return site;
		case 3:
			return date;
		case 4:
			return round;
		case 5:
			return white;
		case 6:
			return black;
		case 7:
			return result;
		case 8:
			return pgn;
		case 9:
			return this.toString();
		case 10:
			return "" + currentPly;
		case 11:
			return this.getDetails();
		case 12:
			if (isFavorite) {
				return "" + 1;
			} else {
				return "" + 0;
			}
		default:
			return null;
		}
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getEvent() {
		return event;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getSite() {
		return site;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDate() {
		return date;
	}

	public void setRound(String round) {
		this.round = round;
	}

	public String getRound() {
		return round;
	}

	public void setWhite(String white) {
		this.white = white;
	}

	public String getWhite() {
		return white;
	}

	public void setBlack(String black) {
		this.black = black;
	}

	public String getBlack() {
		return black;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getResult() {
		return result;
	}

	public void setPgn(String pgn) {
		this.pgn = pgn;
	}

	public String getPgn() {
		return pgn;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setCurrentPly(int currentPly) {
		this.currentPly = currentPly;
	}

	public int getCurrentPly() {
		return currentPly;
	}

	public String getDetails() {
		StringBuilder info = new StringBuilder();
		info.append("<b>" + result + "</b>");
		info.append(' ');
		if (event.length() > 0) {
			info.append(' ');
			info.append(event);
		}
		if (site.length() > 0) {
			info.append(' ');
			info.append(site);
		}
		if (round.length() > 0) {
			info.append(' ');
			info.append(round);
		}
		if (date.length() > 0) {
			info.append(' ');
			info.append(date);
		}
		return info.toString();

	}

	public void setFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}

	public boolean isFavorite() {
		return isFavorite;
	}
}
