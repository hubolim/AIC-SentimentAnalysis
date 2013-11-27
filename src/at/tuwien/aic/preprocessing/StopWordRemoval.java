/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.tuwien.aic.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Klaus
 */
public class StopWordRemoval {

	private String pathToDictionary;
	private List<String> dictionary;

	public StopWordRemoval(String pathToDictionary) throws FileNotFoundException, IOException {
		this.pathToDictionary = pathToDictionary;

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(pathToDictionary))));
		dictionary = new ArrayList<String>();
		String line;

		while ((line = bufferedReader.readLine()) != null) {
			dictionary.add(line);
		}
		
		bufferedReader.close();
	}

	public String processText(String text) {
		String[] words = text.replaceAll("(?i)^(RT)?(.*)$", "$2").replace("\\n", " ").split("[\\s\\\\\\\"]");
		StringBuilder sb = new StringBuilder();
		
		for (String word: words) {
			if (!word.equals("") && !word.startsWith("http") && !Character.isWhitespace(word.charAt(0)) && !this.dictionary.contains(word)) {
				if (word.startsWith("@")) {
					word = "@";
				}
				
				sb.append(word);
				sb.append(" ");
			}
		}
		
		return sb.toString();
	}

}
