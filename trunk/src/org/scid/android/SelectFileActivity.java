package org.scid.android;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
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
	private static Stack<String> path = new Stack<String>();
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
		Intent i = getIntent();
		String extension = i.getAction();
		if (extension != null && extension.length() != 0) {
			this.extension = extension;
		} else {
			this.extension = "*";
		}
		path = getStringStackPref(this, "lastPath");
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		defaultItem = preferences.getInt("lastPathDefaultItem", 0);
		final SelectFileActivity fileList = this;
		File scidFileDir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + ScidAndroidActivity.SCID_DIRECTORY);
		if (!scidFileDir.exists()) {
			scidFileDir.mkdirs();
		}
		fileList.showList();
	}

	protected void showList() {
		listAdapter = new FileListArrayAdapter(this,
				R.layout.select_file_list_item, R.id.select_file_label,
				new ArrayList<String>());
		setListAdapter(listAdapter);
		List<String> fileNames = changePath();
		if (path.size() == 0 && fileNames.size() == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name).setMessage(
					R.string.no_scid_files);
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
		ListView lv = getListView();
		lv.setSelectionFromTop(defaultItem, 0);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				if (pos >= 0 && pos < listAdapter.getCount()) {
					String item = listAdapter.getItem(pos);
					if (path.size() > 0 && item.equals(PARENT_FOLDER)) {
						SelectFileActivity.path.pop();
						changePath();
						return;
					}
					defaultItem = pos;
					File itemFile = new File(item);
					if (itemFile.isDirectory()) {
						path.add(item);
						changePath();
					} else {
						setResult(Activity.RESULT_OK,
								(new Intent()).setAction(item));
						SharedPreferences preferences = PreferenceManager
								.getDefaultSharedPreferences(SelectFileActivity.this);
						Editor editor = preferences.edit();
						editor.putInt("lastPathDefaultItem", defaultItem);
						editor.commit();
						setStringStackPref(SelectFileActivity.this, "lastPath",
								path);
						finish();
					}
				}
			}
		});
	}

	private String getFullPath() {
		String pathName;
		if (path.size() > 0) {
			pathName = path.lastElement();
		} else {
			pathName = Environment.getExternalStorageDirectory()
					+ File.separator + ScidAndroidActivity.SCID_DIRECTORY
					+ File.separator;
		}
		return pathName;
	}

	private List<String> changePath() {
		TextView titleView = (TextView) findViewById(android.R.id.title);
		if (titleView != null) {
			String breadcrumb = "/";
			for (String crumb : path) {
				breadcrumb += new File(crumb).getName() + "/";
			}
			titleView.setText(breadcrumb);
		}
		listAdapter.clear();
		List<String> newFileNames = findFilesInDirectory(getFullPath(),
				this.extension);
		if (path.size() > 0) {
			listAdapter.add(PARENT_FOLDER);
		}
		for (String fileName : newFileNames) {
			listAdapter.add(fileName);
		}
		return newFileNames;
	}

	private List<String> findFilesInDirectory(String dirName,
			final String extension) {
		File dir = new File(dirName);
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isFile()
						&& (pathname.getAbsolutePath().endsWith(
								extension.toLowerCase())
								|| pathname.getAbsolutePath().endsWith(
										extension.toUpperCase()) || extension
								.equals("*"));
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
		List<String> resultList = new ArrayList<String>(Arrays.asList(dirNames));
		resultList.addAll(Arrays.asList(fileNames));
		return resultList;
	}

	private void setStringStackPref(Context context, String key,
			Stack<String> values) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		JSONArray a = new JSONArray();
		for (int i = 0; i < values.size(); i++) {
			a.put(values.get(i));
		}
		if (!values.isEmpty()) {
			editor.putString(key, a.toString());
		} else {
			editor.putString(key, null);
		}
		editor.commit();
	}

	private Stack<String> getStringStackPref(Context context, String key) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String json = prefs.getString(key, null);
		Stack<String> result = new Stack<String>();
		if (json != null) {
			try {
				JSONArray a = new JSONArray(json);
				for (int i = 0; i < a.length(); i++) {
					String url = a.optString(i);
					result.add(url);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
