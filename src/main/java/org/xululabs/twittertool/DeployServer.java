package org.xululabs.twittertool;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.xululabs.twitter.Twitter4jApi;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class DeployServer extends AbstractVerticle {
	HttpServer server;
	Router router;
	Twitter4jApi twitter4jApi;
	String host;
	int port;

	/**
	 * constructor use to initialize values
	 */
	public DeployServer() {
		this.host = "localhost";
		this.port = 8283;
		this.twitter4jApi = new Twitter4jApi();
	}

	/**
	 * Deploying the verical
	 */
	@Override
	public void start() {
		server = vertx.createHttpServer();
		router = Router.router(vertx);
		// Enable multipart form data parsing
		router.route().handler(BodyHandler.create());
		router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("Content-Type, Authorization"));
		// registering different route handlers
		this.registerHandlers();
		server.requestHandler(router::accept).listen(port, host);
	}

	/**
	 * For Registering different Routes
	 */
	public void registerHandlers() {
		router.route(HttpMethod.GET, "/").blockingHandler(this::welcomeRoute);
		router.route(HttpMethod.POST, "/searchTweets").blockingHandler(this::searchTweets);
		router.route(HttpMethod.POST, "/searchProfiles").blockingHandler(this::searchProfiles);
		router.route(HttpMethod.POST, "/searchInTweetsAndProfiles").blockingHandler(this::searchInTweetsAndProfiles);
		router.route(HttpMethod.POST, "/retweet").blockingHandler(this::retweetRoute);
		router.route(HttpMethod.POST, "/followUser").blockingHandler(this::followRoute);
		router.route(HttpMethod.POST, "/friendsList").blockingHandler(this::friendsListRoute);
		router.route(HttpMethod.POST, "/followersList").blockingHandler(this::followersListRoute);
		router.route(HttpMethod.POST, "/followerIds").blockingHandler(this::followerIdsRoute);
		router.route(HttpMethod.POST, "/followersInfo").blockingHandler(this::followersInfoRoute);
	}

	/**
	 * welcome route
	 * 
	 * @param routingContext
	 */

	public void welcomeRoute(RoutingContext routingContext) {
		routingContext.response().end("<h1>Welcome To Twitter Tool</h1>");
	}

	/**
	 * route to search tweets
	 * @param routingContext
	 * twitter api limit : 160-180 calls(search 100 tweets per call) in 15 min window
	 */
	public void searchTweets(RoutingContext routingContext) {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String query = (routingContext.request().getParam("query") == null) ? "data science" : routingContext.request().getParam("query");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		try {
			TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			ArrayList<Map<String, Object>> tweets = this.getTweets(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), query);
		    responseMap.put("tweets", tweets);
		    response = mapper.writeValueAsString(responseMap);
		} catch (Exception ex) {
			response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
		}
		routingContext.response().end(response);
	}
	/**
	 * route for search Profile
	 * @param routingContext
	 * limit : twitter api can search 900 users per 15 minute window,20 per call
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public void searchProfiles(RoutingContext routingContext) {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String keyword = (routingContext.request().getParam("keyword") == null) ? "data science" : routingContext.request().getParam("keyword");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		String friendsListJson =  (routingContext.request().getParam("friendsList") == null) ? "" : routingContext.request().getParam("friendsList");

		try {
			TypeReference<ArrayList<Long>> friendsListType = new TypeReference<ArrayList<Long>>() {};
			ArrayList<Long> friendsList = mapper.readValue(friendsListJson, friendsListType);
			TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			ArrayList<Map<String, Object>> users = this.getUsers(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), keyword,friendsList);
		    responseMap.put("users", users);
		    response = mapper.writeValueAsString(responseMap);
		} catch (Exception ex) {
			response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
		}
		routingContext.response().end(response);
	}
	/**
	 * route use to search in both profiles and tweets
	 * @param routingContext
	 */
	public void searchInTweetsAndProfiles(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String query = (routingContext.request().getParam("query") == null) ? "data science" : routingContext.request().getParam("query");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		String keyword = (routingContext.request().getParam("keyword") == null) ? "data science" : routingContext.request().getParam("keyword");
		String friendsListJson =  (routingContext.request().getParam("friendsList") == null) ? "" : routingContext.request().getParam("friendsList");
		try {
			TypeReference<ArrayList<Long>> friendsListType = new TypeReference<ArrayList<Long>>() {};
			ArrayList<Long> friendsList = mapper.readValue(friendsListJson, friendsListType);
			TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			
			ArrayList<Map<String, Object>> users = this.getUsers(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), keyword,friendsList);
			ArrayList<Map<String, Object>> tweets = this.getTweets(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")), query);
			responseMap.put("users", users);
			responseMap.put("tweets", tweets);
		    response = mapper.writeValueAsString(responseMap);
		} catch (Exception ex) {
			response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
		}
		routingContext.response().end(response);
	}
	/**
	 * use to retweet by id
	 * @param routingContext
	 * limits : 2,400 tweets per day also having limts  according hour
	 */
	public void retweetRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String tweetId = (routingContext.request().getParam("tweetId") == null) ? "" : routingContext.request().getParam("tweetId");
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
	    try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			boolean retweeted = twitter4jApi.retweet(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),Long.parseLong(tweetId));
	        responseMap.put("retweeted", retweeted);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	/**
	 * use to follow by id
	 * @param routingContext
	 * can't follow more than 1000 user in one day, total 5000 users can be followed by a account
	 */
	public void followRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;
		String userId = (routingContext.request().getParam("userId") == null) ? "" : routingContext.request().getParam("userId");
		boolean inputString ;
		Long userId1 = null;
		
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
	    try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			ArrayList<Map<String, Object>> FreindshipResponse = null;
			if(userId.toString().matches("^\\d+(\\.\\d+)?")) {
				 userId1 = Long.parseLong(userId.toString());
				 FreindshipResponse = this.getFriendShip(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),userId1);
			} else {
			 FreindshipResponse = this.getFriendShip(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),userId);
			}		
	        responseMap.put("following ", FreindshipResponse);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	
	/**
	 * use to follow by id
	 * @param routingContext
	 * can't follow more than 1000 user in one day, total 5000 users can be followed by a account
	 */
	public void friendsListRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;	
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		String cursor = (routingContext.request().getParam("cursorValue") == null) ? "" : routingContext.request().getParam("cursorValue");
		long cursorvalue=-1;
		if(!cursor.isEmpty()){
	 cursorvalue = Long.parseLong(cursor);
	}
		try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType); //Long.parseLong
			ArrayList<Map<String, Object>> FreindshipResponse = twitter4jApi.getFriendsList(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),cursorvalue);
	        responseMap.put("Friendslist ", FreindshipResponse);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	
	/**
	 * use to get followersList
	 * @param routingContext
	 * @param ScreenName
	 *having limit of 15 request for 15 minutes
	 */
	public void followersListRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;	
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		String screenName = (routingContext.request().getParam("screenName") == null) ? "" : routingContext.request().getParam("screenName");
		String cursor = (routingContext.request().getParam("cursorValue") == null) ? "" : routingContext.request().getParam("cursorValue");
		long cursorvalue=-1;
		if(!cursor.isEmpty()){
	 cursorvalue = Long.parseLong(cursor);
	}	
		try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType); //Long.parseLong
			ArrayList<Map<String, Object>> FollowersListResponse = this.getFollowersList(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),screenName,cursorvalue);
	        //////
			responseMap.put("followersList", FollowersListResponse);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	/**
	 * use to get followersList
	 * @param routingContext
	 * @param ScreenName
	 *having limit of 15 request for 15 minutes
	 */
	public void followerIdsRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;	
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		String screenName = (routingContext.request().getParam("screenName") == null) ? "" : routingContext.request().getParam("screenName");
		String cursor = (routingContext.request().getParam("cursorValue") == null) ? "" : routingContext.request().getParam("cursorValue");
		long cursorvalue=-1;
		if(!cursor.isEmpty()){
	 cursorvalue = Long.parseLong(cursor);
	}	
		try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType); //Long.parseLong
			ArrayList<Map<String, Object>> FollowersListResponse = this.getFollowersIds(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),screenName,cursorvalue);
	        //////
			responseMap.put("followersIds",FollowersListResponse);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	/**
	 * use to get followersList
	 * @param routingContext
	 * @param ScreenName
	 *having limit of 15 request for 15 minutes
	 */
	public void followersInfoRoute(RoutingContext routingContext){
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		String response;	
		String credentialsJson = (routingContext.request().getParam("credentials") == null) ? "": routingContext.request().getParam("credentials");
		//String page = (routingContext.request().getParam("page") == null) ? "" : routingContext.request().getParam("page");
		String followersId = (routingContext.request().getParam("followerIds") == null) ? "" : routingContext.request().getParam("followerIds");
		/* int pagevalue=10;
		if(!page.isEmpty()){
			pagevalue = Integer.parseInt(page);
	}	*/
		try{
	    	TypeReference<HashMap<String, Object>> credentialsType = new TypeReference<HashMap<String, Object>>() {};
	    	TypeReference<LinkedList<long []>> followerIdsType = new TypeReference<LinkedList<long []>>() {};
			HashMap<String, String> credentials = mapper.readValue(credentialsJson, credentialsType);
			LinkedList<long []> followerIds = mapper.readValue(followersId, followerIdsType);
			ArrayList<Map<String, Object>> FollowersListResponse = this.getFollowersInfo(this.getTwitterInstance(credentials.get("consumerKey"), credentials.get("consumerSecret"), credentials.get("accessToken"), credentials.get("accessTokenSecret")),followerIds);
	        //////
			responseMap.put("followersIds",FollowersListResponse);
	        response = mapper.writeValueAsString(responseMap);
	    }catch(Exception ex){
	    	response = "{\"status\" : \"error\", \"msg\" :" + ex.getMessage() + "}";
	    }
	    routingContext.response().end(response);
	}
	
	
	/**
	 * use to get tweets 
	 * @param twitter
	 * @param query
	 * @return list of tweets
	 * @throws TwitterException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getTweets(Twitter twitter, String query) throws TwitterException {
	
		return twitter4jApi.getTweets(twitter, query);
	}
	/**
	 * use to get friendsList 
	 * @param twitter
	 * @return friendsList
	 * @throws TwitterException 
	 * @throws IllegalStateException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getFriendsList(Twitter twitter,long cursor) throws IllegalStateException, TwitterException {
	
		return twitter4jApi.getFriendsList(twitter,cursor);
	}
	/**
	 * use to get followerslist 
	 * @param twitter
	 * @return followersList
	 * @throws TwitterException 
	 * @throws IllegalStateException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getFollowersList(Twitter twitter,String ScreenName,long cursor) throws TwitterException {
	
		return twitter4jApi.getFollowersList(twitter,ScreenName,cursor);
	}
	
	/**
	 * use to get followerslist 
	 * @param twitter
	 * @return followersList
	 * @throws TwitterException 
	 * @throws IllegalStateException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getFollowersIds(Twitter twitter,String ScreenName,long cursor) throws TwitterException {
	
		return twitter4jApi.getFollowersIds(twitter,ScreenName,cursor);
	}
	
	/**
	 * use to get followerslist 
	 * @param twitter
	 * @return followersList
	 * @throws TwitterException 
	 * @throws IllegalStateException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getFollowersInfo(Twitter twitter,LinkedList<long[]> chunks) throws TwitterException {
	
		return twitter4jApi.getFollowersInfo(twitter,chunks);
	}
	
	/**
	 * use to create friendship 
	 * @param twitter
	 * @param user Screen Name
	 * @return friended data about user
	 * @throws TwitterException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getFriendShip(Twitter twitter,String ScreenName) throws TwitterException,Exception {
	
		return twitter4jApi.createFriendship(twitter,ScreenName);
	}
	/**
	 * use to create friendship 
	 * @param twitter
	 * @param user id
	 * @return friended data about user
	 * @throws TwitterException 
	 * @throws Exception
	 */
	public ArrayList<Map<String, Object>> getFriendShip(Twitter twitter,Long ScreenName) throws TwitterException,Exception {
	
		return twitter4jApi.createFriendship(twitter,ScreenName);
	}
	
	/**
	 * use to get users by searching keyword in their profiles
	 * @param twitter
	 * @param keyword
	 * @return
	 * @throws TwitterException
	 */
	public ArrayList<Map<String, Object>> getUsers(Twitter twitter, String keyword,ArrayList<Long> friensList) throws TwitterException{
		ArrayList<Map<String, Object>> usersList = twitter4jApi.getUsersProfiles(twitter, keyword,friensList);
		return usersList;
	}
	/**
	 * use to get twitter instance
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 * @param accessTokenSecret
	 * @return
	 */

	public Twitter getTwitterInstance(String consumerKey, String consumerSecret, String accessToken,
			String accessTokenSecret) throws TwitterException {
		return twitter4jApi.getTwitterInstance(consumerKey, consumerSecret, accessToken, accessTokenSecret);
	}
}
