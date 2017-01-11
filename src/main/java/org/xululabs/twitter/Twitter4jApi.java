package org.xululabs.twitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import twitter4j.IDs;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class Twitter4jApi {
	int count = 0;

	/**
	 * use to get twitter instance
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 * @param accessTokenSecret
	 * @return twitter
	 */

	public Twitter getTwitterInstance(String consumerKey,
			String consumerSecret, String accessToken, String accessTokenSecret) {
		Twitter twitter = null;
		try {
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey)
					.setOAuthConsumerSecret(consumerSecret)
					.setOAuthAccessToken(accessToken)
					.setOAuthAccessTokenSecret(accessTokenSecret);
			TwitterFactory tf = new TwitterFactory(cb.build());
			twitter = tf.getInstance();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return twitter;
	}

	/**
	 * search keyword
	 * 
	 * @param twitter
	 * @param keyword
	 * @return tweets
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getTweets(Twitter twitter,
			String searchQuery) throws TwitterException {
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		Query query = new Query(searchQuery);
		QueryResult queryResult;
		query.setCount(100);
		try {
			queryResult = twitter.search(query);
			for (Status tweet : queryResult.getTweets()) {
				Map<String, Object> tweetInfo = new HashMap<String, Object>();
				tweetInfo.put("id", tweet.getId());
				tweetInfo.put("userId", tweet.getUser().getId());
				tweetInfo.put("tweet", tweet.getText());
				tweetInfo.put("screenName", tweet.getUser().getScreenName());
				tweetInfo.put("name", tweet.getUser().getName());
				tweetInfo.put("retweetCount", tweet.getRetweetCount());
				tweetInfo.put("followersCount", tweet.getUser()
						.getFollowersCount());
				tweetInfo.put("user_image", tweet.getUser()
						.getProfileImageURL());
				tweetInfo.put("description", tweet.getUser().getDescription());
				tweetInfo.put("retweetedByMe", tweet.isRetweetedByMe());
				tweets.add(tweetInfo);
			}
		} catch (TwitterException e) {

			e.printStackTrace();
		}

		return tweets;
	}

	/**
	 * use to get users by searching in their profile
	 * 
	 * @param twitter
	 * @param searchTerm
	 * @return
	 * @throws TwitterException
	 */
	public ArrayList<Map<String, Object>> getUsersProfiles(Twitter twitter,
			String searchTerm, ArrayList<Long> friensList)
			throws TwitterException {
		ArrayList<Map<String, Object>> usersList = new ArrayList<Map<String, Object>>();
		ResponseList<User> users;
		try {
			if (friensList.isEmpty()) {
				users = twitter.searchUsers(searchTerm, 1);
				for (User user : users) {
					Map<String, Object> userInfo = new HashMap<String, Object>();
					userInfo.put("name", user.getName());
					userInfo.put("screenName", user.getScreenName());
					userInfo.put("userImage", user.getProfileImageURL());
					userInfo.put("description", user.getDescription());
					usersList.add(userInfo);
				}
			} else {
				users = twitter.searchUsers(searchTerm, 1);
				for (User user : users) {
					Map<String, Object> userInfo = new HashMap<String, Object>();
					int isFollowing = friensList.contains(user.getId()) ? 1 : 0;
					userInfo.put("name", user.getName());
					userInfo.put("screenName", user.getScreenName());
					userInfo.put("userImage", user.getProfileImageURL());
					userInfo.put("description", user.getDescription());
					userInfo.put("isFollowing", isFollowing);
					usersList.add(userInfo);
				}
			}
		} catch (TwitterException e) {

			e.printStackTrace();
		}

		return usersList;
	}

	/**
	 * use to retweet tweet
	 * 
	 * @param twitter
	 * @param tweetId
	 * @return
	 * @throws TwitterException
	 */
	public boolean retweet(Twitter twitter, long tweetId)
			throws TwitterException {
		Status retweeted = twitter.retweetStatus(tweetId);
		return true;
	}

	/**
	 * use to create friendship
	 * 
	 * @param twitter
	 * @param ScreenName
	 * @return friended, info of the person
	 * @throws TwitterException
	 */
	public ArrayList<Map<String, Object>> createFriendship(Twitter twitter,
			String ScreenName) throws TwitterException {

		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		try {
			Relationship relation = twitter.showFriendship(
					twitter.getScreenName(), ScreenName);
			Map<String, Object> tweetInfo = null;
			if (!relation.isSourceFollowingTarget()) {
				User tweet = twitter.createFriendship(ScreenName);
				tweetInfo = new HashMap<String, Object>();
				tweetInfo.put("screenName", tweet.getScreenName());
				tweetInfo.put("id", tweet.getId());
				tweetInfo.put("name", tweet.getName());
				tweetInfo.put("followersCount", tweet.getFollowersCount());
				tweetInfo.put("userImage", tweet.getProfileImageURL());
				tweetInfo.put("description", tweet.getDescription());
				tweets.add(tweetInfo);
			} else {
				tweetInfo.put("Message", "Already Following");
				tweets.add(tweetInfo);
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		return tweets;
	}

	/**
	 * use to get friendlist of
	 * 
	 * @param twitter
	 * @return friendListIds,of the user
	 * @throws TwitterException
	 */

	public ArrayList<Map<String, Object>> getFriendsList(Twitter twitter,
			long cursor) throws TwitterException {
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		long[] friendIds;
		int request = 1;
		IDs friendIDs = null;
		Map<String, Object> tweetInfo = null;
		try {
			User followersCount = twitter.showUser(twitter.getId());
			friendIDs = twitter.getFriendsIDs(twitter.getId(), cursor);
			friendIds = friendIDs.getIDs();

			LinkedList<long[]> chunks = chunks(friendIds, 100);

			for (int j = 0; j < chunks.size(); j++) {

				Map<String, Object> cursorValue = new HashMap<String, Object>();
				cursorValue.put("nextCursor", friendIDs.getNextCursor());
				cursorValue.put("previousCursor", friendIDs.getPreviousCursor());
				cursorValue.put("friendscount",
						followersCount.getFriendsCount());

				ResponseList<User> users = twitter.lookupUsers(chunks.get(j));

				tweets.add(cursorValue);
				for (User tweet : users) {
					tweetInfo = new HashMap<String, Object>();
					tweetInfo.put("screenName", tweet.getScreenName().trim());
					tweetInfo.put("id", tweet.getId());
					tweets.add(tweetInfo);
				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		return tweets;
	}

	/**
	 * use to get followerslist of
	 * 
	 * @param twitter
	 * @return friendListIds,of the user
	 * @throws TwitterException
	 */

	public ArrayList<Map<String, Object>> getFollowersList(Twitter twitter,
			String ScreenName, long cursor) throws TwitterException {
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		try {
			IDs followerIDs = null;
			long[] friendIds = null;
			LinkedList<long[]> chunks = null;
			followerIDs = twitter.getFollowersIDs(ScreenName, cursor);
			friendIds = followerIDs.getIDs();
			chunks = chunks(friendIds, 100);
			User user = twitter.showUser(ScreenName);
			Map<String, Object> tweetInfo = null;
			Map<String, Object> cursorValue = new HashMap<String, Object>();
			cursorValue.put("nextCursor", followerIDs.getNextCursor());
			cursorValue.put("previousCursor", followerIDs.getPreviousCursor());
			cursorValue.put("myFollowersCount", user.getFollowersCount());
			tweets.add(cursorValue);

			for (int j = 0; j < chunks.size(); j++) {

				cursor = followerIDs.getNextCursor();
				ResponseList<User> users = twitter.lookupUsers(chunks.get(j));

				for (User tweet : users) {

					tweetInfo = new HashMap<String, Object>();
					tweetInfo.put("followersCount", tweet.getFollowersCount());
					tweetInfo.put("screenName", tweet.getScreenName().trim());
					tweetInfo.put("id", tweet.getId());
					tweetInfo.put("name", tweet.getName());
					tweetInfo.put("userImage", tweet.getProfileImageURL());
					tweetInfo.put("description", tweet.getDescription());
					tweets.add(tweetInfo);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tweets;
	}

	/**
	 * use to get Ids of followers of
	 * 
	 * @param twitter
	 * @return followetrIds,of the user
	 * @throws TwitterException
	 */

	public ArrayList<Map<String, Object>> getFollowersIds(Twitter twitter,
			String ScreenName, long cursor) throws TwitterException {
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();

		try {
			IDs followerIDs = null;
			long[] friendIds = null;
			LinkedList<long[]> chunks = null;
			followerIDs = twitter.getFollowersIDs(ScreenName, cursor);
			friendIds = followerIDs.getIDs();
			chunks = chunks(friendIds, 100);
			User user = twitter.showUser(ScreenName);
			Map<String, Object> tweetInfo = null;
			Map<String, Object> cursorValue = new HashMap<String, Object>();
			cursorValue.put("nextCursor", followerIDs.getNextCursor());
			cursorValue.put("previousCursor", followerIDs.getPreviousCursor());
			cursorValue.put("myFollowersCount", user.getFollowersCount());
			cursorValue.put("listSize", chunks.size());
			tweets.add(cursorValue);
			Map<String, Object> ids = new HashMap<String, Object>();
			ids.put("Ids", chunks);
			tweets.add(ids);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return tweets;
	}

	/**
	 * use to get followerslist of
	 * 
	 * @param twitter
	 * @return friendListIds,of the user
	 * @throws TwitterException
	 */

	public ArrayList<Map<String, Object>> getFollowersInfo(Twitter twitter,LinkedList<long[]> chunks) throws TwitterException {
		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();

		try {

			for (int j = 0; j < chunks.size(); j++) {

				ResponseList<User> users = twitter.lookupUsers(chunks.get(j));
				Map<String, Object> tweetInfo = null;
				for (User tweet : users) {

					tweetInfo = new HashMap<String, Object>();
					tweetInfo.put("followersCount", tweet.getFollowersCount());
					tweetInfo.put("screenName", tweet.getScreenName().trim());
					tweetInfo.put("id", tweet.getId());
					tweetInfo.put("name", tweet.getName());
					tweetInfo.put("userImage", tweet.getProfileImageURL());
					tweetInfo.put("description", tweet.getDescription());
					tweets.add(tweetInfo);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return tweets;
	}

	public static LinkedList<long[]> chunks(long[] bigList, int n) {
		int partitionSize = n;
		LinkedList<long[]> partitions = new LinkedList<long[]>();
		for (int i = 0; i < bigList.length; i += partitionSize) {
			long[] bulk = Arrays.copyOfRange(bigList, i,
					Math.min(i + partitionSize, bigList.length));
			partitions.add(bulk);
		}

		return partitions;
	}

	/**
	 * use to create FriendShip of
	 * 
	 * @param twitter
	 * @return friendListIds,of the user
	 * @throws TwitterException
	 */

	public ArrayList<Map<String, Object>> createFriendship(Twitter twitter,
			long userId) throws TwitterException {

		ArrayList<Map<String, Object>> tweets = new ArrayList<Map<String, Object>>();
		try {
			User tweet = twitter.createFriendship(userId);

			Map<String, Object> tweetInfo = new HashMap<String, Object>();

			tweetInfo.put("screenName", tweet.getScreenName());
			tweetInfo.put("id", tweet.getId());
			tweetInfo.put("name", tweet.getName());
			tweetInfo.put("followersCount", tweet.getFollowersCount());
			tweetInfo.put("userImage", tweet.getProfileImageURL());
			tweetInfo.put("description", tweet.getDescription());
			tweets.add(tweetInfo);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tweets;
	}

}
