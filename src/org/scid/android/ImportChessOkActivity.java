package org.scid.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
	// TODO: allow downloading of all links in one category into one database
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
				final Map<String, List<Link>> linkMap = downloader
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

	protected void showList(final Map<String, List<Link>> linkMap) {
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
						PgnLinkListActivity.class);
				List<String> linkList = new ArrayList<String>();
				List<String> descList = new ArrayList<String>();
				for (Link link:linkMap.get(item)){
					linkList.add(link.getLink());
					descList.add(link.toString());
				}
				intent.putStringArrayListExtra("linklist", (ArrayList<String>)linkList);
				intent.putStringArrayListExtra("linkdescription", (ArrayList<String>)descList);
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
