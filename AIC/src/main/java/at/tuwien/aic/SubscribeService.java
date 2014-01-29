package at.tuwien.aic;

import at.tuwien.aic.db.Database;
import at.tuwien.aic.model.Task;
import at.tuwien.aic.twitter.DefaultTweetHandler;
import at.tuwien.aic.twitter.TweetCrawler;
import at.tuwien.aic.twitter.TweetHandler;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import java.net.UnknownHostException;

public class SubscribeService implements Runnable {

    private String user;
    private String topic;
    private long time;
    private TweetCrawler tc = null;
    private TweetHandler handler;

    public SubscribeService(final String user, final String topic, final long time) throws UnknownHostException {
        this.user = user;
        this.topic = topic;
        this.time = time;

        handler = new DefaultTweetHandler() {
            Task t = new Task(user, topic, time);

            @Override
            public String getId() {
                return t.getId();
            }

            @Override
            public boolean isMatch(String topic) {
                return t.isMatch(topic);
            }
        };
    }

    @Override
    public void run() {
        try {
            tc = TweetCrawler.getInstance();
            tc.collectTweets(handler, topic.split(" "));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException ei) {
            tc.stopCollecting();
        }
    }

    public void stop() {
        tc.removeHandler(handler);
    }
}