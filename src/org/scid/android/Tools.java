package org.scid.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Tools {
	private static Matcher matcherTag;
	private static Matcher matcherLink;
	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	private static Pattern patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
	private static Pattern patternLink = Pattern
			.compile(HTML_A_HREF_TAG_PATTERN);

	/**
	 * Extract links from html using regular expressions
	 * 
	 * @param html
	 *            html content for validation
	 * @return List of links
	 */
	public static List<Link> getLinks(final String html) {
		List<Link> result = new ArrayList<Link>();
		matcherTag = patternTag.matcher(html);
		while (matcherTag.find()) {
			String anchor = matcherTag.group(0);
			final String description = anchor.substring(
					anchor.indexOf(">") + 1, anchor.lastIndexOf("<"));
			String href = matcherTag.group(1); // href
			matcherLink = patternLink.matcher(href);
			while (matcherLink.find()) {
				final Link link = new Link(matcherLink.group(1).replaceAll(
						"\"", ""), description); // link
				result.add(link);
			}
		}
		return result;
	}

	/**
	 * Add names of engine files (files which not have an ignored extension) to
	 * the specified set of already found engines.
	 * 
	 * @param foundEngines
	 *            Set of already found engines or null if a new set should be
	 *            created.
	 * @param dirPath
	 *            Path of directory to search.
	 * @param ignoreExtensions
	 *            Extensions (including the period) of non-engine files.
	 * @return Updated or created set of engine file names.
	 */
	public static final SortedSet<String> findEnginesInDirectory(
			String dirPath, Set<String> ignoreExtensions) {
		File dir = new File(dirPath);
		final Set<String> _ignore = ignoreExtensions;
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				if (pathname.isFile() && !pathname.getName().startsWith(".")) {
					int index = pathname.getName().lastIndexOf('.');
					if (index >= 0) {
						String ext = pathname.getName().substring(index);
						if (_ignore != null && _ignore.contains(ext)) {
							return false;
						}
					}
					return true;
				}
				return false;
			}
		});
		SortedSet<String> engines = new TreeSet<String>();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				engines.add(files[i].getName());
			}
		}
		return engines;
	}

	/**
	 * Download file to scid directory with file name from HTTP header or create
	 * temp file if the HTTP header does not provide enough information
	 * 
	 * @param path
	 *            the path to the URL
	 * @return the downloaded file
	 * @throws IOException
	 *             if there's an error downloading the file
	 */
	public static final File downloadFile(String path) throws IOException {
		File result = null;
		URL url = null;
		try {
			url = new URL(path);
			Log.d("SCID", "start downloading from: " + url.toString());
			URLConnection uc;
			uc = url.openConnection();
			String contentType = uc.getContentType();
			// get file name from http headers
			String fileName = uc.getHeaderField("Content-Disposition");
			if (fileName != null) {
				if (fileName.indexOf("filename=") > -1) {
					fileName = fileName.substring(
							fileName.indexOf("filename=") + 9).trim();
					if (fileName.length() > 0)
						result = new File(Environment
								.getExternalStorageDirectory()
								+ File.separator
								+ ScidAndroidActivity.SCID_DIRECTORY
								+ File.separator + fileName);
				}
			} else {
				fileName = getFileNameFromUrl(path);
				if (fileName != null) {
					result = new File(Environment.getExternalStorageDirectory()
							+ File.separator
							+ ScidAndroidActivity.SCID_DIRECTORY
							+ File.separator + fileName);
				}
			}
			Log.d("SCID", "fileName: " + result);
			if (contentType == null) {
				throw new IOException("URL not available.");
			}
			InputStream raw = uc.getInputStream();
			InputStream in = new BufferedInputStream(raw);

			int downloadedSize = 0;
			byte[] buffer = new byte[1024];
			int bufferLength = 0;
			if (result == null) {
				result = File.createTempFile("temp", ".tmp");
			}
			FileOutputStream out = new FileOutputStream(result
					.getAbsolutePath());
			while ((bufferLength = in.read(buffer)) > 0) {
				out.write(buffer, 0, bufferLength);
				downloadedSize += bufferLength;
			}
			// TODO: compare downloadedSize with content size if content
			// size > 0
			in.close();
			// TODO: check if there was something written to the file,
			// otherwise delete file and return null
			out.flush();
			out.close();
		} catch (MalformedURLException e) {
			Log.e("SCID", e.getMessage(), e);
		}
		return result;
	}

	public static void importPgn(final Activity activity, String baseName,
			final int resultId) {
		final String pgnFileName;
		if (baseName.endsWith(".pgn")) {
			pgnFileName = baseName;
		} else {
			pgnFileName = baseName + ".pgn";
		}
		String scidFileName = pgnFileName.replace(".pgn", ".si4");
		File scidFile = new File(scidFileName);
		if (scidFile.exists()) {
			final AlertDialog fileExistsDialog = new AlertDialog.Builder(
					activity).create();
			fileExistsDialog.setTitle("Database exists");
			String message = String.format(activity
					.getString(R.string.pgn_import_db_exists), scidFile
					.getName());
			fileExistsDialog.setMessage(message);
			fileExistsDialog.setIcon(android.R.drawable.ic_dialog_alert);
			fileExistsDialog.setButton(activity.getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							startPgnImport(activity, pgnFileName, resultId);
						}
					});
			fileExistsDialog.setButton2(activity.getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Toast
									.makeText(
											activity.getApplicationContext(),
											activity
													.getString(R.string.pgn_import_cancel),
											Toast.LENGTH_SHORT).show();
						}
					});
			fileExistsDialog.show();
		} else {
			startPgnImport(activity, pgnFileName, resultId);
		}
	}

	private static void startPgnImport(Activity activity, String pgnFileName,
			final int resultId) {
		Intent i = new Intent(activity, ImportPgnActivity.class);
		i.setAction(pgnFileName);
		activity.startActivityForResult(i, resultId);
	}

	public static String getFullScidFileName(final String fileName) {
		String pathName = getFullFileName(fileName);
		return stripExtension(pathName);
	}

	public static String stripExtension(String pathName) {
		int pos = pathName.lastIndexOf(".");
		if (pos > 0) {
			pathName = pathName.substring(0, pathName.indexOf("."));
		}
		return pathName;
	}

	private static String getFullFileName(final String fileName) {
		String sep = File.separator;
		String pathName = Environment.getExternalStorageDirectory() + sep
				+ ScidAndroidActivity.SCID_DIRECTORY + sep + fileName;
		return pathName;
	}

	public static String getFileNameFromUrl(String path) {
		String result = null;
		int lastPathSep = path.lastIndexOf("/");
		if (lastPathSep > 0 && path.length() > lastPathSep + 1) {
			result = path.substring(lastPathSep + 1);
		}
		if (result != null) {
			int lastEquals = result.lastIndexOf("=");
			if (lastEquals > 0 && result.length() > lastEquals + 1) {
				result = result.substring(lastEquals + 1);
			}
		}
		return result;
	}

	public static void showErrorMessage(final Activity activity,
			final String message) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						activity);
				builder.setTitle(activity.getString(R.string.error));
				builder.setMessage(message);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder
						.setPositiveButton(activity.getString(R.string.ok),
								null);
				builder.show();
			}
		});
	}

	public static void bringPointtoView(TextView textView,
			ScrollView scrollView, int offset) {
		if (textView.getLayout() != null) {
			int line = textView.getLayout().getLineForOffset(offset);
			int y = (int) ((line + 0.5) * textView.getLineHeight());
			scrollView.smoothScrollTo(0, y - scrollView.getHeight() / 2);
		}
	}

	public static void setKeepScreenOn(Activity activity, boolean alwaysOn) {
		if (alwaysOn) {
			activity.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity
					.getWindow()
					.clearFlags(
							android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	public static boolean copyFile(File sourceFile, File destFile) {
		FileInputStream istream = null;
		FileOutputStream fout = null;
		String errorMsg;
		boolean copied = false;
		try {
			istream = new FileInputStream(sourceFile);
			fout = new FileOutputStream(destFile);
			byte[] b = new byte[4096];
			int cnt = 0;
			while ((cnt = istream.read(b)) != -1) {
				fout.write(b, 0, cnt);
			}
			istream.close();
			fout.close();
			copied = true;
		} catch (IOException e) {
			errorMsg = e.getLocalizedMessage();
			Log.e("SCID", errorMsg, e);
		} catch (SecurityException se) {
			errorMsg = se.getLocalizedMessage();
			Log.e("SCID", errorMsg, se);
		} finally {
			// Ensure streams are closed should an exception occur.
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) { /* Ignore */
				}
			}
			if (istream != null) {
				try {
					istream.close();
				} catch (IOException e) { /* Ignore */
				}
			}
		}
		return copied;
	}
}
