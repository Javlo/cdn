package org.javlo.cdn.helper;

import java.net.URI;
import java.net.URISyntaxException;

public class StringHelper {
	
	private static final String ISO_ACCEPTABLE_CHAR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.";
	
	/**
	 * transform a string null in a empty String.
	 * 
	 * @param inStr
	 *            a string can be null
	 * @return never null ( empty string if input is null)
	 */
	public static String neverNull(Object inStr) {
		return neverNull(inStr, "");
	}

	/**
	 * transform a string null in a empty String.
	 * 
	 * @param inStr
	 *            a string can be null
	 * @param replaceWith
	 *            replace with this if null.
	 * @return never null ( empty string if input is null)
	 */
	public static String neverNull(Object inStr, String replaceWith) {
		if (inStr == null) {
			return replaceWith;
		} else {
			return "" + inStr;
		}
	}

	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		return uri.getHost();
	}
	
	public static String createFileName(String fileName) {
		return createFileName(fileName, '-');
	}

	private static String createFileName(String fileName, char defaultReplaceChar) {
		if (fileName == null) {
			return null;
		}
		return createCleanName(fileName, ISO_ACCEPTABLE_CHAR, defaultReplaceChar).toLowerCase();
	}
	
	private static String createCleanName(String fileName, String acceptableCharacters, char defaultReplaceChar) {
		if (fileName == null) {
			return null;
		}
		fileName = fileName.trim();

		StringBuffer res = new StringBuffer();

		char[] source = fileName.toCharArray();
		for (char element : source) {
			if (acceptableCharacters.indexOf(element) >= 0) {
				res.append(element);
			} else {
				switch (element) {
				case '\u00e9':
				case '\u00e8':
				case '\u00eb':
				case '\u00ea':
					res.append('e');
					break;
				case '\u00c9':
				case '\u00c8':
				case '\u00cb':
				case '\u00ca':
					res.append('E');
					break;
				case '\u00e0':
				case '\u00e2':
				case '\u00e4':
				case '\u00e3':
				case '\u00e5':
					res.append('a');
					break;
				case '\u00c0':
				case '\u00c2':
				case '\u00c4':
				case '\u00c3':
				case '\u00c5':
					res.append('A');
					break;
				case '\u00ee':
				case '\u00ef':
				case '\u00ec':
				case '\u00ed':
					res.append('i');
					break;
				case '\u00ce':
				case '\u00cf':
				case '\u00cc':
				case '\u00cd':
					res.append('I');
					break;
				case '\u00f2':
				case '\u00f3':
				case '\u00f4':
				case '\u00f5':
					res.append('o');
					break;
				case '\u00d2':
				case '\u00d3':
				case '\u00d4':
				case '\u00d5':
					res.append('O');
					break;
				case '\u00f9':
				case '\u00fa':
				case '\u00fb':
				case '\u00fc':
					res.append('u');
					break;
				case '\u00d9':
				case '\u00da':
				case '\u00db':
				case '\u00dc':
					res.append('U');
					break;
				case '\u00fd':
				case '\u00fe':
				case '\u00ff':
					res.append('y');
					break;
				case '\u0160':
					res.append('S');
					break;
				case '\u0161':
					res.append('s');
					break;
				case '\u00dd':
				case '\u00de':
				case '\u0178':
					res.append('Y');
					break;
				case '\u00e7':
					res.append('c');
					break;
				case '\u00c7':
					res.append('C');
					break;
				case '\u20ac':
					res.append("EUR");
					break;
				case '$':
					res.append("USD");
					break;
				case '/':
				case '\\':
					res.append("-");
					break;
				default:
					res.append(defaultReplaceChar);
					break;
				}

			}
		}
		String cleanName = res.toString();
		while (cleanName.contains(("" + defaultReplaceChar) + defaultReplaceChar)) {
			cleanName = cleanName.replace(("" + defaultReplaceChar) + defaultReplaceChar, "" + defaultReplaceChar);
		}
		return cleanName;
	}
	
	/**
	 * retreive the file extension.
	 * 
	 * @param inFileName
	 *            a file name
	 * @return a file extension without dot ( pdf, zip, ... )
	 */
	public static String getFileExtension(String inFileName) {
		if (inFileName == null) {
			return "";
		}
		String outExt = "";
		int dotIndex = inFileName.lastIndexOf('.');
		int endIndex = inFileName.lastIndexOf('?');
		int jsessionIndex = inFileName.lastIndexOf(';');
		if (jsessionIndex >= 0 && dotIndex > jsessionIndex) {
			dotIndex = inFileName.substring(0, jsessionIndex).lastIndexOf('.');
		}
		if (endIndex <= 0 || endIndex < dotIndex) {
			if (jsessionIndex > -1) {
				endIndex = jsessionIndex;
			} else {
				endIndex = inFileName.length();
			}
		} else {
			if (jsessionIndex >= 0 && jsessionIndex < endIndex) {
				endIndex = jsessionIndex;
			}
		}
		if (dotIndex >= 0) {
			outExt = inFileName.substring(dotIndex + 1, endIndex);
		}
		
		if (outExt.equals("gzip")) {
			return getFileExtension(inFileName.replace(".gzip", ""));
		}

		return outExt;
	}

}
