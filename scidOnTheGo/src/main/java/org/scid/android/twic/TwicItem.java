package org.scid.android.twic;


public class TwicItem {

	private String link;

	public TwicItem(String link) {
		this.link = link;
	}

	public String getLink() {
		return link;
	}

	
	@Override
	public String toString() {
		String twicNo = "???";
		if (link != null) {
			twicNo = link.replaceAll("[^0-9]*", "");
		}
		return "The Week in Chess " + twicNo;
	}

}
