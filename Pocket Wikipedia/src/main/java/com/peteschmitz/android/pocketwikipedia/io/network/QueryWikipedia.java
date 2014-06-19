package com.peteschmitz.android.pocketwikipedia.io.network;

import android.content.Context;
import android.util.Log;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.language.LanguageKey;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * Network request builder for Wikipedia.
 * <p/>
 * Created by Pete Schmitz on 3/25/14.
 */
public class QueryWikipedia extends Thread {

    private static final String TAG = "pwiki query wikipedia";

    public static final String REQUEST_NONE = "requestNone";
    public static final String REQUEST_FRONT_PAGE = "requestFrontPage";
    public static final String REQUEST_ARTICLE = "requestArticle";
    public static final String REQUEST_REDIRECT = "requestRedirect";

    public interface Callback {
        void onQuerySuccess(@NotNull QueryWikipedia query, @NotNull JSONObject object, String requestId);

        void onQueryFailure(Exception exception, String requestId);
    }

    private final Callback mCallback;
    private StringBuilder mBuilder = new StringBuilder();
    private String mRequestId;
    private volatile boolean mShouldContinue = true;

    public QueryWikipedia(@NotNull Context mContext, @Nullable Callback callback) {
        this(mContext, callback, REQUEST_NONE);
    }

    public QueryWikipedia(@NotNull Context mContext, @Nullable Callback callback, String requestId) {
        LanguageKey.setLanguage(mContext.getString(R.string.language_id));
        reset();
        setRequest(requestId);
        mCallback = callback;
    }

    public QueryWikipedia setRequest(String requestId){
        mRequestId = requestId;

        return this;
    }

    public QueryWikipedia reset(){
        mBuilder = new StringBuilder();
        mBuilder.append(Wikipedia.BASE_API_URL);

        return this;
    }

    @Override
    public void run() {
        if (!mShouldContinue) return;

        String request = mBuilder.toString();
        //Log.d(TAG, "Request: " + request);

        try {
            String response = readJsonFromUrl(request);
            if (!mShouldContinue) return;

            JSONObject responseObject = new JSONObject(response);
            if (!mShouldContinue) return;

            if (mCallback != null) mCallback.onQuerySuccess(this, responseObject, mRequestId);
        } catch (IOException e) {
            if (mCallback != null) mCallback.onQueryFailure(e, mRequestId);
        } catch (JSONException e) {
            if (mCallback != null) mCallback.onQueryFailure(e, mRequestId);
        }
    }

    public static boolean hasRedirect(@NotNull JSONObject queryObject){
        try {
            JSONObject query = queryObject.getJSONObject("query");

            // No redirects found; load article
            if (!query.has("redirects")) {
                return false;
            }

            JSONArray redirects = query.getJSONArray("redirects");

            if (redirects.length() == 0) {
                return false;
            } else {
                return true;
            }

        } catch (JSONException e) {
            return false;
        }
    }

    private static String readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            return readAll(rd);
        } finally {
            is.close();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    @Nullable
    public static String getRedirect(@NotNull JSONObject queryObject){

        try {
            String redirect = queryObject.getJSONObject("query")
                    .getJSONArray("redirects")
                    .getJSONObject(0)
                    .getString("to");

            return redirect.replace(" ", "_");
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse redirection: " + e.getMessage());
            return null;
        }
    }

    private void appendList(List<String> additionList) {
        for (Iterator<String> itr = additionList.iterator(); itr.hasNext(); ) {
            mBuilder.append(itr.next());

            if (itr.hasNext()) mBuilder.append("|");
        }
    }

    private void appendList(String... additionList) {
        for (int i = 0; i < additionList.length; i++) {
            mBuilder.append(additionList[i]);
            if (i < additionList.length - 1) mBuilder.append("|");
        }
    }

    public QueryWikipedia actionParse() {
        mBuilder.append(Wikipedia.ACTION_PARSE);
        return this;
    }

    public QueryWikipedia actionQuery() {
        mBuilder.append(Wikipedia.ACTION_QUERY);
        return this;
    }

    public QueryWikipedia properties(String... properties) {
        mBuilder.append(Wikipedia.PROPERTIES);
        appendList(properties);
        return this;
    }

    public QueryWikipedia imageProperties(String... imageProperties) {
        mBuilder.append(Wikipedia.IMAGE_INFO_PROPERTIES);
        appendList(imageProperties);
        return this;
    }

    public QueryWikipedia pageMain() {
        mBuilder.append(Wikipedia.PAGE_MAIN);
        return this;
    }

    public QueryWikipedia page(String article) {
        mBuilder.append(Wikipedia.PAGE).append(article);
        return this;
    }

    public QueryWikipedia mobileFormat() {
        mBuilder.append(Wikipedia.MOBILE_FORMAT);
        return this;
    }

    public QueryWikipedia title(String title) {
        mBuilder.append(Wikipedia.TITLES).append(title);
        return this;
    }

    public QueryWikipedia titles(List<String> titles) {
        mBuilder.append(Wikipedia.TITLES);
        appendList(titles);
        return this;
    }

    public QueryWikipedia redirects() {
        mBuilder.append(Wikipedia.REDIRECTS);
        return this;
    }

    public QueryWikipedia list(String type) {
        mBuilder.append(Wikipedia.LIST).append(type);
        return this;
    }

    public QueryWikipedia randomLimit(int limit) {
        mBuilder.append(Wikipedia.RANDOM_LIMIT).append(Integer.toString(limit));
        return this;
    }

    public QueryWikipedia randomNamespace(int namespaceId) {
        mBuilder.append(Wikipedia.RANDOM_NAMESPACE).append(Integer.toString(namespaceId));
        return this;
    }

    public QueryWikipedia firstSection(){
        mBuilder.append(Wikipedia.SECTION_FIRST);

        return this;
    }

    public void setShouldContinue(boolean shouldContinue){
        mShouldContinue = shouldContinue;
    }


}
