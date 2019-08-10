package org.scid.android;

import java.io.Serializable;

public class Link implements Serializable {

	private static final long serialVersionUID = 2903802880080582515L;
	private String link;
	private String description = null;

	public Link(final String link, final String description) {
		this.link = link;
		this.description = description;
	}

	public Link(String link) {
		this.link = link;
	}

	public String getLink() {
		return link;
	}

	@Override
	public String toString() {
		if (description != null) {
			return description;
		} else {
			String twicNo = "???";
			if (link != null) {
				twicNo = link.replaceAll("[^0-9]*", "");
			}
			return "The Week in Chess " + twicNo;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLink(String link) {
		this.link = link;
	}
}
