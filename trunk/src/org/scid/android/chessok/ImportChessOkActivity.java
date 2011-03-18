package org.scid.android.chessok;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.scid.android.Link;
import org.scid.android.R;
import org.scid.android.R.string;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ImportChessOkActivity extends ListActivity {
	private ProgressDialog progressDlg;
	private ChessOkDownloader downloader;
	final static int PROGRESS_DIALOG = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ImportChessOkActivity chessOkList = this;
		this.progressDlg = ProgressDialog.show(this,
				getString(R.string.get_chessok_information),
				getString(R.string.downloading), true, false);
		new Thread(new Runnable() {

			public void run() {
				downloader = new ChessOkDownloader();
				final Map<String, Map<String, List<Link>>> linkMap = downloader
						.parseChessOkSite();
				if (linkMap.isEmpty()) {
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
						chessOkList.showList(linkMap);
					}
				});
			}
		}).start();
	}

	protected void showList(final Map<String, Map<String, List<Link>>> linkMap) {
		progressDlg.dismiss();
		final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new Vector<String>(linkMap
						.keySet()));
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				String item = aa.getItem(pos);
				Intent intent = new Intent(ImportChessOkActivity.this,
						ChessOkLinkMapActivity.class);
				
				intent.putExtra("linkmap", new LinkMap(linkMap.get(item)));
				startActivity(intent);
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
