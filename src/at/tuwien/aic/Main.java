/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.tuwien.aic;

import at.tuwien.aic.twitter.TweetCrawler;
import com.mongodb.util.JSON;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Klaus
 */
public class Main {

	private static Properties _prop;
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) {
		_prop = new Properties();
		try {
			System.out.println(new java.io.File(".").getCanonicalPath());
		} catch (IOException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}

		try {
			_prop.load(new InputStreamReader(new FileInputStream("properties.properties")));
		} catch (FileNotFoundException ex) {
			logger.severe("Could not find the properties file!");
			exitWithError(1);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not reed the properties file due to an IOException: {0}", ex.getMessage());
			exitWithError(1);
		}

		TweetCrawler tc = null;

		try {
			tc = new TweetCrawler(
					_prop.getProperty("db_host"),
					_prop.getProperty("db_name"),
					_prop.getProperty("db_tabl"),
					_prop.getProperty("twitter_ckey"),
					_prop.getProperty("twitter_csec"),
					_prop.getProperty("twitter_acct"),
					_prop.getProperty("twitter_acts")
			);
		} catch (UnknownHostException ex) {
			logger.severe("Could not connect to mongoDb");
			exitWithError(2);
		}

		String action = "";

		do {
			action = getNonEmptyString("The following actions can be run:\n\t1. Collect Tweets\n\t2. Analyze tweets to a specific topic\n\nWhat action do you want to execute");
		} while (!action.equals("1") && !action.equals("2"));
		
		if (action.equals("1")) {
			tc.collectTweets(Integer.parseInt(getNonEmptyString("How many Tweets do you want to collect")));
		}
		
		exit();
	}

	private static String getNonEmptyString(String msg) {
		return getNonEmptyString(msg, "");
	}

	private static String getNonEmptyString(String msg, String defaultValue) {
		Scanner scanner = new Scanner(System.in);
		String ret = defaultValue;

		System.out.print(msg + " [" + ret + "]: ");

		while (scanner.hasNextLine()) {
			ret = scanner.nextLine();
			
			if (!ret.equals("")) {
				break;
			}
			
			System.out.print(msg + " [" + ret + "]: ");
		}

		return ret;
	}

	private static void exit() {
		System.out.println("Application is exiting - Goodbye!");
		System.exit(0);
	}

	private static void exitWithError(int errorCode) {
		System.out.println("Exiting with errorCode " + errorCode);
		System.exit(errorCode);
	}
}
