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
import at.tuwien.aic.twitter.TweetScorer;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.QueryBuilder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import twitter4j.internal.org.json.JSONException;
import weka.classifiers.Classifier;

/**
 *
 * @author 1027822 Klaus Harrer
 *
 * This is the main entry point for the stage 1 program You can run the various
 * actions from the commandline
 */
public class Main {

	private static Properties _prop;
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	/**
	 * Main entry point
	 *
	 * @param args
	 */
	@SuppressWarnings("empty-statement")
	public static void main(String[] args) throws IOException {
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

		int action;

		while (true) {
			action = getDecision("The following actions can be executed", new String[]{
				"Subscribe to topic", "Query topic", "Test preprocessing", "Recreate the evaluation model", "Quit the application"
			}, "What action do you want to execute?");

			switch (action) {
				case 1:
					tc.collectTweets(getNonEmptyString("Which topic do you want to subscribe to (use spaces to specify more than one keyword)?").split(" "));

					System.out.println("Starting to collection tweets");
					System.out.println("Press enter to quit collecting");

					while (System.in.read() != 10) ;

					tc.stopCollecting();

					break;
				case 2:
					classifyTopic();
					break;
				case 3: {
					int subAction = getDecision("The following preprocessing steps are available", new String[]{
						"Stop word removal", "Stemming", "Both"
					}, "What do you want to test?");

					switch (subAction) {
						case 1:
							stopWords();
							break;
						case 2:
							stem();
							break;
						case 3:
							stem(stopWords());
						default:
							break;
					}

					break;
				}
				case 4: {
					ClassifyTweet.saveModel("resources/traindata.arff", "resources/classifier.model");
					break;
				}
				case 5:
					exit();
				case 6: {
					ClassifyTweet.saveModel("resources/traindata.arff", "resources/classifier.model");
					Classifier c = ClassifyTweet.loadModel("resources/classifier.model");
					ClassifyTweet.classifyTweetArff(c, "resources/unlabeled.arff");
					//ClassifyTweet.evaluate(c, "resources/traindata.arff");
					break;
				}
			}
		}
	}

	private static String getNonEmptyString(String msg) {
		return getNonEmptyString(msg, "");
	}

	private static String getNonEmptyString(String msg, String defaultValue) {
		Scanner scanner = new Scanner(System.in);
		String ret = defaultValue;

		print(msg, ret);

		while (scanner.hasNextLine()) {
			ret = scanner.nextLine();

			if (!ret.equals("")) {
				break;
			}

			print(msg, ret);
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

	private static int getDecision(String input, String[] options, String output) {
		System.out.println(input);

		int c = 0;
		int action = -1;

		for (String option : options) {
			System.out.println("\t" + ++c + ". " + option);
		}

		System.out.println("");

		while (action < 0 || action > c) {
			try {
				action = Integer.parseInt(getNonEmptyString(output));
			} catch (NumberFormatException e) {
			}
		}

		return action;
	}

	private static void print(String msg, String ret) {
		if (!ret.equals("")) {
			System.out.print(msg + " [" + ret + "]: ");
		} else {
			System.out.print(msg + ": ");
		}
	}

	private static String stopWords() {
		String text = getNonEmptyString("Enter the text to be StopWordRemoved");

		try {
			StopWordRemoval swr = new StopWordRemoval("resources/stopwords.txt");
			text = swr.processText(text);
			System.out.println(text);
		} catch (IOException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}

		return text;
	}

	private static void stem() {
		stem(null);
	}

	private static void stem(String text) {
		if (text == null) {
			text = getNonEmptyString("Enter the text to be stemmed");
		}

		System.out.println(Stem.stem(text));
	}

	private static void classifyTopic() {
		String topic = getNonEmptyString("Enter a topic you want to query");
		Mongo mongo;
		DB db = null;

		try {
			mongo = new Mongo(_prop.getProperty("db_host"));
			db = mongo.getDB(_prop.getProperty("db_name"));
		} catch (UnknownHostException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}

		Classifier c = ClassifyTweet.loadModel("resources/classifier.model");
		TweetScorer scorer = new TweetScorer();

		DBCollection tweetCollection = db.getCollection("tweets");

		Pattern pattern = Pattern.compile("^.+" + topic + ".+$");
		DBObject query = QueryBuilder.start("text").regex(pattern).get();
		DBCursor resultSet = tweetCollection.find(query);

		int count = 0;
		double value = 0;
		double tweetClassifiedScore = 0;
		double tweetPosUserScore = 0;
		double tweetNegUserScore = 0;

		while (resultSet.hasNext()) {
			try {
				DBObject obj = resultSet.next();
				String tweetText = (String) obj.get("text");

				tweetClassifiedScore += ClassifyTweet.classifyTweet(c, tweetText);
				double score = scorer.scoreTweet(obj);

				if (tweetClassifiedScore > 0) {
					tweetPosUserScore += score;
				} else {
					tweetNegUserScore += score;
				}		
				++count;
			} catch (NumberFormatException ex) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			} catch (JSONException ex) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		// Normalizing between 0 an 1
		value = tweetPosUserScore / (tweetPosUserScore + tweetNegUserScore);
		System.out.println("This topic has a sentiment value of: " + value);
	}
}
