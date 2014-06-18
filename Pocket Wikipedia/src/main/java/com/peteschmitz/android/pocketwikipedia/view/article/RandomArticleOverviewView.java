package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.data.ArticleSummary;
import com.peteschmitz.android.pocketwikipedia.io.network.QueryWikipedia;
import com.peteschmitz.android.pocketwikipedia.io.network.WikiArticleImageTask;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Pete Schmitz on 6/17/2014.
 */
public class RandomArticleOverviewView extends ArticleOverviewView implements QueryWikipedia.Callback {

    public interface Callback{
        void onNewSummary(ArticleSummary summary);
    }

    private static final String REQUEST_RANDOM_ARTICLE = "requestRandomArticle";

    private QueryWikipedia mCurrentRequest;
    private WikiArticleImageTask mCurrentImageTask;
    private String mCurrentArticle;
    private Context mContext;
    private ArticleSummary mSummary;
    private Callback mSummaryCallback;

    public RandomArticleOverviewView(@NotNull Context context,
                                     @Nullable AttributeSet attrs,
                                     @Nullable WikiLinkUtils.LinkListener linkListener,
                                     boolean restrictWidth,
                                     float heightPercentage,
                                     @Nullable ArticleSummary summary,
                                     @Nullable Callback summaryCallback) {
        super(context, attrs, linkListener, restrictWidth, heightPercentage);

        mContext = context;
        mSummaryCallback = summaryCallback;

        mHolder.imageContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCurrentArticle();
            }
        });

        if (summary == null){
            requestRandomArticle();
        } else {
            onNewSummary(summary);
        }
    }

    private void launchCurrentArticle(){
        if (TextUtils.isEmpty(mCurrentArticle)) return;

        mLinkListener.onClick("/wiki/" + mCurrentArticle);
    }

    @Nullable
    public ArticleSummary getSummary(){
        return mSummary;
    }

    public void requestRandomArticle(){
        if (mCurrentRequest != null){
            mCurrentRequest.setShouldContinue(false);
        }
        if (mCurrentImageTask != null){
            mCurrentImageTask.cancel(true);
        }
        Picasso.with(mContext).cancelRequest(mHolder.imageContainer);

        mCurrentRequest = new QueryWikipedia(mContext, this, REQUEST_RANDOM_ARTICLE)
                .actionQuery()
                .list(Wikipedia.RANDOM)
                .randomLimit(1)
                .randomNamespace(Wikipedia.NAMESPACE_MAIN);

        mCurrentRequest.start();
    }

    private void requestRedirect(@NotNull QueryWikipedia query, String encodedArticle) {
        mCurrentArticle = encodedArticle;

        query.reset().setRequest(QueryWikipedia.REQUEST_REDIRECT)
                .actionQuery()
                .title(encodedArticle)
                .redirects()
                .run();
    }

    @Override
    public void onQuerySuccess(@NotNull QueryWikipedia query, @NotNull final JSONObject object, String requestId) {
        if (requestId.equals(QueryWikipedia.REQUEST_REDIRECT)){
            if (QueryWikipedia.hasRedirect(object)){
                requestRedirect(query, QueryWikipedia.getRedirect(object));
            } else {
                query.reset().setRequest(QueryWikipedia.REQUEST_ARTICLE)
                        .actionParse()
                        .mobileFormat()
                        .page(mCurrentArticle)
                        .properties(Wikipedia.TEXT, Wikipedia.IMAGES)
                        .firstSection()
                        .run();
            }

        } else if (requestId.equals(REQUEST_RANDOM_ARTICLE)){
            try {
                JSONArray randomPages = object.getJSONObject("query").getJSONArray("random");
                if (randomPages.length() > 0){
                    String decodedTitle = randomPages.getJSONObject(0).getString("title");
                    String encodedTitle = decodedTitle.replace(" ", "_");
                    requestRedirect(query, encodedTitle);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (requestId.equals(QueryWikipedia.REQUEST_ARTICLE)){

            // Re-enter UI thread with article summary;
            final ArticleSummary summary = new ArticleSummary(object);
            post(new Runnable() {
                @Override
                public void run() {
                    onNewSummary(summary);
                }
            });
        }
    }

    @Override
    public void onQueryFailure(Exception exception, String requestId) {

    }

    private void onNewSummary(final ArticleSummary articleSummary){
        mSummary = articleSummary;

        mHolder.textView.post(new Runnable() {
            @Override
            public void run() {
                setText(articleSummary.getText());
            }
        });

        if (!TextUtils.isEmpty(articleSummary.getImageURL())){
            setBackgroundURL(articleSummary.getImageURL());
        } else {
            mHolder.imageContainer.post(new Runnable() {
                @Override
                public void run() {
                    resetPlaceholderAndImage();
                }
            });
        }

        if (TextUtils.isEmpty(articleSummary.getImageURL()) &&
                mSummary.getImages() != null &&
                mSummary.getImages().length() != 0){

            mCurrentImageTask = new WikiArticleImageTask(mSummary.getImages(), mCurrentArticle, null){
                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);

                    if (TextUtils.isEmpty(s)) return;

                    articleSummary.setImageUrl(s);
                    setBackgroundURL(s);
                }
            };

            mCurrentImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }

        if (mSummaryCallback != null){
            mSummaryCallback.onNewSummary(articleSummary);
        }
    }
}
