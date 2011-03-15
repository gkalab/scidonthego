package org.scid.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import android.os.Environment;
import android.util.Log;

public class Tools {
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
