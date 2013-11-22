import java.net.UnknownHostException;

import twitter4j.FilterQuery;
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

public class TweetCrawler {

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
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true).setOAuthConsumerKey("******************")
					.setOAuthConsumerSecret("******************")
					.setOAuthAccessToken("******************")
					.setOAuthAccessTokenSecret("******************")
					.setJSONStoreEnabled(true);

			// Database collection
			final DBCollection coll = db.getCollection("tweets");

			// Initializing Twitter stream and listener
			TwitterStream twitterStream = new TwitterStreamFactory(cb.build())
					.getInstance();

			StatusListener listener = new StatusListener() {
				int tweetCount = 0;

				@Override
				public void onStatus(Status status) {
					tweetCount++;
					System.out.println(tweetCount);
					// Transforming tweets into JSON objects and saving them
					// into the DB collection
					String tweet = DataObjectFactory.getRawJSON(status);
					DBObject doc = (DBObject) JSON.parse(tweet);
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
			twitterStream.addListener(listener);
			// Starting the stream
			twitterStream.sample();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
