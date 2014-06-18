package com.peteschmitz.android.pocketwikipedia.activity;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.abs.SearchableDrawerActivity;
import com.peteschmitz.android.pocketwikipedia.adapter.ArticleDataAdapter;
import com.peteschmitz.android.pocketwikipedia.adapter.ArticleDrawerAdapter;
import com.peteschmitz.android.pocketwikipedia.constant.BundleKey;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.peteschmitz.android.pocketwikipedia.io.local.Preferences;
import com.peteschmitz.android.pocketwikipedia.io.network.QueryWikipedia;
import com.peteschmitz.android.pocketwikipedia.io.network.WikiArticleImageTask;
import com.peteschmitz.android.pocketwikipedia.util.ImageEvaluations;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleDrawerHeader;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleHeaderView;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleImagePreviewView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Activity for displaying data of a Wikipedia article.
 * <p/>
 * Created by Pete Schmitz on 4/26/14
 */
public class ArticleActivity
        extends SearchableDrawerActivity<ArticleData>
        implements QueryWikipedia.Callback, ArticleData.OnItemBuiltListener, WikiLinkUtils.LinkListener,
        ImageEvaluation.BuildListener {

    private static final String TAG = "pwiki article";
    private Handler mHandler = new Handler();
    private String mCurrentArticle;
    private ArticleDataAdapter mAdapter;
    private ListView mListView;
    private ArticleHeaderView mListHeader;
    private int mLinkColor;
    private int mBackgroundColor;
    private int mColorIndex;
    private int mLowlightColor;
    private ArticleDrawerAdapter mArticleDrawerAdapter;
    private String mTopicQueue;
    private ArticleImagePreviewView mArticleImageView;
    private ArticleDrawerHeader mArticleDrawerHeader;

    private String mCurrentHeaderIcon;
    private String mCurrentBackground;
    private String mActiveParseData;
    private int mActiveParseIndex;
    private int mActiveParseSkipIndex;
    private ArticleData mTopicItemQueue;


    public static void launch(Activity activity, String encodedArticle) {
        Intent intent = new Intent(activity, ArticleActivity.class);
        Uri.Builder uri = new Uri.Builder();
        uri.appendPath("wiki");
        uri.appendPath(encodedArticle);

        intent.setData(uri.build());

        activity.startActivity(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findBundleData(savedInstanceState);
        setColor(savedInstanceState);
        setContentView(R.layout.article);

        buildDrawer();
        attachWikipediaSearch();

        findIds();

        buildAdapter();
        requestArticle(savedInstanceState, mCurrentArticle);

        // Open drawer on first launch
        if (Preferences.getShowArticleDrawer(this)) {
            Preferences.setShowArticleDrawer(this, false);
            openDrawer();
        }
    }

    private void setColor(@Nullable Bundle bundle) {

        // Grab bundled color index or choose a random index
        if (bundle == null || bundle.getInt(BundleKey.COLOR_INDEX, -1) == -1) {
            mColorIndex = new Random(System.currentTimeMillis()).nextInt(getResources().getIntArray(R.array.placeholder_colors).length);
        } else {
            mColorIndex = bundle.getInt(BundleKey.COLOR_INDEX);
        }

        mLinkColor = getResources().getIntArray(R.array.text_colors)[mColorIndex];
        mLowlightColor = getResources().getIntArray(R.array.lowlight_colors)[mColorIndex];
        mBackgroundColor = getResources().getIntArray(R.array.placeholder_colors)[mColorIndex];
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(BundleKey.ARTICLE, mCurrentArticle);
        outState.putString(BundleKey.ARTICLE_ICON, mCurrentHeaderIcon);
        outState.putString(BundleKey.ARTICLE_BACKGROUND, mCurrentBackground);
        outState.putInt(BundleKey.COLOR_INDEX, mColorIndex);

        if (!TextUtils.isEmpty(mActiveParseData)) {
            outState.putString(BundleKey.ARTICLE_ACTIVE_PARSE_DATA, mActiveParseData);
            outState.putInt(BundleKey.ARTICLE_ACTIVE_PARSE_INDEX, mActiveParseIndex);
        }

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArray(BundleKey.ARTICLE_DATA, mAdapter.getItems());
        }

        ArrayList<ImageEvaluation> evaluations = mArticleImageView.getEvaluations();
        if (evaluations != null) {
            outState.putParcelableArray(BundleKey.IMAGES, evaluations.toArray(new ImageEvaluation[evaluations.size()]));
        }
        mArticleImageView.dispose();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getSherlock().getMenuInflater().inflate(R.menu.article_menu, menu);

        return true;
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share_article:
                WikiLinkUtils.defaultShareBehavior(this, WikiLinkUtils.getArticleURL(mCurrentArticle));
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void findHeaderIconImage(String encodedArticle) {
        new WikiArticleImageTask(encodedArticle, new WikiArticleImageTask.FullQueryListener() {
            @Override
            public void onSuccess(String encodedArticleName, String url) {
                if (TextUtils.isEmpty(url)) {
                    return;
                }

                setHeaderIcon(url);
            }

            @Override
            public void onFailure(String encodedArticleName) {

            }
        })
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setHeaderIcon(final String url) {
        mCurrentHeaderIcon = url;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListHeader.setIconURL(url);
                mArticleDrawerHeader.setImage(url);
            }
        });
    }

    public void setBackgroundImage(final String url) {
        mCurrentBackground = url;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListHeader.setBackgroundURL(url);
            }
        });
    }

    public void setHeaderTitle(final String encodedArticle) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListHeader.setLabel(Uri.decode(encodedArticle.replace("_", " ")));
                mArticleDrawerHeader.setLabel(Uri.decode(encodedArticle.replace("_", " ")));
            }
        });
    }

    private void findIds() {
        this.mListView = ((ListView) findViewById(R.id.article_list_view));
    }

    protected ArrayAdapter<ArticleData> getDrawerAdapter() {
        if (this.mArticleDrawerAdapter == null) {
            this.mArticleDrawerAdapter = new ArticleDrawerAdapter(this);
        }
        return this.mArticleDrawerAdapter;
    }

    private void buildAdapter() {
        mArticleImageView = new ArticleImagePreviewView(this, mBackgroundColor);
        this.mAdapter = new ArticleDataAdapter(this, mCurrentArticle, this, mArticleImageView, mArticleImageView, mColorIndex);
        this.mListHeader = new ArticleHeaderView(this, this.mBackgroundColor);

        mListHeader.setClickable(false);
        mListHeader.setGalleryPreviewListener(mArticleImageView);
        this.mListView.addHeaderView(this.mListHeader);
        mListView.addHeaderView(mArticleImageView);

        this.mListView.setDivider(null);
        this.mListView.setAdapter(this.mAdapter);

        setHeaderTitle(mCurrentArticle);
    }

    private void findBundleData(Bundle bundle) {

        // Prioritize bundled article name
        if (bundle != null) {
            if (!TextUtils.isEmpty(bundle.getString(BundleKey.ARTICLE))) {
                mCurrentArticle = bundle.getString(BundleKey.ARTICLE);

                return;
            }
        }

        // Search intent URI for article name since bundle check failed
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            throw new IllegalStateException("No article information was provided for Article Activity");
        }

        String[] splits = uri.toString().split("/wiki/");
        String article = splits[splits.length - 1];
        String[] articleParams = article.split("%23|#");

        mCurrentArticle = articleParams[0];

        // Remember postfix so we can auto-seek to this topic later
        if (articleParams.length > 1) {
            mTopicQueue = Uri.decode(articleParams[1].replace("_", " "));
        }

    }

    private void requestArticle(Bundle bundle, String encodedArticle) {

        if (bundle != null) {

            Parcelable[] articleData = bundle.getParcelableArray(BundleKey.ARTICLE_DATA);
            final String articleParseData = bundle.getString(BundleKey.ARTICLE_ACTIVE_PARSE_DATA);
            String articleIcon = bundle.getString(BundleKey.ARTICLE_ICON);
            String articleBackground = bundle.getString(BundleKey.ARTICLE_BACKGROUND);
            int parseIndex = bundle.getInt(BundleKey.ARTICLE_ACTIVE_PARSE_INDEX);
            Parcelable[] evaluations = bundle.getParcelableArray(BundleKey.IMAGES);

            if (!TextUtils.isEmpty(articleParseData) || articleData != null) {

                if (articleData != null) {
                    for (Parcelable parcel : articleData) {
                        ArticleData data = (ArticleData) parcel;
                        mAdapter.add(data);

                        if (!TextUtils.isEmpty(data.title)) {
                            getDrawerAdapter().add(data);
                        }
                    }
                }

                // Set images
                if (evaluations != null) {
                    ArrayList<ImageEvaluation> imageEvaluations = new ArrayList<ImageEvaluation>();
                    for (Parcelable parcel : evaluations) {
                        imageEvaluations.add((ImageEvaluation) parcel);
                    }
                    mArticleImageView.setPreviews(imageEvaluations);
                }

                // Set icon
                if (!TextUtils.isEmpty(articleIcon)) {
                    setHeaderIcon(articleIcon);
                }

                // Set background
                if (!TextUtils.isEmpty(articleBackground)) {
                    setBackgroundImage(articleBackground);
                }

                if (!TextUtils.isEmpty(articleParseData)) {
                    mActiveParseSkipIndex = parseIndex;

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                processArticleRequest(new JSONObject(articleParseData));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            return null;
                        }
                    }
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                return;
            }

        }

        // Bundle failed; determine if redirection is needed
        requestRedirect(encodedArticle);
    }

    private void requestRedirect(String encodedArticle) {
        mCurrentArticle = encodedArticle;
        new QueryWikipedia(this, this, QueryWikipedia.REQUEST_REDIRECT)
                .actionQuery()
                .title(encodedArticle)
                .redirects()
                .start();
    }

    private void loadArticle(final String encodedArticle) {

        mCurrentArticle = encodedArticle;

        // Async must be started on UI thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                findHeaderIconImage(encodedArticle);
            }
        });

        setHeaderTitle(mCurrentArticle);

        new QueryWikipedia(this, this, QueryWikipedia.REQUEST_ARTICLE)
                .actionParse()
                .mobileFormat()
                .page(this.mCurrentArticle)
                .properties(Wikipedia.TEXT, Wikipedia.IMAGES)
                .start();
    }

    public void onQuerySuccess(@NotNull QueryWikipedia query, @NotNull JSONObject object, String requestId) {
        if (requestId.equals(QueryWikipedia.REQUEST_REDIRECT)) {
            processRedirect(object);


        } else if (requestId.equals(QueryWikipedia.REQUEST_ARTICLE)) {
            processArticleRequest(object);
        }
    }

    private void processArticleRequest(@NotNull JSONObject object) {
        mActiveParseData = object.toString();

        try {
            // Log.d("pwiki article", "Getting json object...");
            JSONObject body = object.getJSONObject("parse");

            final JSONArray images = body.getJSONArray("images");

            // Async must be started on UI thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    findBackgroundImage(images);
                    mArticleImageView.setImages(WikiArticleImageTask.getImages(images));
                }
            });

            String text = body.getJSONObject("text").getString("*");

            //Log.d("pwiki article", "Creating data");
            ArticleData data = new ArticleData(this, text, this, this, this, mLinkColor, mLowlightColor);

            //Log.d("pwiki article", "Finished data");
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            mActiveParseData = null;

            if (mTopicItemQueue != null) {
                mListView.post(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d("pwiki topic queue", "scroll to : " + getDrawerAdapter().getPosition(mTopicItemQueue));
                        onDrawerItemClick(null, null, getDrawerAdapter().getPosition(mTopicItemQueue) + 1, 0L);
                    }
                });
            }
        }
    }

    private void findBackgroundImage(@NotNull JSONArray images) {
        List<String> omissions = new LinkedList<String>();
        if (!TextUtils.isEmpty(mCurrentHeaderIcon)) {
            omissions.add(WikiArticleImageTask.getOriginalImageName(mCurrentHeaderIcon));
        }

        new WikiArticleImageTask(images, mCurrentArticle, omissions) {
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                if (TextUtils.isEmpty(s)) {
                    if (!TextUtils.isEmpty(mCurrentHeaderIcon)) {
                        s = mCurrentHeaderIcon;
                    } else {
                        return;
                    }
                }

                setBackgroundImage(s);
            }
        }
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void processRedirect(@NotNull JSONObject object) {
        try {
            JSONObject query = object.getJSONObject("query");

            // No redirects found; load article
            if (!query.has("redirects")) {
                loadArticle(mCurrentArticle);
                return;
            }

            JSONArray redirects = query.getJSONArray("redirects");

            if (redirects.length() == 0) {
                loadArticle(mCurrentArticle);
            } else {

                // Redirect found; recursively re-query until an article is found
                String redirect = redirects.getJSONObject(0).getString("to");
                requestRedirect(redirect.replace(" ", "_"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onQueryFailure(Exception exception, String requestId) {
    }

    public void onItemBuilt(final @NotNull ArticleData item) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                if (mActiveParseSkipIndex == 0 || mActiveParseIndex > mActiveParseSkipIndex) {
                    addArticleItem(item);
                }

                ++mActiveParseIndex;
            }
        });
    }

    private void addArticleItem(@NotNull ArticleData item) {

        mAdapter.add(item);
        if (!TextUtils.isEmpty(item.title)) {
            getDrawerAdapter().add(item);

            if (!TextUtils.isEmpty(mTopicQueue) && item.title.equalsIgnoreCase(mTopicQueue)) {

                mTopicItemQueue = item;
            }
        }
    }

    private void buildDrawer() {
        ListView listView = getDrawerListView();
        mArticleDrawerHeader = new ArticleDrawerHeader(this, mBackgroundColor);
        listView.addHeaderView(mArticleDrawerHeader);

        initDrawer();
    }

    @Override
    protected void onDrawerItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onDrawerItemClick(parent, view, position, id);

        final int i = position == 0 ? 0 : mAdapter.getPosition(mArticleDrawerAdapter.getItem(position - 1)) + 2;

        closeDrawer();

        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(i);
            }
        });
    }

    public void onClick(String link) {

        if (link.contains("File:")) {

            if (mArticleImageView != null) {
                String[] splits = link.split("/");
                String normalized = Uri.decode(splits[splits.length - 1]).replaceAll("_", " ");
                ImageEvaluation image = ImageEvaluations.withNormalizedName(mArticleImageView.getEvaluations(), normalized);

                if (image != null) {
                    mArticleImageView.onGalleryPreviewClick(image.getImageUrl());
                }
            }
        } else {
            WikiLinkUtils.defaultLinkBehavior(this, link);
        }


    }

    @Override
    public void onEvaluationBuilt(@NotNull final ImageEvaluation evaluation) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mArticleImageView.add(evaluation);
            }
        });
    }
}