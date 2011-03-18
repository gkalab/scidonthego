package org.scid.android;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class ChessOkDownloader {

	private static String CHESSOK_SITE = "http://chessok.com/?page_id=139";

	public Map<String, List<Link>> parseChessOkSite() {
		final Map<String, List<Link>> resultMap = new LinkedHashMap<String, List<Link>>();
		String data = "";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet(CHESSOK_SITE);
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
				while ((line = reader.readLine()) != null) {
					stringBuffer.append(line + "\n");
				}
				data = stringBuffer.toString();
				reader.close();
			}
			String html = "";
			final String[] lines = data.split("\n");
			boolean start = false;
			final Map<Integer, String> titleMap = new HashMap<Integer, String>();
			int level = 0;
			String currentTitle = "";
			for (int i = 0; i < lines.length; i++) {
				final String line = lines[i];
				if (line.contains("class=\"recent_broadcasts\"")) {
					start = true;
				}
				if (start) {
					if (line.contains("</table")) {
						break;
					}
					if (line.contains("<ul")) {
						if (line.indexOf("id=") > 0) {
							String titleString = line.substring(line
									.indexOf("id=") + 4);
							titleString = titleString.substring(0, titleString
									.indexOf('"'));
							titleMap.put(level, titleString);
							currentTitle = this.getTitleFromMap(titleMap);
						}
						html = "";
						level += 1;
					} else if (line.contains("</ul>")) {
						final List<Link> links = Tools.getLinks(html);
						if (links.size() > 0) {
							List<Link> linkList = new ArrayList<Link>();
							String lastDescription = null;
							for (Link link : links) {
								if (link.getLink().endsWith(".pgn")) {
									if (lastDescription != null) {
										link.setDescription(lastDescription);
										lastDescription = null;
									} else {
										link.setDescription(link.getLink());
									}
									link.setLink("http://chessok.com/"+link.getLink());
									linkList.add(link);
								} else {
									lastDescription = link.getDescription();
								}
							}
							resultMap.put(currentTitle, linkList);
						}
						level -= 1;
						titleMap.put(level, "");
						html = "";
					} else {
						html += line + "\n";
					}
				}
			}

		} catch (IOException e1) {
			Log.e("SCID", e1.getMessage(), e1);
			data = e1.getMessage();
		}
		return resultMap;
	}

	private String getTitleFromMap(final Map<Integer, String> titleMap) {
		String title = "";
		String part = "";
		int level = 1;
		while (part != null) {
			part = titleMap.get(level);
			if (part != null) {
				if (title.length() > 0) {
					title += " - ";
				}
				title += part.trim();
			}
			level += 1;
		}
		return title;
	}
}
