/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.tuwien.aic;

import at.tuwien.aic.preprocessing.Stem;
import at.tuwien.aic.preprocessing.StopWordRemoval;
import at.tuwien.aic.classify.ClassifyTweet;
import at.tuwien.aic.twitter.TweetCrawler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Classifier;

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
			return;
		}

		String action = "";

		do {
			action = getNonEmptyString("The following actions can be run:\n\t1. Collect Tweets\n\t2. Run stop word removal on a string\n\t3. Run stemming on a string\n\t4. Run both\n\nWhat action do you want to execute");
		} while (!action.equals("1") && !action.equals("2") && !action.equals("3") && !action.equals("4") && !action.equals("5") && !action.equals("6"));
		
		switch (action) {
			case "1":
				tc.collectTweets(Integer.parseInt(getNonEmptyString("How many Tweets do you want to collect")));
				break;
			case "2":
				{
					String text = getNonEmptyString("Enter the text to be StopWordRemoved");
					try {
						StopWordRemoval swr = new StopWordRemoval("resources/stopwords.txt");
						System.out.println(swr.processText(text));
					} catch (IOException ex) {
						Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
					}		break;
				}
			case "3":
				{
					String text = getNonEmptyString("Enter the text to be stemmed");
					System.out.println(Stem.stem(text));
					break;
				}
			case "4":
				{
					String text = getNonEmptyString("Enter the text to be processed");
					try {
						StopWordRemoval swr = new StopWordRemoval("resources/stopwords.txt");
						System.out.println(Stem.stem(swr.processText(text)));
					} catch (IOException ex) {
						Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
					}		break;
				}
                        case "5":
                                {
                                    ClassifyTweet.saveModel("resources/traindata.arff", "resources/classifier.model");
                                    Classifier c = ClassifyTweet.loadModel("resources/classifier.model");
                                    ClassifyTweet.classifyTweetArff(c, "resources/unlabeled.arff");
                                    //ClassifyTweet.evaluate(c, "resources/traindata.arff");
                                    break;
                                }
                        case "6":
                                {
                                    ClassifyTweet.saveModel("resources/traindata.arff", "resources/classifier.model");
                                    String text = getNonEmptyString("Enter Text to be classified: ");
                                    Classifier c = ClassifyTweet.loadModel("resources/classifier.model");
                                    System.out.println(ClassifyTweet.classifyTweet(c, text));
                                    break;
                                }
		}
	}

	private static String getNonEmptyString(String msg) {
		return getNonEmptyString(msg, "");
	}

	private static String getNonEmptyString(String msg, String defaultValue) {
		Scanner scanner = new Scanner(System.in);
		String ret = defaultValue;

		if (!ret.equals("")) {
			System.out.print(msg + " [" + ret + "]: ");
		} else {
			System.out.print(msg + ": ");
		}

		while (scanner.hasNextLine()) {
			ret = scanner.nextLine();

			if (!ret.equals("")) {
				break;
			}

			System.out.print(msg + " [" + ret + "]: ");
		}

		return ret;
	}

	public static void exit() {
		System.out.println("Application is exiting - Goodbye!");
		System.exit(0);
	}

	private static void exitWithError(int errorCode) {
		System.out.println("Exiting with errorCode " + errorCode);
		System.exit(errorCode);
	}
}
