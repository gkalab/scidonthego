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

class TwicDownloader {
	private final static String OLD_TWIC_SITE = "https://www.theweekinchess.com";
	private final static String NEW_TWIC_SITE = "https://theweekinchess.com";
	private final static String TWIC_SITE = NEW_TWIC_SITE + "/twic/";
	private Set<String> linkList = new HashSet<>();

	File getPgnFromZipUrl(String directory, String zipUrl) throws IOException {
		File f = Tools.downloadFile(zipUrl);
		if (f != null) {
			return Tools.unzip(directory, f, true);
		} else {
			return null;
		}
	}

	List<TwicItem> getLinkList() {
		List<String> llist = new ArrayList<>(linkList);
		Collections.sort(llist, Collections.reverseOrder());
		List<TwicItem> result = new ArrayList<>();
		for (String link : llist) {
			result.add(new TwicItem(link));
		}
		return result;
	}

	void parseTwicSite() {
		String data;
		try {
			URL obj = new URL(TWIC_SITE);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");

			int status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				StringBuilder response = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				data = response.toString();
			} else {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				StringBuilder stringBuffer = new StringBuilder();
				String line;
				int noLinks = 0;
				while (noLinks < 20 && (line = reader.readLine()) != null) {
					stringBuffer.append(line).append("\n");
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
					linkList.add(link.getLink().replace(OLD_TWIC_SITE, NEW_TWIC_SITE));
				}
			}
		} catch (IOException e1) {
			Log.e("SCID", e1.getMessage(), e1);
		}
	}
}
