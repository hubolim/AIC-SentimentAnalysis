package at.tuwien.aic.model;

import at.tuwien.aic.db.Database;
import com.mongodb.*;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Task {

    private DB database = Database.getInstance().getDatabase("AIC-DB");
    private DBObject dbTask;
    private String id;
    private String user;
    private String[] topics;
    private Long stopTime;
    private Long startTime;

    public Task(String user, String topic, Long stopTime) throws UnknownHostException {
        final DBCollection tasks = database.getCollection("tasks");

        this.startTime = Calendar.getInstance().getTimeInMillis();
        this.stopTime = stopTime;
        final BasicDBObject obj = new BasicDBObject().append("user", user).append("topic", topic).append("stopTime", stopTime).append("startTime", startTime);
        tasks.insert(obj);

        this.dbTask = obj;
        this.id = obj.get("_id").toString();
        this.user = user;
        this.topics = topic.split(" ");
    }

    public Task(String taskId) throws UnknownHostException {
        final DBCollection tasks = database.getCollection("tasks");
        final BasicDBObject ref = new BasicDBObject("_id", new ObjectId(taskId));
        final DBCursor cursor = tasks.find(ref);

        try {
            this.dbTask = cursor.next();
            this.id = this.dbTask.get("_id").toString();
            this.topics = this.dbTask.get("topic").toString().split(" ");
            this.user = this.dbTask.get("user").toString();
            this.stopTime = (Long) this.dbTask.get("stopTime");
            this.startTime = (Long) this.dbTask.get("startTime");
        } catch (Exception e) {
            throw new IllegalArgumentException("This id doesn't exist");
        }
    }

    public DBCursor getTweets() {
        return database.getCollection("tweets").find(new BasicDBObject("taskId", id));
    }

    public boolean isMatch(String tweet) {
        for (String topic : topics) {
            if (tweet.toLowerCase().contains(topic.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public static List<Task> findByUser(String name) {
        try {
            final DBCursor cursor = Database.getInstance().getDatabase("AIC-DB").getCollection("tasks").find(new BasicDBObject("user", name));
            ArrayList<Task> tasks = new ArrayList<>();

            while (cursor.hasNext()) {
                DBObject next = cursor.next();
                tasks.add(new Task(next.get("_id").toString()));
            }

            return tasks;
        } catch (UnknownHostException e) {
            return new ArrayList<>();
        }
    }

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public String[] getTopics() {
        return topics;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public int countTweets() {
        return getTweets().count();
    }
}
