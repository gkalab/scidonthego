package org.scid.database;

public class Filter {
	int[] plyList = {};
	int[] gameList = {};

	public Filter(int[] filter) {
		if (filter != null) {
			plyList = filter;
			int count = 0;
			for (int i = 0; i < plyList.length; i++) {
				if (plyList[i] > 0) {
					count++;
				}
			}
			gameList = new int[count];
			int j = 0;
			for (int i = 0; i < plyList.length; i++) {
				if (plyList[i] > 0) {
					gameList[j] = i;
					j++;
				}
			}
		}
	}

	public int getGameNo(int index) {
		if (gameList.length > index) {
			return gameList[index];
		}
		return -1;
	}

	public int getSize() {
		return gameList.length;
	}

	public int getGamePly(int index) {
		int gameNo = getGameNo(index);
		if (plyList.length > gameNo) {
			return plyList[gameNo];
		}
		return -1;
	}

	public int[] getFilter() {
		return plyList;
	}
}
