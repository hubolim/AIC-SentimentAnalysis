package at.tuwien.aic.twitter;

import at.tuwien.aic.Main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import at.tuwien.aic.db.Database;
import com.mongodb.*;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import com.mongodb.util.JSON;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import twitter4j.FilterQuery;

import static at.tuwien.aic.Main.exitWithError;

/**
 * A simple Twitter crawler which extracts tweets via Twitterstream and safes
 * them into a Mongodb.
 */
public class TweetCrawler {

    private static TweetCrawler crawler;
    private Lock lock = new ReentrantLock();

    public static TweetCrawler getInstance() throws UnknownHostException {
        if (crawler == null) {
            Properties _prop = new Properties();

            try {
                _prop.load(new InputStreamReader(new FileInputStream("properties.properties")));
            } catch (FileNotFoundException ex) {
                logger.severe("Could not find the properties file!");
                exitWithError(1);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not reed the properties file due to an IOException: {0}", ex.getMessage());
                exitWithError(1);
            }

            crawler = new TweetCrawler(
                    _prop.getProperty("db_name"),
                    _prop.getProperty("db_tabl"),
                    _prop.getProperty("twitter_ckey"),
                    _prop.getProperty("twitter_csec"),
                    _prop.getProperty("twitter_acct"),
                    _prop.getProperty("twitter_acts")
            );
        }

        return crawler;
    }

    private static final Logger logger = Logger.getLogger(TweetCrawler.class.getName());
    private final DB db;
    private final String dbTable, twitter_consumerKey, twitter_consumerSecret, twitter_accessToken, twitter_accessTokenSecret;
    private TwitterStream twitterStream;
    private ArrayList<TweetHandler> handlers;

    public TweetCrawler(String dbName, String dbTable, String twitter_consumerKey, String twitter_consumerSecret, String twitter_accessToken, String twitter_accessTokenSecret) throws UnknownHostException {
        this.dbTable = dbTable;
        this.twitter_consumerKey = twitter_consumerKey;
        this.twitter_consumerSecret = twitter_consumerSecret;
        this.twitter_accessToken = twitter_accessToken;
        this.twitter_accessTokenSecret = twitter_accessTokenSecret;

        this.db = Database.getInstance().getDatabase(dbName);
        this.handlers = new ArrayList<>();
    }

    private void openStream(String twitter_consumerKey, String twitter_consumerSecret, String twitter_accessToken, String twitter_accessTokenSecret) {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true).setOAuthConsumerKey(twitter_consumerKey)
                .setOAuthConsumerSecret(twitter_consumerSecret)
                .setOAuthAccessToken(twitter_accessToken)
                .setOAuthAccessTokenSecret(twitter_accessTokenSecret)
                .setJSONStoreEnabled(true);

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

                synchronized (lock) {
                    if (handlers.size() == 0) {
                        stopCollecting();
                        twitterStream = null;
                        return;
                    }

                    for (int i = 0; i < handlers.size(); i++) {
                        TweetHandler tweetHandler = handlers.get(i);

                        if (tweetHandler.isMatch(doc.get("text").toString())) {
                            tweetHandler.onTweet(doc);

                            tweetCount++;
                            System.out.println(tweetCount);
                        }
                    }
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            }

            @Override
            public void onScrubGeo(long l, long l1) {
            }

            @Override
            public void onException(Exception ex) {
                logger.info("Tweetcrawler stopped");
            }

            @Override
            public void onStallWarning(StallWarning arg0) {
            }
        };

		/*
         * Attach the listener --> this ensures that the onStatus method gets
		 * called when a tweet is downloaded
		 */
        twitterStream.addListener(listener);
    }

    /**
     * Handles the collection of tweets
     *
     * @param keywords For which keywords should the tweets be limited to
     */
    public void collectTweets(final TweetHandler handler, String... keywords) throws InterruptedException {
        if (twitterStream == null) {
            openStream(this.twitter_consumerKey, this.twitter_consumerSecret, this.twitter_accessToken, this.twitter_accessTokenSecret);
            FilterQuery fq = new FilterQuery();
            fq.track(keywords);
            fq.language(new String[]{"en"});

			/*
             * Start the filtered download --> the downloaded tweets will only
			 * contain tweets with the keywords in it
			 */
            twitterStream.filter(fq);
        }

        synchronized (lock) {
            handlers.add(handler);
        }
    }

    public void stopCollecting() {
        twitterStream.shutdown();
        twitterStream.cleanUp();
    }

    public void removeHandler(TweetHandler handler) {
        synchronized (lock) {
            handlers.remove(handler);
        }
    }
}
