package org.scid.android.twic;

import java.io.File;

import org.scid.android.IDownloadCallback;
import org.scid.android.R;
import org.scid.android.Tools;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ImportTwicActivity extends ListActivity implements
		IDownloadCallback {
	private ProgressDialog progressDlg;
	private TwicDownloader downloader;
	private static final int RESULT_PGN_IMPORT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ImportTwicActivity twicList = this;
		this.progressDlg = ProgressDialog.show(this,
				getString(R.string.get_twic_information),
				getString(R.string.downloading), true, false);
		new Thread(new Runnable() {

			public void run() {
				downloader = new TwicDownloader();
				downloader.parseTwicSite();
				if (downloader.getLinkList().isEmpty()) {
					if (progressDlg != null && progressDlg.isShowing()) {
						progressDlg.dismiss();
					}
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
		if (progressDlg != null && progressDlg.isShowing()) {
			progressDlg.dismiss();
		}
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
						getString(R.string.twic_downloading),
						getString(R.string.downloading), true, false);
				new ImportZipTask().execute(ImportTwicActivity.this,
						progressDlg, item.getLink());
			}
		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to destroy progress dialog in case user turns device
		if (progressDlg != null && progressDlg.isShowing()) {
			progressDlg.dismiss();
		}
	}

	@Override
	public void downloadSuccess(File pgnFile) {
		Tools.importPgnFile(ImportTwicActivity.this, pgnFile, RESULT_PGN_IMPORT);
	}

	@Override
	public void downloadFailure(String message) {
		Tools.showErrorMessage(this, this.getText(R.string.download_error)
				+ " (" + message + ")");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_PGN_IMPORT:
			// the result after importing the pgn file - delete pgn file
			if (resultCode == RESULT_OK && data != null) {
				String pgnFileName = data.getAction();
				if (pgnFileName != null) {
					new File(pgnFileName).delete();
				}
			}
			break;
		}
	}
}
