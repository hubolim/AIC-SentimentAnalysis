/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.tuwien.aic;

import at.tuwien.aic.db.Database;
import at.tuwien.aic.preprocessing.Stem;
import at.tuwien.aic.preprocessing.StopWordRemoval;
import at.tuwien.aic.classify.ClassifyTweet;
import at.tuwien.aic.twitter.DefaultTweetHandler;
import at.tuwien.aic.twitter.TweetCrawler;
import at.tuwien.aic.twitter.TweetHandler;
import at.tuwien.aic.twitter.TweetScorer;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.QueryBuilder;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import twitter4j.internal.org.json.JSONException;
import weka.classifiers.Classifier;

/**
 *
 * @author 1027822 Klaus Harrer
 *
 * This is the main entry point for the stage 1 program You can run the various
 * actions from the commandline
 */
public class Main {

    private static Properties _prop;
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Main entry point
     *
     * @param args
     */
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws IOException, InterruptedException {

        try {
            System.out.println(new java.io.File(".").getCanonicalPath());
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        TweetCrawler tc = null;

        try {
            tc = TweetCrawler.getInstance();
        } catch (UnknownHostException ex) {
            logger.severe("Could not connect to mongoDb");
            exitWithError(2);
            return;
        }

        int action;

        while (true) {
            action = getDecision("The following actions can be executed", new String[]{
                "Subscribe to topic", "Query topic", "Test preprocessing", "Recreate the evaluation model", "Quit the application"
            }, "What action do you want to execute?");

            switch (action) {
                case 1:
                    tc.collectTweets(new DefaultTweetHandler() {
                        @Override
                        public boolean isMatch(String topic) {
                            return true;
                        }
                    }, getNonEmptyString("Which topic do you want to subscribe to (use spaces to specify more than one keyword)?").split(" "));

                    System.out.println("Starting to collection tweets");
                    System.out.println("Press enter to quit collecting");

                    while (System.in.read() != 10) ;

                    tc.stopCollecting();

                    break;
                case 2:
                    System.out.println("Enter tweet");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String tweet = br.readLine();
                    //double prediction = ClassifyTweet.classifyTweets(c, tweet, 2);
                    System.exit(0);
                    //classifyTopic();
                    break;
                case 3:
                    int subAction = getDecision("The following preprocessing steps are available", new String[]{
                        "Stop word removal", "Stemming", "Both"
                    }, "What do you want to test?");

                    switch (subAction) {
                        case 1:
                            stopWords();
                            break;
                        case 2:
                            stem();
                            break;
                        case 3:
                            stem(stopWords());
                        default:
                            break;
                    }

                    break;
                case 4:
                    //ClassifyTweet.classifyTweetArff(c, "resources/unlabeled.arff", "resources/train.arff");
                    ArrayList<String> tweets = new ArrayList<>();
                    ArrayList<String> processedTweets = new ArrayList<>();

                    //Positive Tweets
                    tweets.add("#Office365 is the fastest growing business in Microsoft’s history, one out of four enterprise clients owns #Office365 in the past 12 months");
                    tweets.add("oh yeah back to microsoft word it's great haha");
                    tweets.add("Microsoft Visual Studio 2013 Ultimate: excellent tool, but a bit pricey at $13K - http://t.co/qbc4MHeOrF");
                    tweets.add("Apple 'absolutely' plans to release new product types this year - Design Week: Design WeekApple 'absolutely' p... http://t.co/EK4rIbaHa0");
                    tweets.add("RT @ReformedBroker: \"Apple can't innovate.\" Motherf***er you're watching a movie on a 4 ounce plate of glass.");
                    tweets.add("What's the best brand of shoes?! Lol. There's too damn many, what do you prefer. Me is some Adidas.");
                    tweets.add("I want ?? “@AdorableWords: Tribal/Aztec pattern Nike free runs ?? http://t.co/WBxT8CNsPN”");
                    tweets.add("I achieved the Streak Week trophy with my Nike+ FuelBand. #nikeplus http://t.co/OgtpRcoSvp");
                    tweets.add("RT @DriveOfAthletes: Retweet for Nike! Favorite for UA! http://t.co/sKZ8hb27xH");

                    //Neutral Tweets
                    tweets.add("This site is giving away Free Microsoft Points #XBOX LIVE http://t.co/ZR1ythfqJ4");
                    tweets.add("How To Save The World: 1. Open Microsoft Word. 2. In a size 12-36 font, type \"The World\". 3. Click save.");
                    tweets.add("Microsoft Special Deals for Education: Microsoft special deals for Students, faculty and staff: http://t.co/Hf0b2ixPZa");
                    tweets.add("Microsoft is about to take Windows XP off life support On April 8, Windows XP's life is coming to an end. On that d http://t.co/kcSf4uIqW4");
                    tweets.add("Microsoft open sources its internet servers http://t.co/oLNTlVjE6Y");
                    tweets.add("The Apple Macintosh computer turns 30 - ... http://t.co/CAfq09Jgn7 #CarlIcahn #IsaacsonIt #SteveJobs #WalterIsaacson");
                    tweets.add("News Update| Samsung opens 60 dedicated stores in Europe with Carphone Warehouse http://t.co/1voh4yPMpN");
                    tweets.add("I posted a new photo to Facebook http://t.co/fI40hwklUj");
                    tweets.add("Brand New Men's ADIDAS VIGOR TR 3 Athletic Running shoes. Size: 11.5 http://t.co/oPuFoXLpeI");
                    tweets.add("I just ran 2.58 mi with Nike+. http://t.co/pYLkhBxH4Y #nikeplus");
                    tweets.add("Why is facebook still a thing");

                    //Negative Tweets
                    tweets.add("Thank God for microsoft programs.....");
                    tweets.add("RT @verge: UK government once again threatens to ditch Microsoft Office http://t.co/vhvybI1GwI");
                    tweets.add("Apple charge far too much for very poor phone cases");
                    tweets.add("Here's Why Everyone Is Worried About Apple's iPhone Sales http://t.co/Eq3oPt76AG");
                    tweets.add("Is Apple Ready to Disrupt Another Industry? http://t.co/07gedlN0cs via @zite");
                    tweets.add("Tim Cook Officially Admits iPhone 5c Didn’t Meet Expectations http://t.co/OdzGZOmdv7 #iPhone #Apple");
                    tweets.add("@MushIsAJedi @HeyItsAmine i dont rlly like samsung that much");
                    tweets.add("twitter facebook die shit");
                    tweets.add("I am thinking of leaving Facebook for a while... To much spying going on.. I am sick and tired of thinking about... http:/)/t.co/ULydkDEube");
                    tweets.add("RT @OfficialSheIdon: RIP Facebook, too many of our parents joined.");

                    StopWordRemoval swr = new StopWordRemoval("resources/stopwords.txt");

                    for (String t : tweets) {
                        t = swr.processText(t);
                        processedTweets.add(t);
                    }

                    for (int i = 0; i < 28; i++) {
                        if (i != 4) {
                            ArrayList<Integer> results = ClassifyTweet.classifyTweets(processedTweets, i);

                            int correctCount = 0;
                            int positiveCorrect = 0;
                            int neutralCorrect = 0;
                            int negativeCorrect = 0;
                            int falsePosNeu = 0;
                            int falsePosNeg = 0;
                            int falseNeuPos = 0;
                            int falseNeuNeg = 0;
                            int falseNegNeu = 0;
                            int falseNegPos = 0;

                            for (int j = 0; j < 30; j++) {
                                int pred = results.get(j);

                                if (j >= 0 && j < 10) {
                                    if (pred == 1) {
                                        correctCount++;
                                        positiveCorrect++;
                                    } else if (pred == 0) {
                                        falsePosNeu++;
                                    } else if (pred == -1) {
                                        falsePosNeg++;
                                    }
                                } else if (j >= 10 && j < 20) {
                                    if (pred == 0) {
                                        correctCount++;
                                        neutralCorrect++;
                                    } else if (pred == 1) {
                                        falseNeuPos++;
                                    } else if (pred == -1) {
                                        falseNeuNeg++;
                                    }

                                } else if (j >= 20 && j < 30) {
                                    if (pred == -1) {
                                        correctCount++;
                                        negativeCorrect++;
                                    } else if (pred == 0) {
                                        falseNegNeu++;
                                    } else if (pred == 1) {
                                        falseNegPos++;
                                    }
                                }
                            }

                            System.out.println("Correct Predictions: " + correctCount + " / 30");
                            System.out.println("Correct Positive: " + positiveCorrect + " / 10");
                            System.out.println("Correct Neutral: " + neutralCorrect + " / 10");
                            System.out.println("Correct Negative: " + negativeCorrect + " / 10");
                            
                            System.out.println("False Positive as Neutral: " + falsePosNeu);
                            System.out.println("False Positive as Negative: " + falsePosNeg);
                            
                            System.out.println("False Neutral as Positive: " + falseNeuPos);
                            System.out.println("False Neutral as Negative: " + falseNeuNeg);
                            
                            System.out.println("False Negative as Positive: " + falseNegPos);
                            System.out.println("False Negative as Neutral: " + falseNegNeu);
                        }

                    }

                    exit();
                case 5:
                    exit();
            }
        }
    }

    private static String getNonEmptyString(String msg) {
        return getNonEmptyString(msg, "");
    }

    private static String getNonEmptyString(String msg, String defaultValue) {
        Scanner scanner = new Scanner(System.in);
        String ret = defaultValue;

        print(msg, ret);

        while (scanner.hasNextLine()) {
            ret = scanner.nextLine();

            if (!ret.equals("")) {
                break;
            }

            print(msg, ret);
        }

        return ret;
    }

    public static void exit() {
        System.out.println("Application is exiting - Goodbye!");
        System.exit(0);
    }

    public static void exitWithError(int errorCode) {
        System.out.println("Exiting with errorCode " + errorCode);
        System.exit(errorCode);
    }

    private static int getDecision(String input, String[] options, String output) {
        System.out.println(input);

        int c = 0;
        int action = -1;

        for (String option : options) {
            System.out.println("\t" + ++c + ". " + option);
        }

        System.out.println("");

        while (action < 0 || action > c) {
            try {
                action = Integer.parseInt(getNonEmptyString(output));
            } catch (NumberFormatException e) {
            }
        }

        return action;
    }

    private static void print(String msg, String ret) {
        if (!ret.equals("")) {
            System.out.print(msg + " [" + ret + "]: ");
        } else {
            System.out.print(msg + ": ");
        }
    }

    private static String stopWords() {
        String text = getNonEmptyString("Enter the text to be StopWordRemoved");

        try {
            StopWordRemoval swr = new StopWordRemoval("resources/stopwords.txt");
            text = swr.processText(text);
            System.out.println(text);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return text;
    }

    private static void stem() {
        stem(null);
    }

    private static void stem(String text) {
        if (text == null) {
            text = getNonEmptyString("Enter the text to be stemmed");
        }

        System.out.println(Stem.stem(text));
    }

    /*
     private static void classifyTopic() {
        
     String topic = getNonEmptyString("Enter a topic you want to query");
     Mongo mongo;
     DB db = null;

     try {
     mongo = new Mongo(_prop.getProperty("db_host"));
     db = mongo.getDB(_prop.getProperty("db_name"));
     } catch (UnknownHostException ex) {
     Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
     return;
     }

     Classifier c = ClassifyTweet.loadModel(0);
     TweetScorer scorer = new TweetScorer();

     DBCollection tweetCollection = db.getCollection("tweets");

     Pattern pattern = Pattern.compile("^.+" + topic + ".+$");
     DBObject query = QueryBuilder.start("text").regex(pattern).get();
     DBCursor resultSet = tweetCollection.find(query);

     int count = 0;
     double value = 0;
     double cValue = 0;
     double tweetClassifiedScore = 0;
     double tweetPosUserScore = 0;
     double tweetNegUserScore = 0;

     while (resultSet.hasNext()) {
     try {
     DBObject obj = resultSet.next();
     String tweetText = (String) obj.get("text");

     cValue = ClassifyTweet.classifyTweet(c, tweetText);

     System.out.println(tweetText);
     System.out.println(cValue);

     tweetClassifiedScore += cValue;
     double score = scorer.scoreTweet(obj);

     if (tweetClassifiedScore > 0) {
     tweetPosUserScore += score;
     } else {
     tweetNegUserScore += score;
     }
     ++count;
     } catch (NumberFormatException ex) {
     Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
     } catch (JSONException ex) {
     Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
     } catch (IOException ex) {
     Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
     }
     }
     // Normalizing between 0 an 1
     value = tweetPosUserScore / (tweetPosUserScore + tweetNegUserScore);
     System.out.println("This topic has a sentiment value of: " + value);
     }
     */
}
