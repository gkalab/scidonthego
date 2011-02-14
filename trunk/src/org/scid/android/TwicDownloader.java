package org.scid.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	private Matcher matcherTag;
	private Matcher matcherLink;
	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	private Pattern patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
	private Pattern patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);

	public File getCurrentTwic(String directory) {
		parseTwicSite();
		String currentZip = getCurrentTwicZipName();
		File f = downloadFile(currentZip);
		return unzip(directory, f);
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
			// TODO
			e.printStackTrace();
		} finally {
			f.delete();
		}
		return result;
	}

	public List<String> getLinkList() {
		List<String> result = new ArrayList<String>(linkList);
		Collections.sort(result, Collections.reverseOrder());
		return result;
	}

	/**
	 * @param path
	 * @return
	 */
	private File downloadFile(String path) {
		File result = null;
		URL url = null;
		try {
			url = new URL(path);
			URLConnection uc;
			try {
				uc = url.openConnection();
				String contentType = uc.getContentType();
				int contentLength = uc.getContentLength();
				if (contentType.startsWith("text/") || contentLength == -1) {
					throw new IOException("This is not a binary file.");
				}
				InputStream raw = uc.getInputStream();
				InputStream in = new BufferedInputStream(raw);
				byte[] data = new byte[contentLength];
				int bytesRead = 0;
				int offset = 0;
				while (offset < contentLength) {
					bytesRead = in.read(data, offset, data.length - offset);
					if (bytesRead == -1)
						break;
					offset += bytesRead;
				}
				in.close();

				if (offset != contentLength) {
					throw new IOException("Only read " + offset
							+ " bytes; Expected " + contentLength + " bytes");
				}
				result = File.createTempFile("twic", ".tmp");
				FileOutputStream out = new FileOutputStream(result
						.getAbsolutePath());
				out.write(data);
				out.flush();
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	private String getCurrentTwicZipName() {
		String result = null;
		if (this.linkList.size() > 0) {
			result = this.getLinkList().get(0);
		}
		return result;
	}

	private void parseTwicSite() {
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
				String result = "";
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));
				String line = null;
				int noLinks=0;
				while (noLinks<5 && (line = reader.readLine()) != null) {
					result += line + "\n";
					if (line.contains("g.zip")) {
						noLinks ++;
					}
				}
				data = result;
				reader.close();
			}
			List<String> links = getLinks(data);
			for (String link : links) {
				String stripped = link.replaceAll("\"", "");
				if (stripped.endsWith("g.zip")) {
					linkList.add(stripped);
				}
			}
		} catch (IOException e1) {
			// TODO
			data = e1.getMessage();
		}
	}

	/**
	 * Extract links from html using regular expressions
	 * 
	 * @param html
	 *            html content for validation
	 * @return List of links
	 */
	private List<String> getLinks(final String html) {
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

}
