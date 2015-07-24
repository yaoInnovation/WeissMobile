package com.weiss.mobile;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;

/**
 * Created by Yao on 7/23/15.
 */
public class DialogUtil extends AsyncTask<Void, Void, Void> {
    private static final String USER_AGENT = "Mozilla/5.0";
    private static String hostname = "localhost";
    private static int port = 8000;
    private static int fid = -1;
    private static final String TAG = "DialogUtil";

    private static final String initURL = "/api/init";
    private static final String inqueryURL = "/api/inquire";
    private static final String closeURL = "/api/close";


    private boolean inited = false;
    private ArrayList<String> response = null;

    public DialogUtil() {

    }

    @Override
    protected Void doInBackground(Void... params) {
        return null;
    }



    public ArrayList<String> init() {
        response = new ArrayList<String>();

        String url = "http://"+hostname+":"+port+initURL;

        String responseString = getRequest(url);
        if(responseString == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();

        //Get fid
        JsonElement fidObj = jsonParser.parse(responseString).getAsJsonObject().get("fid");
        response.add(fidObj.toString());

        //Get greetings
        JsonElement greetingsObj = jsonParser.parse(responseString).getAsJsonObject().get("response");
        response.add(fidObj.toString());

        return response;
    }

    public ArrayList<String> query(int fid, String query) {
        if(inited == false) {
            return null;
        }
        String url = hostname+":"+port+inqueryURL;
        String responseString = queryPostRequest(url, query);
        if(responseString == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();

        //Get greetings
        JsonElement greetingsObj = jsonParser.parse(responseString).getAsJsonObject().get("response");
        response.add(greetingsObj.toString());

        response = new ArrayList<String>();

        return response;
    }

    public void close() {
        if(inited == false) {
            return ;
        }

        String url = hostname+":"+port+closeURL;
        closePostRequest(url);

    }

    /**
     * @brief getRequest template (currently used by init)
     * @param url
     * @return
     */
    private String getRequest(String url) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);
            return response.toString();

        }catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getClass().getSimpleName());
            Log.d(TAG, "URL:"+url);
            return null;
        }
    }

    /**
     * @brief send Query and get response
     * @param URL
     * @param query
     * @return
     */
    public String queryPostRequest(String URL, String query) {
        Gson gson= new Gson();
        HttpPost post = new HttpPost(URL);
        Query q = new Query(this.fid, query);
        HttpClient httpClient = new DefaultHttpClient();

        try {
            StringEntity postingString = new StringEntity(gson.toJson(q));//convert query to json
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");
            HttpResponse response = httpClient.execute(post);
            return response.toString();
        } catch(Exception e) {
            Log.d(TAG, e.getClass().getSimpleName());
            return null;
        }finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * @brief Close the session
     * @param URL
     */
    public void closePostRequest(String URL) {
        Gson gson= new Gson();
        HttpPost post = new HttpPost(URL);

        HttpClient httpClient = new DefaultHttpClient();

        try {
            StringEntity postingString = new StringEntity(gson.toJson(this.fid));//convert query to json
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");

        } catch(Exception e) {
            Log.d(TAG, e.getClass().getSimpleName());
        }finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private class Query {
        int fid = 0;
        String query;

        Query(int fid, String query) {
            this.fid = fid;
            this.query = query;
        }

    }
}
