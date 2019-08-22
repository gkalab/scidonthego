package org.scid.database;

import java.util.HashMap;
import java.util.Map;

public class Utf8Converter {
	private static final Map<String, String> CP1252_CONVERSION = new HashMap<String, String>() {
		{
			put("\u00C3\u20AC", "À");
			put("\u00C3\uFFFD", "Á");
			put("\u00C3\u201A", "Â");
			put("\u00C3\u0192", "Ã");
			put("\u00C3\u201E", "Ä");
			put("\u00C3\u2026", "Å");
			put("\u00C3\u2020", "Æ");
			put("\u00C3\u2021", "Ç");
			put("\u00C3\u02C6", "È");
			put("\u00C3\u2030", "É");
			put("\u00C3\u0160", "Ê");
			put("\u00C3\u2039", "Ë");
			put("\u00C3\u0152", "Ì");
			put("\u00C3\uFFFD", "Í");
			put("\u00C3\u017D", "Î");
			put("\u00C3\uFFFD", "Ï");
			put("\u00C3\uFFFD", "Ð");
			put("\u00C3\u2018", "Ñ");
			put("\u00C3\u2019", "Ò");
			put("\u00C3\u201C", "Ó");
			put("\u00C3\u201D", "Ô");
			put("\u00C3\u2022", "Õ");
			put("\u00C3\u2013", "Ö");
			put("\u00C3\u02DC", "Ø");
			put("\u00C3\u2122", "Ù");
			put("\u00C3\u0161", "Ú");
			put("\u00C3\u203A", "Û");
			put("\u00C3\u0153", "Ü");
			put("\u00C3\uFFFD", "Ý");
			put("\u00C3\u017E", "Þ");
			put("\u00C3\u0178", "ß");
			put("\u00C3\u00A0", "à");
			put("\u00C3\u00A1", "á");
			put("\u00C3\u00A2", "â");
			put("\u00C3\u00A3", "ã");
			put("\u00C3\u00A4", "ä");
			put("\u00C3\u00A5", "å");
			put("\u00C3\u00A6", "æ");
			put("\u00C3\u00A7", "ç");
			put("\u00C3\u00A8", "è");
			put("\u00C3\u00A9", "é");
			put("\u00C3\u00AA", "ê");
			put("\u00C3\u00AB", "ë");
			put("\u00C3\u00AC", "ì");
			put("\u00C3\u00AD", "í");
			put("\u00C3\u00AE", "î");
			put("\u00C3\u00AF", "ï");
			put("\u00C3\u00B0", "ð");
			put("\u00C3\u00B1", "ñ");
			put("\u00C3\u00B2", "ò");
			put("\u00C3\u00B3", "ó");
			put("\u00C3\u00B4", "ô");
			put("\u00C3\u00B5", "õ");
			put("\u00C3\u00B6", "ö");
			//put("\u00C3\u0153", "œ"); TODO: find correct values
			put("\u00C3\u00B8", "ø");
			put("\u00C3\u00B9", "ù");
			put("\u00C3\u00BA", "ú");
			put("\u00C3\u00BB", "û");
			put("\u00C3\u00BC", "ü");
			put("\u00C3\u00BD", "ý");
			put("\u00C3\u00BE", "þ");
			put("\u00C3\u00BF", "ÿ");
		}
	};

	static String convertToUTF8(String value) {
		for (String source : CP1252_CONVERSION.keySet()) {
			value = value.replace(source, CP1252_CONVERSION.get(source));
		}
		return value;
	}

}
