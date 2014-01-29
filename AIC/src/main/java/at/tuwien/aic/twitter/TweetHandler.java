package at.tuwien.aic.twitter;

import com.mongodb.DBObject;

public interface TweetHandler {

    //UUID
    public String getId();
    public void onTweet(DBObject tweet);
    public boolean isMatch(String topic);

}
