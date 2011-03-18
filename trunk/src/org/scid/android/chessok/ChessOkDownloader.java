package org.scid.android.chessok;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.scid.android.Link;
import org.scid.android.Tools;

import android.util.Log;

public class ChessOkDownloader {

	private static String CHESSOK_SITE = "http://chessok.com/?page_id=139";

	public Map<String, Map<String, List<Link>>> parseChessOkSite() {
		final Map<String, Map<String, List<Link>>> resultMap = new LinkedHashMap<String, Map<String, List<Link>>>();
		Map<String, List<Link>> currentMap = new LinkedHashMap<String, List<Link>>();
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
			int level = 0;
			String currentTitle = "";
			String mainTitle = "";
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
							if (level == 2) {
								currentTitle = titleString;
							} else if (level == 1) {
								mainTitle = titleString;
							}
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
									// fix wrong PGN links on chessOK site
									String linkString = link.getLink();
									int saveId = linkString.lastIndexOf("saveid=");
									if (saveId > 0 && linkString.substring(saveId).contains("pgn/")) {
										linkString = linkString.replaceAll("getpgn.php.*action=save&saveid=", "");
									}
									link.setLink("http://chessok.com/"
											+ linkString);
									linkList.add(link);
								} else {
									lastDescription = link.getDescription();
								}
							}
							if (!resultMap.containsKey(mainTitle)) {
								currentMap = new LinkedHashMap<String, List<Link>>();
								resultMap.put(mainTitle, currentMap);
							} 
							else {
								currentMap = resultMap.get(mainTitle);
							}
							currentMap.put(currentTitle, linkList);
						}
						level -= 1;
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
}
