package org.scid.android.twic;

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
import org.scid.android.Link;
import org.scid.android.Tools;

import android.util.Log;

public class TwicDownloader {

	private static String TWIC_SITE = "http://www.theweekinchess.com/twic/";
	private Set<String> linkList = new HashSet<String>();

	public File getCurrentTwic(String directory) throws IOException {
		parseTwicSite();
		String currentZip = getCurrentTwicZipName();
		return getPgnFromZipUrl(directory, currentZip);
	}

	public File getPgnFromZipUrl(String directory, String zipUrl) throws IOException {
		File f = Tools.downloadFile(zipUrl);
		if (f != null) {
			return Tools.unzip(directory, f, true);
		} else {
			return null;
		}
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
			List<Link> links = Tools.getLinks(data);
			for (Link link : links) {
				if (link.getLink().endsWith("g.zip")) {
					linkList.add(link.getLink());
				}
			}
		} catch (IOException e1) {
			Log.e("SCID", e1.getMessage(), e1);
			data = e1.getMessage();
		}
	}

}
