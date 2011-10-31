package org.scid.android;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LoadScidFileActivity extends ListActivity {

	private Stack<String> path = new Stack<String>();
	private ArrayAdapter<String> listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final LoadScidFileActivity fileList = this;
		fileList.showList();
	}

	protected void showList() {
		final List<String> fileNames = this.findFilesInDirectory(
				this.getFullPath(), ".si4");
		if (fileNames.size() == 0) {
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
		listAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, fileNames);
		setListAdapter(listAdapter);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				String item = listAdapter.getItem(pos);
				if (item.endsWith(".si4")) {
					setResult(Activity.RESULT_OK,
							(new Intent()).setAction(getFullPath() + item));
					finish();
				} else {
					// must be a directory
					path.add(item);
					changePath();
				}
			}
		});
	}

	private String getFullPath() {
		String pathName = Environment.getExternalStorageDirectory()
				+ File.separator + ScidAndroidActivity.SCID_DIRECTORY
				+ File.separator;
		for (String part : this.path) {
			pathName += part + File.separator;
		}
		return pathName;
	}

	private void changePath() {
		listAdapter.clear();
		List<String> newFileNames = findFilesInDirectory(getFullPath(), ".si4");
		for (String fileName : newFileNames) {
			listAdapter.add(fileName);
		}
	}

	private List<String> findFilesInDirectory(String dirName,
			final String extension) {
		File dir = new File(dirName);
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isFile()
						&& (pathname.getAbsolutePath().endsWith(extension));
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
			fileNames[i] = files[i].getName();
		}
		String[] dirNames = new String[dirs.length];
		for (int i = 0; i < dirs.length; i++) {
			dirNames[i] = dirs[i].getName();
		}
		Arrays.sort(dirNames, String.CASE_INSENSITIVE_ORDER);
		Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
		List<String> resultList = new ArrayList<String>(Arrays.asList(dirNames));
		resultList.addAll(Arrays.asList(fileNames));
		return resultList;
	}

	@Override
	public void onBackPressed() {
		if (this.path.size() > 0) {
			this.path.pop();
			changePath();
		} else {
			super.onBackPressed();
		}
	}
}
