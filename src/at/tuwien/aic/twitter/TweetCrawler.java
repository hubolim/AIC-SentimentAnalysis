package at.tuwien.aic.twitter;

import java.net.UnknownHostException;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import com.mongodb.DB;
import com.mongodb.DBAddress;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.FilterQuery;

public class TweetCrawler {

	private final Logger logger = Logger.getLogger(TweetCrawler.class.getName());
	private Mongo mongo;
	private DB db;
	private final String dbHost, dbName, dbTable, twitter_consumerKey, twitter_consumerSecret, twitter_accessToken, twitter_accessTokenSecret;

	public TweetCrawler(String dbHost, String dbName, String dbTable, String twitter_consumerKey, String twitter_consumerSecret, String twitter_accessToken, String twitter_accessTokenSecret) throws UnknownHostException {
		this.dbHost = dbHost;
		this.dbName = dbName;
		this.dbTable = dbTable;
		this.twitter_consumerKey = twitter_consumerKey;
		this.twitter_consumerSecret = twitter_consumerSecret;
		this.twitter_accessToken = twitter_accessToken;
		this.twitter_accessTokenSecret = twitter_accessTokenSecret;

		this.mongo = new Mongo(dbHost);
		this.db = mongo.getDB(dbName);
	}

	public void collectTweets(final int nrToCollect) {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey("JJGOaJEwbCVM2gaSHgUXA")
				.setOAuthConsumerSecret("JGfdFmhEwa7237EEe0c4lDPchZxfH6h5H21gmeA7E")
				.setOAuthAccessToken("618954661-PQUeP6i3neTtkwsrWpUZUh0xN9RTdtyvHJFI2nVU")
				.setOAuthAccessTokenSecret("pEi4NlMOyFWtY07gs3zyrm6yDrjyhM2Drhrn9JPVGvo4a")
				.setJSONStoreEnabled(true);

		// Database collection
		final DBCollection coll = db.getCollection("tweets");

		// Initializing Twitter stream and listener
		final TwitterStream twitterStream = new TwitterStreamFactory(cb.build())
				.getInstance();

		StatusListener listener = new StatusListener() {
			int tweetCount = 0;

			@Override
			public void onStatus(Status status) {
				tweetCount++;
				System.out.println(tweetCount);
				String tweet = DataObjectFactory.getRawJSON(status);
				System.out.println(tweet);
				DBObject doc = (DBObject) JSON.parse(tweet);
				coll.insert(doc);
				if (tweetCount >= nrToCollect) {
					twitterStream.shutdown();
				}
			}

			@Override
			public void onDeletionNotice(
					StatusDeletionNotice statusDeletionNotice) {
			}

			@Override
			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
			}

			@Override
			public void onScrubGeo(long l, long l1) {
			}

			@Override
			public void onException(Exception ex) {
				ex.printStackTrace();
			}

			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub
				System.out.println(arg0);

			}
		};

		twitterStream.addListener(listener);
		FilterQuery fq = new FilterQuery();
		fq.language(new String[]{"en"}); //@TODO so wie's ausschaut kann ma ned nur nach der sprache filtern - müss ma sich genauer anschauen
		twitterStream.filter(fq);
		// Starting the stream
		twitterStream.sample();
	}

	/**
	 * A simple Twitter crawler which extracts tweets via Twitterstream and
	 * safes them into a Mongodb.
	 */
	public static void main(String[] args) {
		Mongo m;
		try {
			// Database connection
			m = new Mongo("localhost");
			DB db = m.getDB("AIC-DB");

			// Twitter connection
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
