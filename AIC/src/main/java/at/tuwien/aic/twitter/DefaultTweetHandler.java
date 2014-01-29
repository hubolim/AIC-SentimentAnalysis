package at.tuwien.aic.twitter;

import at.tuwien.aic.db.Database;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import java.net.UnknownHostException;
import java.util.UUID;

public abstract class DefaultTweetHandler implements TweetHandler {

    private String uuid = UUID.randomUUID().toString();
    private final DB database;
    private DBCollection tweets;

    public DefaultTweetHandler() throws UnknownHostException {
        this.database = Database.getInstance().getDatabase("AIC-DB");
        this.tweets = database.getCollection("tweets");
    }

    @Override
    public String getId() {
        return uuid;
    }

    @Override
    public void onTweet(DBObject tweet) {
        tweet.put("taskId", getId());
        tweets.insert(tweet);
    }

    @Override
    public abstract boolean isMatch(String topic);
}
