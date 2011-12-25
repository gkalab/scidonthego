package org.scid.android.chessok;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.scid.android.DownloadTask;
import org.scid.android.IDownloadCallback;
import org.scid.android.Link;
import org.scid.android.R;
import org.scid.android.ScidAndroidActivity;
import org.scid.android.Tools;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PgnLinkListActivity extends ListActivity implements IDownloadCallback {
	private static ProgressDialog progressDlg;
	final static int PROGRESS_DIALOG = 0;
	static private final int RESULT_PGN_IMPORT = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LinkList linkList = (LinkList) this.getIntent().getSerializableExtra(
				"linklist");
		final PgnLinkListActivity chessOkList = this;
		progressDlg = ProgressDialog.show(this,
				getString(R.string.get_chessok_information),
				getString(R.string.downloading), true, false);
		chessOkList.showList(linkList.getLinkList());
	}

	protected void showList(final List<Link> linkList) {
		progressDlg.dismiss();
		final ArrayAdapter<Link> aa = new LinkListArrayAdapter(this,
				R.id.item_title);
		for (Link link : linkList) {
			aa.add(link);
		}
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				Link item = aa.getItem(pos);
				PgnLinkListActivity.progressDlg = ProgressDialog.show(PgnLinkListActivity.this,
						getString(R.string.please_wait),
						getString(R.string.downloading), true, false);
				new DownloadTask().execute(PgnLinkListActivity.this,
						item.getLink());
			}
		});
	}

	private void replacePgn(final String fileName) {
		try {
			final FileInputStream f = new FileInputStream(fileName);
			final byte b[] = new byte[f.available()];
			f.read(b);
			f.close();
			final String pgn = ChessOkCommentReplacer.replace(new String(b));
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(fileName));
				writer.write(pgn);
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch (final IOException e) {
				}
			}

		} catch (final FileNotFoundException e) {
			Log.e("SCID", "replacing PGN comments: file not found", e);
		} catch (final IOException e) {
			Log.e("SCID", "IOException while replacing PGN comments", e);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to destroy progress dialog in case user turns device
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_PGN_IMPORT:
			if (resultCode == RESULT_OK) {
				// delete file if import was successful
				if (data != null) {
					String pgnFileName = data.getAction();
					if (pgnFileName != null) {
						new File(pgnFileName).delete();
					}
				}
			}
			break;
		}
	}

	@Override
	public void downloadSuccess(File pgnFile) {
		progressDlg.dismiss();
		if (pgnFile != null) {
			if (pgnFile.length() == 0) {
				pgnFile.delete();
				Tools.showErrorMessage(PgnLinkListActivity.this,
						getString(R.string.download_error_file_empty));
			} else {
				Log.d("SCID", "replacing comments");
				replacePgn(pgnFile.getAbsolutePath());
				String pgnFileName = pgnFile.getName();
				Log.d("SCID",
						"moving downloaded file from "
								+ pgnFile.getAbsolutePath()
								+ " to "
								+ Environment
										.getExternalStorageDirectory()
								+ File.separator
								+ ScidAndroidActivity.SCID_DIRECTORY
								+ File.separator + pgnFileName);
				// move to scid directory and rename to ... name +
				// ".pgn"
				pgnFile.renameTo(new File(Environment
						.getExternalStorageDirectory()
						+ File.separator
						+ ScidAndroidActivity.SCID_DIRECTORY,
						pgnFileName));
				Tools.importPgn(PgnLinkListActivity.this,
						Tools.getFullScidFileName(pgnFileName),
						RESULT_PGN_IMPORT);
			}
		} 		
	}

	@Override
	public void downloadFailure(String message) {
		progressDlg.dismiss();
		Tools.showErrorMessage(this, this
				.getText(R.string.download_error)
				+ " (" + message + ")");
	}
}
