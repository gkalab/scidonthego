package org.scid.android;

import java.io.File;
import java.util.Vector;

import org.scid.database.ScidProviderMetaData;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class GameListActivity extends ListActivity {
	/**
	 * maximum number of games supported in list
	 */
	private static final int MAX_GAMES = 5000;

	// TODO: rework game info and game info display
	private static final class GameInfo {
		String summary = "";
		int gameNo = -1;

		public String toString() {
			return summary;
		}
	}

	final static int PROGRESS_DIALOG = 0;
	private static Vector<GameInfo> gamesInFile = new Vector<GameInfo>();
	private String fileName;
	private ProgressDialog progress;
	private static int defaultItem = 0;
	static private long lastModTime = -1;
	static private String lastFileName = "";
	static private Cursor lastCursor = null;
	static private String title = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileName = ((ScidApplication) this.getApplicationContext())
				.getCurrentFileName();
		showDialog(PROGRESS_DIALOG);
		final GameListActivity gameList = this;
		new Thread(new Runnable() {
			public void run() {
				readGameInformation();
				runOnUiThread(new Runnable() {
					public void run() {
						gameList.showList();
					}
				});
			}
		}).start();
	}

	private final void showList() {
		progress.dismiss();
		final ArrayAdapter<GameInfo> aa = new ArrayAdapter<GameInfo>(this,
				R.layout.select_game_list_item, gamesInFile);
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setSelectionFromTop(defaultItem, 0);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				defaultItem = pos;
				setResult(RESULT_OK, (new Intent()).setAction("" + defaultItem));
				finish();
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setTitle(R.string.please_wait);
			progress.setMessage(getString(R.string.gamelist_loading));
			progress.setCancelable(false);
			return progress;
		default:
			return null;
		}
	}

	private final void readGameInformation() {
		if (!fileName.equals(lastFileName)) {
			defaultItem = 0;
		}
		long modTime = new File(fileName).lastModified();
		if ((modTime == lastModTime)
				&& fileName.equals(lastFileName)
				&& lastCursor != null
				&& lastCursor
						.equals(((ScidApplication) getApplicationContext())
								.getGamesCursor())) {
			if (GameListActivity.title.length() > 0) {
				setTitle(GameListActivity.title);
			}
			return;
		}
		lastModTime = modTime;
		lastFileName = fileName;
		lastCursor = ((ScidApplication) getApplicationContext())
				.getGamesCursor();

		gamesInFile.clear();
		Cursor cursor = getCursor();
		if (cursor != null) {
			int noGames = cursor.getCount();
			if (noGames > MAX_GAMES) {
				// limit games shown in list
				String title = getString(R.string.gamelist) + " - " + MAX_GAMES
						+ "/" + noGames;
				setTitle(title);
				GameListActivity.title = title;
				noGames = MAX_GAMES;
			} else {
				int allGames = ((ScidApplication) getApplicationContext())
						.getNoGames();
				if (allGames > noGames) {
					// there's currently a filter
					String title = getString(R.string.gamelist_filter) + " "
							+ noGames + "/" + allGames;
					setTitle(title);
					GameListActivity.title = title;
				} else {
					setTitle(getString(R.string.gamelist));
				}
			}
			progress.setMax(100);
			int percent = -1;
			if (cursor.moveToFirst()) {
				int gameNo = 0;
				addGameInfo(cursor);
				while (gameNo < noGames && cursor.moveToNext()) {
					gameNo++;
					addGameInfo(cursor);
					final int newPercent = (int) (gameNo * 100 / noGames);
					if (newPercent > percent) {
						percent = newPercent;
						if (progress != null) {
							runOnUiThread(new Runnable() {
								public void run() {
									progress.setProgress(newPercent);
								}
							});
						}
					}
				}
			}
		}
	}

	private Cursor getCursor() {
		Cursor cursor = ((ScidApplication) this.getApplicationContext())
				.getGamesCursor();
		if (cursor != null) {
			startManagingCursor(cursor);
		}
		return cursor;
	}

	private void addGameInfo(Cursor cursor) {
		GameInfo gi = new GameInfo();
		final int gameNo = cursor.getInt(cursor.getColumnIndex("_id"));
		gi.gameNo = gameNo;
		gi.summary = cursor.getString(cursor
				.getColumnIndex(ScidProviderMetaData.ScidMetaData.SUMMARY));
		gamesInFile.add(gi);
	}
}
