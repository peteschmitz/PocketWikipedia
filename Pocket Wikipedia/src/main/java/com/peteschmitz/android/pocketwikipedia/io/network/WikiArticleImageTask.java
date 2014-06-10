package com.peteschmitz.android.pocketwikipedia.io.network;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.peteschmitz.android.pocketwikipedia.array.LimitedHashMap;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Asynctask for retrieving images of a Wikipedia article.
 * <p/>
 * Created by Pete Schmitz on 4/7/2014.
 */
public class WikiArticleImageTask extends AsyncTask<Void, Void, String> {

    public interface FullQueryListener {
        void onSuccess(String encodedArticleName, String url);

        void onFailure(String encodedArticleName);
    }

    private static final String TAG = "wmt image task";


    public static final Map<String, String> ARTICLE_CACHE =
            Collections.synchronizedMap(new LimitedHashMap<String, String>(300));
    public static final Map<String, String> IMAGE_CACHE =
            Collections.synchronizedMap(new LimitedHashMap<String, String>(300));

    private static enum Type {
        FULL_QUERY, PROVIDED_QUERY
    }

    private static final int MAX_IMAGES = 10;
    private static final Map<String, WikiArticleImageTask> sActiveFullQueries = new HashMap<String, WikiArticleImageTask>();
    private static final String[] mBlackListedWords = new String[]{"padlock", "blank"};
    private static final String[] mSupportedImageExtensions = new String[]{
            ".jpg", ".gif", ".png", ".bmp", ".jpeg"
    };

    private final String mEncodedArticleName;
    private List<String> mProvidedQuery;
    private Type mType;
    private List<FullQueryListener> mFullQueryListeners = new LinkedList<FullQueryListener>();

    @Nullable
    public static WikiArticleImageTask getActiveFullQuery(String encodedArticleName) {
        return sActiveFullQueries.get(encodedArticleName);
    }


    public WikiArticleImageTask(String encodedArticleName, @Nullable FullQueryListener listener) {
        mEncodedArticleName = encodedArticleName;

        if (listener != null) {
            mFullQueryListeners.add(listener);
        }

        mType = Type.FULL_QUERY;
    }

    public WikiArticleImageTask(@NotNull JSONArray providedQuery,
                                @NotNull String encodedArticleName,
                                @Nullable List<String> omissions) {
        mEncodedArticleName = encodedArticleName;

        mProvidedQuery = new LinkedList<String>();
        for (int i = 0; i < providedQuery.length(); i++) {
            try {
                String imageOption = providedQuery.getString(i);

                boolean banned = false;
                if (omissions != null) {
                    for (String omission : omissions) {
                        if (omission.equalsIgnoreCase(imageOption)) {
                            banned = true;
                            break;
                        }
                    }
                }

                if (banned || imageContainsBlackListedWord(imageOption) ||
                        !imageExtensionIsSupported(imageOption)) {
                    continue;
                }

                mProvidedQuery.add(imageOption);

            } catch (JSONException e) {
                Log.w(TAG, "Couldn't retrieve JSON string during wiki image suggestions");
                break;
            }
        }

        mType = Type.PROVIDED_QUERY;
    }

    public void addFullQueryListener(@NotNull FullQueryListener listener) {
        mFullQueryListeners.add(listener);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mType == Type.FULL_QUERY) {
            sActiveFullQueries.put(mEncodedArticleName, this);
        }
    }

    @Override
    protected String doInBackground(Void... params) {

        switch (mType) {
            case FULL_QUERY:
                if (ARTICLE_CACHE.containsKey(mEncodedArticleName)) {
                    return ARTICLE_CACHE.get(mEncodedArticleName);
                }

                return getArticleImageUrl(mEncodedArticleName);

            case PROVIDED_QUERY:
                if (ARTICLE_CACHE.containsKey(mEncodedArticleName)) {
                    boolean cacheIsBanned = false;
                    String cached = ARTICLE_CACHE.get(mEncodedArticleName);

                    if (mProvidedQuery != null) {
                        cacheIsBanned = true;
                        for (String option : mProvidedQuery) {
                            if (option.equalsIgnoreCase(cached)) {
                                cacheIsBanned = false;
                                break;
                            }
                        }


                    }

                    if (!cacheIsBanned) return cached;
                }

                String bestOption = chooseBestImage(mProvidedQuery, mEncodedArticleName);
                return getFullImageUrl(bestOption);
        }

        return null;

    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (mType == Type.FULL_QUERY) {
            // Cache result
            ARTICLE_CACHE.put(mEncodedArticleName, s);
            IMAGE_CACHE.put(getOriginalImageName(s), s);

            sActiveFullQueries.remove(mEncodedArticleName);

            for (FullQueryListener listener : mFullQueryListeners) {
                listener.onSuccess(mEncodedArticleName, s);
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        if (mType == Type.FULL_QUERY) {

            sActiveFullQueries.remove(mEncodedArticleName);

            for (FullQueryListener listener : mFullQueryListeners) {
                listener.onFailure(mEncodedArticleName);
            }
        }
    }

    /**
     * Retrieve the URL of an article image from the wiki-commons website. Note: supplied query
     * should be decoded.
     */
    private String getArticleImageUrl(final String query) {
        String imageFileName = "";

        // Quick image search
        JSONObject queryObject = performQuickImageSearch(query);
        if (queryObject != null) {
            imageFileName = chooseBestImage(getQuickSearchImages(queryObject), query);
        }
        if (isCancelled()) return "";

        // Full image search
        if (TextUtils.isEmpty(imageFileName)) {
            queryObject = performFullImageSearch(query);

            if (queryObject != null) {
                imageFileName = chooseBestImage(getFullSearchImages(queryObject), query);
            }
        }
        if (isCancelled()) return "";

        // Cancel if both quick and full searches failed
        if (TextUtils.isEmpty(imageFileName)) {
            ARTICLE_CACHE.put(query, "");
            return "";
        }

        String url = getFullImageUrl(imageFileName);
        if (isCancelled()) return "";

        return url;
    }

    public static String getFullImageUrl(String imageFileName) {
        if (TextUtils.isEmpty(imageFileName)) return "";

        // Replaces spaces with underscores
        imageFileName = imageFileName.replaceAll(" ", "_");

        // Generate an MD5 hash of the file name
        String hash = getHash(imageFileName);
        imageFileName = Uri.encode(imageFileName);

        // Build image URL
        String url = Wikipedia.UPLOAD_COMMONS_URL +
                hash.charAt(0) + "/" +
                hash.substring(0, 2) + "/" +
                imageFileName;

        // Check if url is valid; change to lang if it isn't
        if (!URLExists(url)) {
            //Log.d("logd", "switch to lang: " + query);
            return Wikipedia.UPLOAD_LANG_URL +
                    hash.charAt(0) + "/" +
                    hash.substring(0, 2) + "/" +
                    imageFileName;
        }

        return url;
    }

    private ArrayList<String> getQuickSearchImages(JSONObject query) {
        ArrayList<String> images = new ArrayList<String>();
        try {
            JSONArray imageArray = query.getJSONObject("parse").getJSONArray("images");

            for (int i = 0; i < imageArray.length(); i++) {
                String imageName = imageArray.get(i).toString();

                // Skip image if extension is unsupported
                if (!imageExtensionIsSupported(imageName)) continue;

                // Skip image if it contains a blacklisted word
                if (imageContainsBlackListedWord(imageName)) continue;

                images.add(imageName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return images;
    }

    public static boolean imageContainsBlackListedWord(String imageName) {
        imageName = imageName.toLowerCase();
        for (String blackWord : mBlackListedWords) {
            if (imageName.contains(blackWord)) return true;
        }

        return false;
    }

    private ArrayList<String> getFullSearchImages(JSONObject query) {
        ArrayList<String> images = new ArrayList<String>();
        try {
            JSONObject pages = query.getJSONObject("query").getJSONObject("pages");
            JSONObject page = pages.getJSONObject((String) pages.keys().next());

            if (!page.has("images")) return images;

            JSONArray imageArray = page.getJSONArray("images");

            for (int i = 0; i < imageArray.length(); i++) {
                JSONObject imageObject = imageArray.getJSONObject(i);
                String imageName = imageObject.getString("title").split(":")[1];

                // Skip image if extension is unsupported
                if (!imageExtensionIsSupported(imageName)) continue;

                // Skip image if it contains a blacklisted word
                if (imageContainsBlackListedWord(imageName)) continue;

                images.add(imageName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return images;
    }

    /**
     * Search wikipedia for images of supplied article query. Only the first section
     * is queried for images. Returns a string {@code JSONObject} of the query.
     */
    private JSONObject performQuickImageSearch(String articleQuery) {

        String getRequest =
                Wikipedia.BASE_API_URL +
                        Wikipedia.ACTION_PARSE +
                        Wikipedia.PAGE + articleQuery +
                        Wikipedia.PROPERTIES + "images" +
                        Wikipedia.SECTION_FIRST;

        String networkResponse = performHTTPRequest(getRequest);

        try {
            return new JSONObject(networkResponse);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Search wikipedia for all images (up to {@code MAX_IMAGES}) for the supplied article query.
     * This full query should only be used when {@link #performQuickImageSearch(String)} doesn't
     * provide a sufficient result.
     */
    private JSONObject performFullImageSearch(String articleQuery) {

        String getRequest =
                Wikipedia.BASE_API_URL +
                        Wikipedia.ACTION_QUERY +
                        Wikipedia.TITLES + articleQuery +
                        Wikipedia.PROPERTIES + "images" +
                        Wikipedia.IMAGE_LIMIT + MAX_IMAGES;

        String networkResponse = performHTTPRequest(getRequest);

        try {
            return new JSONObject(networkResponse);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String performHTTPRequest(String getRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(getRequest);

        try {
            HttpResponse response = client.execute(get);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {

                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

            } else {
                Log.e(TAG, "Http status invalid, code: " + statusCode);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return stringBuilder.toString();
    }

    /**
     * Convenience function; get MD5 of a string
     */
    private static String getHash(String base) {

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] crypt = digest.digest(base.getBytes());
            return toHex(crypt);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not supported");
        }
    }

    private String chooseBestImage(List<String> images, String query) {

        if (images == null || images.isEmpty()) return "";

        List<String> imageNames = new LinkedList<String>();
        List<String> queryNames = new LinkedList<String>();

        // Split query words; omit words that are 1 or 2 chars
        for (String queryName : query.split("_")) {
            if (queryName.length() > 2) queryNames.add(queryName.toLowerCase());
        }

        // Collect relevant image names (anything that contains a query word)
        lp:
        for (String imageName : images) {
            for (String queryName : queryNames) {
                if (imageName.toLowerCase().contains(queryName)) {
                    imageNames.add(imageName);
                    continue lp;
                }
            }

        }

        return imageNames.isEmpty() ? images.get(0) : imageNames.get(0);
    }

    public static boolean imageExtensionIsSupported(String imageName) {
        imageName = imageName.toLowerCase();

        for (String extension : mSupportedImageExtensions) {
            if (imageName.toLowerCase().contains(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean URLExists(String url) {
        HttpClient client = new DefaultHttpClient();
        HttpHead head = new HttpHead(url);

        try {
            HttpResponse response = client.execute(head);
            return response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static String toHex(byte[] data) {
        char[] chars = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            chars[i * 2] = HEX_DIGITS[(data[i] >> 4) & 0xf];
            chars[i * 2 + 1] = HEX_DIGITS[data[i] & 0xf];
        }
        return new String(chars);
    }

    public static String getOriginalImageName(String image) {
        String[] nonUrlSplit = image.split("/");

        return nonUrlSplit[nonUrlSplit.length - 1];
    }

    private String taskDescription = "WikiArticleImageTask";

    public static List<String> getImages(JSONArray providedQuery) {
        List<String> images = new ArrayList<String>();

        for (int i = 0; i < providedQuery.length(); i++) {
            try {
                String image = providedQuery.getString(i);
                if (!imageContainsBlackListedWord(image) && imageExtensionIsSupported(image)) {
                    images.add(image);
                }

            } catch (JSONException e) {
                Log.w(TAG, "Couldn't retrieve JSON string during wiki image suggestions");
                break;
            }
        }

        return images;
    }
}
