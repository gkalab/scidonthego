package org.scid.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Environment;
import android.util.Log;

public class Tools {
	private static Matcher matcherTag;
	private static Matcher matcherLink;
	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	private static Pattern patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
	private static Pattern patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);

	/**
	 * Extract links from html using regular expressions
	 * 
	 * @param html
	 *            html content for validation
	 * @return List of links
	 */
	public static List<String> getLinks(final String html) {
		List<String> result = new ArrayList<String>();
		matcherTag = patternTag.matcher(html);
		while (matcherTag.find()) {
			String href = matcherTag.group(1); // href
			matcherLink = patternLink.matcher(href);
			while (matcherLink.find()) {
				String link = matcherLink.group(1); // link
				result.add(link);
			}
		}
		return result;
	}
	
	
	public static final String[] findFilesInDirectory(String dirName,
			final String extension) {
		File extDir = Environment.getExternalStorageDirectory();
		String sep = File.separator;
		File dir = new File(extDir.getAbsolutePath() + sep + dirName);
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isFile()
						&& (pathname.getAbsolutePath().endsWith(extension));
			}
		});
		if (files == null) {
			files = new File[0];
		}
		final int numFiles = files.length;
		String[] fileNames = new String[numFiles];
		for (int i = 0; i < files.length; i++) {
			fileNames[i] = files[i].getName();
		}
		Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
		return fileNames;
	}

	/**
	 * Download file to scid directory with file name from HTTP header or create
	 * temp file if the HTTP header does not provide enough information
	 * 
	 * @param path
	 *            the path to the URL
	 * @return the downloaded file
	 */
	public static final File downloadFile(String path) {
		File result = null;
		URL url = null;
		try {
			url = new URL(path);
			Log.d("SCID", "start downloading from: " + url.toString());
			URLConnection uc;
			try {
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
					int lastPathSep = path.lastIndexOf("/");
					if (lastPathSep > 0 && path.length() > lastPathSep + 1) {
						fileName = path.substring(lastPathSep + 1);
						result = new File(Environment
								.getExternalStorageDirectory()
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
			} catch (IOException e) {
				Log.e("SCID", e.getMessage(), e);
			}
		} catch (MalformedURLException e) {
			Log.e("SCID", e.getMessage(), e);
		}
		return result;
	}
}
