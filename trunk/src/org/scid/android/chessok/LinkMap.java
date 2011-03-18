package org.scid.android.chessok;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.scid.android.Link;

public class LinkMap implements Serializable {
	private static final long serialVersionUID = -4384016344960229469L;
	private Map<String, List<Link>> linkMap;

	public LinkMap(Map<String, List<Link>> map) {
		this.setLinkMap(map);
	}

	public void setLinkMap(Map<String, List<Link>> linkMap) {
		this.linkMap = linkMap;
	}

	public Map<String, List<Link>> getLinkMap() {
		return linkMap;
	}
}
