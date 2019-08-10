package org.scid.android.twic;

import android.util.Log;

import org.scid.android.Link;
import org.scid.android.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		try {
			URL obj = new URL(TWIC_SITE);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");

			int status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				data = response.toString();
			} else {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
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
