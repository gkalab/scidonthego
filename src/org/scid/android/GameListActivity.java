package org.scid.android;

import java.util.Vector;

import org.scid.database.DataBaseView;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GameListActivity extends ListActivity {
	/**
	 * maximum number of games supported in list
	 *
	 * A list with thousand entries takes long time to load and useless anyway.
	 */
	private static final int MAX_GAMES = 1000;

	private ArrayAdapter<GameInfo> listAdapter;
	final static int PROGRESS_DIALOG = 0;
	private static Vector<GameInfo> gamesInFile = new Vector<GameInfo>();
	private static DataBaseView previousDataBaseView;
	private static int previousGeneration;
	static private String title = "";
	private ProgressDialog progress = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final DataBaseView dbv = ((ScidApplication) getApplicationContext()).getDataBaseView();
		if (dbv == null){
			// TODO: make sure the function can assume this never happen
			finish();
			return;
		}

		listAdapter = new GameListArrayAdapter(this, R.id.item_title);
		setListAdapter(listAdapter);
		showDialog(PROGRESS_DIALOG);

		new Thread(new Runnable() {
			public void run() {
				final int defaultItem = dbv.getPosition();
				readGameInformation(dbv);
				runOnUiThread(new Runnable() {
					public void run() {
						showList(defaultItem);
					}
				});
			}
		}).start();
	}

	private final void showList(int defaultItem) {
		progress.dismiss();
		ListView lv = getListView();
		lv.setSelectionFromTop(defaultItem, 0);
		lv.setFastScrollEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				setResult(RESULT_OK, (new Intent()).setAction("" + pos));
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

	private final void readGameInformation(DataBaseView dbv) {
		if (previousDataBaseView == dbv
				&& previousGeneration == dbv.getGenerationCounter()) {
			if (GameListActivity.title.length() > 0) {
				setTitle(GameListActivity.title);
			}
			runOnUiThread(new Runnable() {
				public void run() {
					for (GameInfo info : gamesInFile) {
						listAdapter.add(info);
					}
				}
			});
			return;
		}

		// need to (re)load
		previousDataBaseView = dbv;
		previousGeneration = dbv.getGenerationCounter();
		gamesInFile.clear();
		int noGames = dbv.getCount();
		if (noGames > MAX_GAMES) {
			// limit games shown in list
			String title = getString(R.string.gamelist) + " - " + MAX_GAMES
					+ "/" + noGames;
			setTitle(title);
			GameListActivity.title = title;
			noGames = MAX_GAMES;
		} else {
			int allGames = dbv.getTotalGamesInFile();
			final String title;
			if (allGames > noGames) {
				// there's currently a filter
				title = getString(R.string.gamelist_filter) + " " + noGames
						+ "/" + allGames;
			} else {
				title = getString(R.string.gamelist);
			}
			runOnUiThread(new Runnable() {
				public void run() {
					setTitle(title);
				}
			});
		}

		gamesInFile.ensureCapacity(noGames);
		int percent = -1;
		if (progress != null)
			progress.setMax(100);
		int savedPosition = dbv.getPosition();
		dbv.setLoadPGN(false); // speeding up the list view
		for (int gameNo = 0; gameNo < noGames; ++gameNo) {
			if(!dbv.moveToPosition(gameNo))
				break;
			addGameInfo(dbv);
			if (progress != null) {
				final int newPercent = (int) (gameNo * 100 / noGames);
				if (newPercent > percent) {
					percent = newPercent;
					runOnUiThread(new Runnable() {
							public void run() {
								progress.setProgress(newPercent);
							}
						});
				}
			}
		}
		dbv.setLoadPGN(true); // re-enable loading of PGN data in dbv
		dbv.moveToPosition(savedPosition);
	}

	private void addGameInfo(DataBaseView dbv) {
		final GameInfo info = new GameInfo(dbv.getGameInfo());
		gamesInFile.add(info);
		runOnUiThread(new Runnable() {
			public void run() {
				listAdapter.add(info);
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to destroy progress dialog in case user turns device
		if (progress != null) {
			progress.dismiss();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (progress != null) {
			progress.dismiss();
		}
	}
}
