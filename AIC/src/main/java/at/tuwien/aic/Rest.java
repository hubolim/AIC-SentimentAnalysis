package at.tuwien.aic;

import at.tuwien.aic.classify.*;
import at.tuwien.aic.classify.Configuration;
import at.tuwien.aic.db.Database;
import at.tuwien.aic.model.Task;
import at.tuwien.aic.preprocessing.StopWordRemoval;
import at.tuwien.aic.twitter.TweetScorer;
import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import twitter4j.internal.org.json.JSONException;

import javax.naming.AuthenticationException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/")
public class Rest {

    public DBObject getUserByField(String name, String field) throws UnknownHostException {
        BasicDBObject query = new BasicDBObject(field, name);
        final DBCollection users = Database.getInstance().getDatabase("AIC-DB").getCollection("users");
        final DBCursor dbUsers = users.find(query);

        if (dbUsers.hasNext()) {
            return dbUsers.next();
        }

        return null;
    }

    @POST
    @Path("register")
    public Boolean register(@FormParam("name") String name, @FormParam("pass") String pass) throws UnknownHostException, NoSuchAlgorithmException {
        final DBCollection users = Database.getInstance().getDatabase("AIC-DB").getCollection("users");

        if (getUserByField(name, "name") == null) {
            users.insert(new BasicDBObject().append("name", name).append("pass", DigestUtils.sha512Hex(pass)));
            return true;
        } else {
            return false;
        }
    }

    @POST
    @Path("login")
    public Object login(@FormParam("name") String name, @FormParam("pass") String pass) throws UnknownHostException {
        final DBCollection users = Database.getInstance().getDatabase("AIC-DB").getCollection("users");
        final DBObject user = getUserByField(name, "name");

        if (user.get("pass").equals(DigestUtils.sha512Hex(pass))) {
            String token = DigestUtils.md5Hex(name + String.valueOf(Calendar.getInstance().getTimeInMillis()));
            user.put("token", token);

            BasicDBObject newUser = new BasicDBObject("name", name).append("pass", user.get("pass")).append("token", token);
            users.update(new BasicDBObject().append("name", name), newUser);

            return token;
        } else {
            return false;
        }
    }

    @GET
    @Path("logout")
    public void logout(@QueryParam("token") String token) throws UnknownHostException {
        final DBCollection users = Database.getInstance().getDatabase("AIC-DB").getCollection("users");
        final DBObject user = getUserByField(token, "token");

        BasicDBObject newUser = new BasicDBObject("name", user.get("name")).append("pass", user.get("pass")).append("token", "");
        users.update(new BasicDBObject().append("token", token), newUser);
    }

    @GET
    @Path("subscribe")
    public Boolean subscribe(@QueryParam("token") String token, @QueryParam("topic") String topic, @QueryParam("time") String time) throws UnknownHostException {
        final DBObject user = getUserByField(token, "token");

        if (user == null) {
            return false;
        }

        int seconds = timeToSeconds(time);

        final Calendar cal = Calendar.getInstance();
        SubscribeExecutor.getInstance().submit(new SubscribeService(user.get("name").toString(), topic, cal.getTimeInMillis() + seconds * 1000), seconds);

        return true;
    }

    @GET
    @Path("query")
    @Produces("application/json")
    public String query(@QueryParam("token") String token, @QueryParam("task") String task, @QueryParam("config") int config) throws IOException, JSONException {
        checkToken(token);

        Task t = new Task(task);
        List<DBObject> dbTweets = t.getTweets().toArray();

        ArrayList<String> tweets = new ArrayList<>();

        StopWordRemoval swr = new StopWordRemoval("resources/stopwords.txt");

        for (DBObject dbObject : dbTweets) {
            final String text = dbObject.get("text").toString();
            tweets.add(swr.processText(text));
            System.out.println(text);
        }

        ArrayList<Integer> results = ClassifyTweet.classifyTweets(tweets, config);
        final TweetScorer scorer = new TweetScorer();
        double sentiment = 0;
        double pos = 0;
        double neutral = 0;
        double neg = 0;
        int iPos = 0;
        int iNeutral = 0;
        int iNeg = 0;

        for (int i = 0; i < results.size(); i++) {
            int value = results.get(i);
            final DBObject dbTweet = dbTweets.get(i);
            dbTweet.put("value", value);
            double importance = scorer.scoreTweet(dbTweet);

            if (value > 0) {
                pos += importance;
                ++iPos;
            } else if (value < 0) {
                neg += importance;
                ++iNeg;
            } else {
                neutral += importance;
                ++iNeutral;
            }
        }

        sentiment = 0.5 + pos / (pos + neg) / 2 - neg / (pos + neg) / 2;

        BasicDBObject obj = new BasicDBObject("topic", StringUtils.join(t.getTopics(), ' '));
        obj.put("tweets", dbTweets);
        obj.put("value", new DecimalFormat("##.###").format(sentiment * 100));
        obj.append("positive", iPos).append("neutral", iNeutral).append("negative", iNeg);

        return JSON.serialize(obj);
    }

    @GET
    @Path("configurations")
    @Produces("application/json")
    public String configurations() throws UnknownHostException {
        BasicDBList configurations = new BasicDBList();

        for (int i = 0; i < ClassifyTweet.configurations.size(); i++) {
            Configuration c = ClassifyTweet.configurations.get(i);
            configurations.add(new BasicDBObject("name", c.getModelName()).append("i", i));
        }

        return JSON.serialize(new BasicDBObject("configurations", configurations));
    }

    private DBObject checkToken(String token) throws UnknownHostException {
        DBObject user = getUserByField(token, "token");
        if (user == null) {
            throw new WebApplicationException(Response.status(403).build());
        }

        return user;
    }

    @GET
    @Path("tasks")
    @Produces("application/json")
    public String tasks(@QueryParam("token") String token) throws UnknownHostException {
        final DBObject user = checkToken(token);
        final List<Task> tasks = Task.findByUser(user.get("name").toString());
        List<DBObject> objects = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy - HH:mm:ss");

        for (Task t : tasks) {
            final long now = Calendar.getInstance().getTimeInMillis();
            double timeSinceStart = now - t.getStartTime();
            double timeToRun = t.getStopTime() - t.getStartTime();
            double progress = 100;
            int count = t.countTweets();

            if (timeToRun > timeSinceStart) {
                progress = (double) Math.round(100 / timeToRun * timeSinceStart * 100) / 100;
            }

            objects.add(new BasicDBObject("id", t.getId())
                    .append("user", t.getUser())
                    .append("topic", t.getTopics())
                    .append("runUntil", dateFormat.format(t.getStopTime()))
                    .append("startTime", dateFormat.format(t.getStartTime()))
                    .append("progress", progress)
                    .append("count", count));
        }

        return JSON.serialize(objects);
    }

    private int timeToSeconds(String strTime) {
        String[] splitted = strTime.split(" ");
        int time = 0;

        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];

            time += toTime(s, "s", 1);
            time += toTime(s, "m", 60);
            time += toTime(s, "h", 3600);
            time += toTime(s, "d", 3600 * 24);
        }

        return time;
    }

    private int toTime(String s, String format, int multiplicator) {
        int time = 0;

        try {
            time += Integer.valueOf(s.replace(format, "")) * multiplicator;
        } catch (NumberFormatException ignored) {
        }

        return time;
    }

}
