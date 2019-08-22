package org.scid.database;

import java.util.Arrays;

import android.util.Log;

public class GameFilter {
	private int[] id; // id[position]
	private short[] ply; // ply[position]

	/** Create Filter from mask (full-size array with ply or 0 if game is not included */
	GameFilter(short[] plyById) {
		if (plyById == null) { // TODO: this should not be needed
			id = new int[0];
			ply = new short[0];
		} else	{
			int count = 0;
			for (short value : plyById) {
				if (value > 0) {
					count++;
				}
			}
			id = new int[count];
			ply = new short[count];
			for (int i = 0, j = 0; i < plyById.length; i++) {
				if (plyById[i] > 0) {
					id[j] = i;
					ply[j] = plyById[i];
					j++;
				}
			}
		}
	}

	/** Create Filter from list of IDs (the list must be ascending): all ply are assumed to be 1 */
	GameFilter(int[] id) {
		this.id = id;
		ply = new short[id.length];
		for (int i = 0; i < ply.length; i++) {
			ply[i] = 1;
		}
	}

	public int getSize() {
		return id.length;
	}

	public int getGameId(int position) {
		if (position < 0 || position >= id.length) { // TODO: this should not be needed
			Log.e("GameFilter", "getGameId: bad position " + position);
			return -1;
		} else {
			return id[position];
		}
	}

	public int getGamePly(int position) {
		if (position < 0 || position >= ply.length) { // TODO: this should not be needed
			Log.e("GameFilter", "getGamePly: bad position " + position);
			return -1;
		} else {
			return ply[position];
		}
	}

	/** returns negative value if id is not present */
	int getPosition(int id) {
		return Arrays.binarySearch(this.id, id);
	}

	/** Prepare a filtering array for a possibly null Filter
	 * (if filter is null, then all games are included) */
	static short[] getFilterArray(GameFilter f, int length) {
		short[] result = new short[length];
		if (f == null) {
			for (int i = 0; i < result.length; i++)
				result[i] = 1;
		} else {
			for (int i = 0; i < f.id.length; i++)
				result[f.id[i]] = f.ply[i];
		}
		return result;
	}
}
