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
	private TwitterStream twitterStream;

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

	/**
	 * Handles the collection of tweets
	 *
	 * @param keywords For which keywords should the tweets be limited to
	 */
	public void collectTweets(String... keywords) {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey("JJGOaJEwbCVM2gaSHgUXA")
				.setOAuthConsumerSecret("JGfdFmhEwa7237EEe0c4lDPchZxfH6h5H21gmeA7E")
				.setOAuthAccessToken("618954661-PQUeP6i3neTtkwsrWpUZUh0xN9RTdtyvHJFI2nVU")
				.setOAuthAccessTokenSecret("pEi4NlMOyFWtY07gs3zyrm6yDrjyhM2Drhrn9JPVGvo4a")
				.setJSONStoreEnabled(true);

		// Database collection
		final DBCollection coll = db.getCollection(dbTable);

		// Initializing Twitter stream and listener
		twitterStream = new TwitterStreamFactory(cb.build())
				.getInstance();

		StatusListener listener = new StatusListener() {
			int tweetCount = 0;

			@Override
			public synchronized void onStatus(Status status) {

				String tweet = DataObjectFactory.getRawJSON(status);
				DBObject doc = (DBObject) JSON.parse(tweet);

				if (!doc.get("lang").equals("en")) {
					return;
				}
				
				tweetCount++;
				System.out.println(tweetCount);

				coll.insert(doc);
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

		/*
		 * Attach the listener --> this ensures that the onStatus method gets
		 * called when a tweet is downloaded
		 */
		twitterStream.addListener(listener);

		if (keywords != null && keywords.length > 0) {
			FilterQuery fq = new FilterQuery();
			fq.track(keywords);
			fq.language(new String[] { "en" });

			/*
			 * Start the filtered download --> the downloaded tweets will only
			 * contain tweets with the keywords in it
			 */
			twitterStream.filter(fq);
		} else {
			// Start downloading all kinds of tweets (random)
			twitterStream.sample();
		}
	}
	
	public void stopCollecting() {
		twitterStream.cleanUp();
	}

}
