/**
 * The MIT License
 * Copyright © 2017 Angelo Zerr and other contributors as
 * indicated by the @author tags.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/**
 *  Code copied from https://github.com/editorconfig/editorconfig-core-java/blob/master/src/main/java/org/editorconfig/core/EditorConfig.java
 */
package org.eclipse.ec4j.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpUtils {

	private static final Pattern OPENING_BRACES = Pattern.compile("(?:^|[^\\\\])\\{");
	private static final Pattern CLOSING_BRACES = Pattern.compile("(?:^|[^\\\\])}");

	static String convertGlobToRegEx(String pattern, List<int[]> ranges) {
		int length = pattern.length();
		StringBuilder result = new StringBuilder(length);
		int i = 0;
		int braceLevel = 0;
		boolean matchingBraces = countAll(OPENING_BRACES, pattern) == countAll(CLOSING_BRACES, pattern);
		boolean escaped = false;
		boolean inBrackets = false;
		while (i < length) {
			char current = pattern.charAt(i);
			i++;
			if ('*' == current) {
				if (i < length && pattern.charAt(i) == '*') {
					result.append(".*");
					i++;
				} else {
					result.append("[^/]*");
				}
			} else if ('?' == current) {
				result.append(".");
			} else if ('[' == current) {
				boolean seenSlash = findChar('/', ']', pattern, length, i) >= 0;
				if (seenSlash || escaped) {
					result.append("\\[");
				} else if (i < length && "!^".indexOf(pattern.charAt(i)) >= 0) {
					i++;
					result.append("[^");
				} else {
					result.append("[");
				}
				inBrackets = true;
			} else if (']' == current || ('-' == current && inBrackets)) {
				if (escaped) {
					result.append("\\");
				}
				result.append(current);
				inBrackets = current != ']' || escaped;
			} else if ('{' == current) {
				int j = findChar(',', '}', pattern, length, i);
				if (j < 0 && -j < length) {
					final String choice = pattern.substring(i, -j);
					final int[] range = getNumericRange(choice);
					if (range != null) {
						result.append("(\\d+)");
						ranges.add(range);
					} else {
						result = new StringBuilder(result);
						result.append("\\{");
						result.append(convertGlobToRegEx(choice, ranges));
						result.append("\\}");
					}
					i = -j + 1;
				} else if (matchingBraces) {
					result.append("(?:");
					braceLevel++;
				} else {
					result.append("\\{");
				}
			} else if (',' == current) {
				result.append(braceLevel > 0 && !escaped ? "|" : ",");
			} else if ('/' == current) {
				if (i < length && pattern.charAt(i) == '*') {
					if (i + 1 < length && pattern.charAt(i + 1) == '*' && i + 2 < length
							&& pattern.charAt(i + 2) == '/') {
						result.append("(?:/|/.*/)");
						i += 3;
					} else {
						result.append(current);
					}
				} else {
					result.append(current);
				}
			} else if ('}' == current) {
				if (braceLevel > 0 && !escaped) {
					result.append(")");
					braceLevel--;
				} else {
					result.append("}");
				}
			} else if ('\\' != current) {
				result.append(escapeToRegex(String.valueOf(current)));
			}
			if ('\\' == current) {
				if (escaped)
					result.append("\\\\");
				escaped = !escaped;
			} else {
				escaped = false;
			}
		}
		return result.toString();
	}

	private static int[] getNumericRange(String choice) {
		final int separator = choice.indexOf("..");
		if (separator < 0)
			return null;
		try {
			int start = Integer.parseInt(choice.substring(0, separator));
			int end = Integer.parseInt(choice.substring(separator + 2));
			return new int[] { start, end };
		} catch (NumberFormatException ignored) {
		}
		return null;
	}

	private static int findChar(final char c, final char stopAt, String pattern, int length, int start) {
		int j = start;
		boolean escapedChar = false;
		while (j < length && (pattern.charAt(j) != stopAt || escapedChar)) {
			if (pattern.charAt(j) == c && !escapedChar) {
				return j;
			}
			escapedChar = pattern.charAt(j) == '\\' && !escapedChar;
			j++;
		}
		return -j;
	}

	private static String escapeToRegex(String group) {
		final StringBuilder builder = new StringBuilder(group.length());
		for (char c : group.toCharArray()) {
			if (c == ' ' || Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '-') {
				builder.append(c);
			} else if (c == '\n') {
				builder.append("\\n");
			} else {
				builder.append("\\").append(c);
			}
		}
		return builder.toString();
	}

	private static int countAll(Pattern regex, String pattern) {
		final Matcher matcher = regex.matcher(pattern);
		int count = 0;
		while (matcher.find())
			count++;
		return count;
	}

	public static int compareVersions(String version1, String version2) {
		String[] version1Components = version1.split("(\\.|-)");
		String[] version2Components = version2.split("(\\.|-)");
		for (int i = 0; i < 3; i++) {
			String version1Component = version1Components[i];
			String version2Component = version2Components[i];
			int v1 = -1;
			int v2 = -1;
			try {
				v1 = Integer.parseInt(version1Component);
			} catch (NumberFormatException ignored) {
			}
			try {
				v2 = Integer.parseInt(version2Component);
			} catch (NumberFormatException ignored) {
			}
			if (v1 != v2)
				return v1 - v2;
		}
		return 0;
	}
}
