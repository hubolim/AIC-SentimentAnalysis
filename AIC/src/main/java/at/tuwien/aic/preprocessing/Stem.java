/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.tuwien.aic.preprocessing;

import org.tartarus.snowball.ext.porterStemmer;

/**
 *
 * @author Klaus
 */
public class Stem {

	private static final porterStemmer stemmer = new porterStemmer();

	public static String stem(String text) {
		char ch;
		StringBuilder sb = new StringBuilder();
		StringBuilder input = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			ch = text.charAt(i);

			if (Character.isWhitespace((char) ch)) {
				if (input.length() > 0) {
					sb.append(stemWord(input.toString()));
					input.delete(0, input.length());
				}
			} else {
				input.append(Character.toLowerCase(ch));
			}
		}

		if (input.length() > 0) {
			sb.append(stemWord(input.toString()));
			input.delete(0, input.length());
		}

		return sb.toString().trim();
	}

	private static String stemWord(String word) {
		stemmer.setCurrent(word);

		for (int j = 1; j != 0; j--) {
			stemmer.stem();
		}

		return stemmer.getCurrent() + " ";
	}

}
