package org.scid.android;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ImportTwicActivity extends ListActivity {
	private ProgressDialog progressDlg;
	private TwicDownloader downloader;
	final static int PROGRESS_DIALOG = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ImportTwicActivity twicList = this;
		this.progressDlg = ProgressDialog.show(this,
				"Getting information from TWIC", "Downloading...", true, false);
		new Thread(new Runnable() {

			public void run() {
				downloader = new TwicDownloader();
				downloader.parseTwicSite();
				if (downloader.getLinkList().isEmpty()) {
					progressDlg.dismiss();
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(),
									getText(R.string.download_error),
									Toast.LENGTH_LONG).show();
						}
					});
					setResult(RESULT_CANCELED);
					finish();
				}
				runOnUiThread(new Runnable() {
					public void run() {
						twicList.showList();
					}
				});
			}
		}).start();
	}

	protected void showList() {
		progressDlg.dismiss();
		final ArrayAdapter<TwicItem> aa = new ArrayAdapter<TwicItem>(this,
				android.R.layout.simple_list_item_1, downloader.getLinkList());
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				TwicItem item = aa.getItem(pos);
				progressDlg = ProgressDialog.show(ImportTwicActivity.this,
						"Downloading from TWIC", "Downloading...", true, false);
				AsyncTask task = new ImportTwicTask().execute(
						ImportTwicActivity.this, progressDlg, item.getLink());
			}
		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to distroy progress dialog in case user turns device
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}
}
