package org.scid.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ImportPgnFileActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ImportPgnFileActivity pgnFileList = this;
		pgnFileList.showList();
	}

	protected void showList() {
		final String[] fileNames = Tools.findFilesInDirectory(
				ScidAndroidActivity.SCID_DIRECTORY, ".pgn");
		if (fileNames.length == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name).setMessage(
					R.string.no_pgn_files);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							return;
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}
		final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, fileNames);
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				String item = aa.getItem(pos);
				setResult(Activity.RESULT_OK, (new Intent()).setAction(item));
				finish();
			}
		});
	}
}
