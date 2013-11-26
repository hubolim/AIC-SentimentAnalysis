package at.tuwien.aic.twitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;

import com.mongodb.DBObject;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;

/**
 * Class for calculating weighting scores for tweets.
 * 
 */
public class TweetScorer {

	public TweetScorer() {

	}

	/**
	 * Calculates a weighting score for a tweet, ranges between 0 an 1
	 * 
	 * @param dbo
	 *            DBObject representing the tweet
	 * @return double the score of the tweet, between 0 and 1
	 * @throws JSONException
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public double scoreTweet(DBObject dbo) throws NumberFormatException,
			JSONException, IOException {
		return scoreTweet(new JSONObject(dbo.toString()));
	}

	/**
	 * Calculates a weighting score for a tweet, ranges between 0 an 1
	 * 
	 * @param jo
	 *            JSONObject representing the tweet
	 * @return double the score of the tweet, between 0 and 1
	 * @throws JSONException
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public double scoreTweet(JSONObject jo) throws JSONException,
			NumberFormatException, IOException {

		// Data of the user who created the tweet
		double favourites_count = 0.0;
		double followers_count = 0.0;
		double friends_count = 0.0;
		double statuses_count = 0.0;
		double listed_count = 0.0;

		// Data of the tweet
		double retweet_count = 0.0;
		double favorite_count = 0.0;

		Iterator<?> keys = jo.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			System.out.println("Key: " + key + " , Data: " + jo.getString(key));
			if (key.equals("retweet_count")) {
				retweet_count = Double.parseDouble(jo.getString(key));
			} else if (key.equals("favorite_count")) {
				favorite_count = Double.parseDouble(jo.getString(key));
			} else if (key.equals("user")) {
				if (jo.get(key) instanceof JSONObject) {
					JSONObject jo_2 = (JSONObject) jo.get(key);
					Iterator<?> keys_2 = jo_2.keys();
					while (keys_2.hasNext()) {
						String key_2 = (String) keys_2.next();
						System.out.println("  Key: " + key_2 + " , Data: "
								+ jo_2.getString(key_2));
						if (key_2.equals("favourites_count")) {
							favourites_count = Double.parseDouble(jo_2
									.getString(key_2));
						} else if (key_2.equals("followers_count")) {
							followers_count = Double.parseDouble(jo_2
									.getString(key_2));
						} else if (key_2.equals("friends_count")) {
							friends_count = Double.parseDouble(jo_2
									.getString(key_2));
						} else if (key_2.equals("statuses_count")) {
							statuses_count = Double.parseDouble(jo_2
									.getString(key_2));
						} else if (key_2.equals("listed_count")) {
							listed_count = Double.parseDouble(jo_2
									.getString(key_2));
						}

					}
				}
			}
		}

		// Saves the maximum data values of all tweets which used this method
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(
						"src/resources/tweetlevel_learning.txt"))));
		String line;
		// Maximum data values of all tweets which used this method
		double max_favourites_count = 1.0;
		double max_followers_count = 1.0;
		double max_friends_count = 1.0;
		double max_statuses_count = 1.0;
		double max_listed_count = 1.0;
		double max_retweet_count = 1.0;
		double max_favorite_count = 1.0;
		int count = 0;
		while ((line = bufferedReader.readLine()) != null) {
			if (count == 0) {
				max_favourites_count = Double.parseDouble(line);
			} else if (count == 1) {
				max_followers_count = Double.parseDouble(line);
			} else if (count == 2) {
				max_friends_count = Double.parseDouble(line);
			} else if (count == 3) {
				max_statuses_count = Double.parseDouble(line);
			} else if (count == 4) {
				max_listed_count = Double.parseDouble(line);
			} else if (count == 5) {
				max_retweet_count = Double.parseDouble(line);
			} else if (count == 6) {
				max_favorite_count = Double.parseDouble(line);
			}
			count++;
		}
		bufferedReader.close();

		if (favourites_count > max_favourites_count) {
			max_favourites_count = favourites_count;
		}
		if (followers_count > max_followers_count) {
			max_followers_count = followers_count;
		}
		if (friends_count > max_friends_count) {
			max_friends_count = friends_count;
		}
		if (statuses_count > max_statuses_count) {
			max_statuses_count = statuses_count;
		}
		if (listed_count > max_listed_count) {
			max_listed_count = listed_count;
		}
		if (retweet_count > max_retweet_count) {
			max_retweet_count = retweet_count;
		}
		if (favorite_count > max_favorite_count) {
			max_favorite_count = favorite_count;
		}

		// Calculate score
		double userScore = (favourites_count / max_favourites_count * 0.02)
				+ (followers_count / max_followers_count * 0.1)
				+ (friends_count / max_friends_count * 0.01)
				+ (statuses_count / max_statuses_count * 0.1)
				+ (listed_count / max_listed_count * 0.1);
		double tweetScore = (retweet_count / max_retweet_count * 0.34)
				+ (favorite_count / max_favorite_count * 0.33);

		System.out.println(followers_count);
		PrintWriter writer = new PrintWriter(
				"src/resources/tweetlevel_learning.txt", "UTF-8");
		writer.println(max_favourites_count);
		writer.println(max_followers_count);
		writer.println(max_friends_count);
		writer.println(max_statuses_count);
		writer.println(max_listed_count);
		writer.println(max_retweet_count);
		writer.println(max_favorite_count);
		writer.close();

		return userScore + tweetScore;
	}
}
