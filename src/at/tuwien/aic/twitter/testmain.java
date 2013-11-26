package at.tuwien.aic.twitter;

import java.io.IOException;
import java.net.UnknownHostException;

import twitter4j.internal.org.json.JSONException;

public class testmain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TweetScorer ts = new TweetScorer();
		try {
			try {
				double xx = ts.scoreTweet();
				System.out.println(xx);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
