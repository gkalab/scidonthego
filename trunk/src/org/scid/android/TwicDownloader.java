package org.scid.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class TwicDownloader {

	private static String TWIC_SITE = "http://www.chess.co.uk/twic/twic";
	private Set<String> linkList = new HashSet<String>();
	private static final int BUFFER = 2048;

	public File getCurrentTwic(String directory) {
		parseTwicSite();
		String currentZip = getCurrentTwicZipName();
		return getPgnFromZipUrl(directory, currentZip);
	}

	public File getPgnFromZipUrl(String directory, String zipUrl) {
		File f = Tools.downloadFile(zipUrl);
		if (f != null) {
			return unzip(directory, f);
		} else {
			return null;
		}
	}

	private File unzip(String directory, File f) {
		File result = null;
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(f.getAbsolutePath());
			ZipInputStream zis = new ZipInputStream(
					new BufferedInputStream(fis));
			ZipEntry entry;
			boolean extracted = false;
			while ((!extracted && (entry = zis.getNextEntry()) != null)) {
				if (entry.getName().toLowerCase().endsWith(".pgn")) {
					Log.d("SCID", "Extracting: " + entry);
					int count;
					byte data[] = new byte[BUFFER];
					// write the files to the disk
					result = new File(directory, entry.getName());
					FileOutputStream fos = new FileOutputStream(result);
					dest = new BufferedOutputStream(fos, BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
					extracted = true;
				}
			}
			zis.close();
		} catch (Exception e) {
			Log.e("SCID", e.getMessage(), e);
		} finally {
			f.delete();
		}
		return result;
	}

	public List<TwicItem> getLinkList() {
		List<String> llist = new ArrayList<String>(linkList);
		Collections.sort(llist, Collections.reverseOrder());
		List<TwicItem> result = new ArrayList<TwicItem>();
		for (String link : llist) {
			result.add(new TwicItem(link));
		}
		return result;
	}

	private String getCurrentTwicZipName() {
		String result = null;
		if (this.linkList.size() > 0) {
			result = this.getLinkList().get(0).getLink();
		}
		return result;
	}

	public void parseTwicSite() {
		String data = "";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet(TWIC_SITE);
		int status = 0;
		try {
			HttpResponse response = httpclient.execute(request);
			status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				ByteArrayOutputStream ostream = new ByteArrayOutputStream();
				response.getEntity().writeTo(ostream);
				data = ostream.toString();
			} else {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));
				StringBuffer stringBuffer = new StringBuffer();
				String line = null;
				int noLinks = 0;
				while (noLinks < 20 && (line = reader.readLine()) != null) {
					stringBuffer.append(line + "\n");
					if (line.contains("g.zip")) {
						noLinks++;
					}
				}
				data = stringBuffer.toString();
				reader.close();
			}
			List<String> links = Tools.getLinks(data);
			for (String link : links) {
				String stripped = link.replaceAll("\"", "");
				if (stripped.endsWith("g.zip")) {
					linkList.add(stripped);
				}
			}
		} catch (IOException e1) {
			Log.e("SCID", e1.getMessage(), e1);
			data = e1.getMessage();
		}
	}

}
