package org.scid.android;

import java.io.File;
import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class PgnLinkListActivity extends ListActivity {
	private ProgressDialog progressDlg;
	final static int PROGRESS_DIALOG = 0;
	static private final int RESULT_PGN_IMPORT = 5;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ArrayList<String> linkList = this.getIntent().getStringArrayListExtra(
				"linklist");
		ArrayList<String> linkDescription = this.getIntent()
				.getStringArrayListExtra("linkdescription");
		final PgnLinkListActivity chessOkList = this;
		this.progressDlg = ProgressDialog.show(this,
				getString(R.string.get_chessok_information),
				getString(R.string.downloading), true, false);
		chessOkList.showList(linkList, linkDescription);
	}

	protected void showList(final ArrayList<String> linkList,
			ArrayList<String> linkDescription) {
		progressDlg.dismiss();
		final ArrayAdapter<Link> aa = new LinkListArrayAdapter(this, R.id.item_title);
		for (int i=0;i<linkList.size();i++) {
			aa.add(new Link(linkList.get(i), linkDescription.get(i)));
		}
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				Link item = aa.getItem(pos);
				File pgnFile = Tools.downloadFile(item.getLink());
				if (pgnFile != null) {
					String pgnFileName = pgnFile.getName();
					Log.d("SCID", "moving downloaded file from "
							+ pgnFile.getAbsolutePath() + " to "
							+ Environment.getExternalStorageDirectory()
							+ File.separator + ScidAndroidActivity.SCID_DIRECTORY
							+ File.separator + pgnFileName);
					// move to scid directory and rename to ... name +
					// ".pgn"
					pgnFile.renameTo(new File(Environment
							.getExternalStorageDirectory()
							+ File.separator + ScidAndroidActivity.SCID_DIRECTORY,
							pgnFileName));
					Tools.importPgn(PgnLinkListActivity.this,
							getFullScidFileName(pgnFileName), true,
							RESULT_PGN_IMPORT);

				} else {
					Toast.makeText(getApplicationContext(),
							getText(R.string.download_error),
							Toast.LENGTH_LONG).show();
				}
			}
		});
		// TODO: process RESULT_PGN_IMPORT --> remove pgn after successful download
	}
	// TODO: move to Tools
	private String getFullScidFileName(final String fileName) {
		String pathName = getFullFileName(fileName);
		String scidFileName = pathName.substring(0, pathName.indexOf("."));
		return scidFileName;
	}
	// TODO: move to Tools
	private String getFullFileName(final String fileName) {
		String sep = File.separator;
		String pathName = Environment.getExternalStorageDirectory() + sep
				+ ScidAndroidActivity.SCID_DIRECTORY + sep + fileName;
		return pathName;
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
