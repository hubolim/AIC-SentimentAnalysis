package at.tuwien.aic.twitter;

import at.tuwien.aic.Main;
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
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.FilterQuery;

/**
 * A simple Twitter crawler which extracts tweets via Twitterstream and safes
 * them into a Mongodb.
 */
public class TweetCrawler {

	private static final Logger logger = Logger.getLogger(TweetCrawler.class.getName());
	private final Mongo mongo;
	private final DB db;
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
		final BufferedWriter wr;

		try {
			wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("trainingdata.txt")));
		} catch (FileNotFoundException ex) {
			Logger.getLogger(TweetCrawler.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}

		StatusListener listener = new StatusListener() {
			int tweetCount = 0;

			@Override
			public synchronized void onStatus(Status status) {

				String tweet = DataObjectFactory.getRawJSON(status);
				DBObject doc = (DBObject) JSON.parse(tweet);

//				System.out.println(doc.get("text"));
//				int read = 0;
//
//				try {
//					read = System.in.read();
//					wr.write(read + ";" + doc.get("text"));
//					wr.flush();
//				} catch (IOException ex) {
//					Logger.getLogger(TweetCrawler.class.getName()).log(Level.SEVERE, null, ex);
//				}

				tweetCount++;
				System.out.println(tweetCount);

				coll.insert(doc);

				if (tweetCount >= nrToCollect) {
					twitterStream.shutdown();
					Main.exit();
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
//		FilterQuery filterQuery = new FilterQuery();
//		filterQuery.language(new String[]{"en"});
//		filterQuery.track(new String[]{"google"});
//		twitterStream.filter(filterQuery);
		// Starting the stream
		twitterStream.sample();
	}

}
