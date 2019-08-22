package org.scid.android;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SelectFileActivity extends ListActivity {

	public static final String PARENT_FOLDER = ".. (parent folder)";
	private String currentPath;
	private ArrayAdapter<String> listAdapter;
	private String extension;
	private int defaultItem = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filelist);
		View title = findViewById(android.R.id.title);
		if (title instanceof TextView) {
			TextView titleText = (TextView) title;
			titleText.setEllipsize(TruncateAt.START);
		}
		Intent intent = getIntent();
		String extension = intent.getAction();
		if (extension != null && extension.length() != 0) {
			this.extension = extension;
		} else {
			this.extension = "*";
		}
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		defaultItem = preferences.getInt("lastPathDefaultItem", 0);
		File scidFileDir = new File(Tools.getScidDirectory());
		if (!scidFileDir.exists()) {
			scidFileDir.mkdirs();
		}
		String lastPathKey = "lastUsedPath" + extension;
		getPath(lastPathKey);
		showList(lastPathKey);
	}

	private void getPath(String lastPathKey) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		currentPath = preferences.getString(lastPathKey,
				Tools.getScidDirectory());
	}

	private void showList(final String lastPathKey) {
		listAdapter = new FileListArrayAdapter(this,
				R.layout.select_file_list_item, R.id.select_file_label,
				new ArrayList<String>());
		setListAdapter(listAdapter);

		changePath();

		ListView lv = getListView();
		lv.setSelectionFromTop(defaultItem, 0);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				if (pos >= 0 && pos < listAdapter.getCount()) {
					String item = listAdapter.getItem(pos);
					if (item.equals(PARENT_FOLDER)) {
						currentPath = new File(currentPath).getParent();
						changePath();
						return;
					}
					defaultItem = pos;
					File itemFile = new File(item);
					if (itemFile.isDirectory()) {
						currentPath = itemFile.getAbsolutePath();
						changePath();
					} else {
						setResult(Activity.RESULT_OK,
								(new Intent()).setAction(item));
						SharedPreferences preferences = PreferenceManager
								.getDefaultSharedPreferences(SelectFileActivity.this);
						Editor editor = preferences.edit();
						editor.putInt("lastPathDefaultItem", defaultItem);
						editor.putString(lastPathKey, currentPath);
						editor.commit();
						finish();
					}
				}
			}
		});
	}

	private String getFullPath() {
		return new File(currentPath).getAbsolutePath();
	}

	private void changePath() {
		setTitle(getFullPath());
		listAdapter.clear();
		List<String> newFileNames = findFilesInDirectory(getFullPath(),
				this.extension);
		if (!currentPath.equals(File.separator)) {
			listAdapter.add(PARENT_FOLDER);
		}
		for (String fileName : newFileNames) {
			listAdapter.add(fileName);
		}
		listAdapter.notifyDataSetChanged();
	}

	private List<String> findFilesInDirectory(String dirName,
			final String extension) {
		File dir = new File(dirName);
		File[] files = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				for (String ex : extension.split("\\|")) {
					if (pathname.isFile()
							&& (pathname
									.getName()
									.toLowerCase(Locale.getDefault())
									.endsWith(
											ex.toLowerCase(Locale.getDefault())) || extension
									.equals("*"))) {
						return true;
					}
				}
				return false;
			}
		});
		if (files == null) {
			files = new File[0];
		}
		File[] dirs = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if (dirs == null) {
			dirs = new File[0];
		}
		String[] fileNames = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			fileNames[i] = files[i].getAbsolutePath();
		}
		String[] dirNames = new String[dirs.length];
		for (int i = 0; i < dirs.length; i++) {
			dirNames[i] = dirs[i].getAbsolutePath();
		}
		Arrays.sort(dirNames, String.CASE_INSENSITIVE_ORDER);
		Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
		List<String> resultList = new ArrayList<>(Arrays.asList(dirNames));
		resultList.addAll(Arrays.asList(fileNames));
		return resultList;
	}
}
