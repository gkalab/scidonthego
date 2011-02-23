package org.scid.android;

import java.io.File;
import java.util.Vector;

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
	private static final class GameInfo {
		String summary = "";
		int gameNo = -1;

		public String toString() {
			return summary;
		}
	}

	private static Vector<GameInfo> gamesInFile = new Vector<GameInfo>();
	private String fileName;
	private ProgressDialog progress;
	private static int defaultItem = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileName = ((ScidApplication) this.getApplicationContext())
				.getCurrentFileName();
		showDialog(PROGRESS_DIALOG);
		final GameListActivity gameList = this;
		new Thread(new Runnable() {
			public void run() {
				readFile();
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
				sendBackResult(aa.getItem(pos));
			}
		});
	}

	final static int PROGRESS_DIALOG = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setTitle(R.string.please_wait);
			progress.setMessage(getString(R.string.please_wait));
			progress.setCancelable(false);
			return progress;
		default:
			return null;
		}
	}

	static long lastModTime = -1;
	static String lastFileName = "";

	private final void readFile() {
		if (!fileName.equals(lastFileName))
			defaultItem = 0;
		long modTime = new File(fileName).lastModified();
		if ((modTime == lastModTime) && fileName.equals(lastFileName))
			return;
		lastModTime = modTime;
		lastFileName = fileName;

		gamesInFile.clear();
		Cursor cursor = getCursor();
		if (cursor != null) {
			int noGames = cursor.getCount();
			progress.setMax(noGames);
			int percent = -1;
			if (cursor.moveToFirst()) {
				addGameInfo(cursor);
				int gameNo = 1;
				while (cursor.moveToNext()) {
					addGameInfo(cursor);
					gameNo++;
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
		startManagingCursor(cursor);
		return cursor;
	}

	private void addGameInfo(Cursor cursor) {
		GameInfo gi = new GameInfo();
		final int gameNo = cursor.getInt(cursor.getColumnIndex("_id"));
		gi.gameNo = gameNo;
		gi.summary = cursor.getString(cursor.getColumnIndex("summary"));
		gamesInFile.add(gi);
	}

	private final void sendBackResult(GameInfo gi) {
		if (gi.gameNo >= 0) {
			setResult(RESULT_OK, (new Intent()).setAction("" + gi.gameNo));
			finish();
			return;
		}
		setResult(RESULT_CANCELED);
		finish();
	}
}
