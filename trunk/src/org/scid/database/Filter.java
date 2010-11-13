package org.scid.database;

import java.util.ArrayList;
import java.util.List;

public class Filter {
	private List<Integer> plyList = new ArrayList<Integer>();

	public Filter(int[] filter) {
		plyList = new ArrayList<Integer>();
		for (int i = 0; i < filter.length; i++) {
			plyList.add(filter[i]);
		}
	}

	public List<Integer> getGameList() {
		List<Integer> gameList = new ArrayList<Integer>();
		int gameNo = 0;
		for (Integer i : plyList) {
			if (i != 0) {
				gameList.add(gameNo);
			}
			gameNo++;
		}
		return gameList;
	}

	public int getGameNo(int index) {
		List<Integer> gameList = getGameList();
		if (gameList.size() > index) {
			return gameList.get(index);
		}
		return -1;
	}

	public int getSize() {
		return getGameList().size();
	}

	public int getGamePly(int index) {
		int gameNo = getGameNo(index);
		if (plyList.size() > gameNo) {
			return plyList.get(gameNo);
		}
		return -1;
	}

	public int[] getFilter() {
		final int[] result = new int[this.plyList.size()];
		for (int i = 0; i < this.plyList.size(); i++) {
			result[i] = this.plyList.get(i).intValue();
		}
		return result;
	}
}
