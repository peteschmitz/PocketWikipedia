package com.peteschmitz.android.pocketwikipedia.activity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.abs.SearchableWikipediaActivity;
import com.peteschmitz.android.pocketwikipedia.constant.BundleKey;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.data.ArticleSummary;
import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;
import com.peteschmitz.android.pocketwikipedia.io.network.QueryWikipedia;
import com.peteschmitz.android.pocketwikipedia.io.network.WikiArticleImageTask;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.view.ViewFactory;
import com.peteschmitz.android.pocketwikipedia.view.article.RandomArticleOverviewView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

/**
 * Activity for displaying front page information from Wikipedia's language-specific landing page.
 * <p/>
 * Created by Pete Schmitz on 3/6/14
 */
public class LandingActivity
        extends SearchableWikipediaActivity
        implements QueryWikipedia.Callback, WikiLinkUtils.LinkListener, RandomArticleOverviewView.Callback {

    private LinearLayout mProgressLayout;
    private LinearLayout mContainerLayout;
    private Handler mHandler = new Handler();
    private FrontPageSectionData[] mFrontPageSectionData;
    private RandomArticleOverviewView mRandomArticle;
    private View mImageReloadIcon;
    private boolean mNewRandomEnabled = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            WikiArticleImageTask.ARTICLE_CACHE.clear();
        }

        setContentView(R.layout.landing);
        findIds();

        attachWikipediaSearch();

        loadMainPage(savedInstanceState);

        mSearchView.requestFocus();
    }

    private void findIds() {
        this.mProgressLayout = ((LinearLayout) findViewById(R.id.landing_progress_layout));
        this.mContainerLayout = ((LinearLayout) findViewById(R.id.landing_container_layout));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getSherlock().getMenuInflater().inflate(R.menu.landing_menu, menu);

        return true;
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_close_application:
                finish();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void onQuerySuccess(@NotNull QueryWikipedia query, @NotNull JSONObject object, String requestId) {
        if (requestId.equals(QueryWikipedia.REQUEST_FRONT_PAGE)) {
            postFrontPageSections(FrontPageSectionData.parseSections(object));
        }
    }

    private void postFrontPageSections(final FrontPageSectionData[] sections) {
        for (FrontPageSectionData section : sections) {
            if (!section.items.isEmpty()) {
                for (FrontPageSectionData.ListItem listItem : section.items) {
                    listItem.buildSpanned();
                }
            } else {
                section.buildSpanned();
            }
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                buildMainPage(sections, null);
            }
        });
    }

    public void onQueryFailure(Exception exception, String requestId) {
    }

    private void loadMainPage(Bundle bundle) {
        toggleProgressBar(true);
        if (bundle != null) {
            this.mFrontPageSectionData = ((FrontPageSectionData[]) bundle.getParcelableArray(BundleKey.FRONT_PAGE_SECTIONS));
        }
        if (this.mFrontPageSectionData != null) {
            ArticleSummary articleSummary = bundle == null ? null : (ArticleSummary) bundle.getParcelable(BundleKey.RANDOM_ARTICLE_SUMMARY);
            buildMainPage(mFrontPageSectionData, articleSummary);
        } else {
            new QueryWikipedia(this, this, QueryWikipedia.REQUEST_FRONT_PAGE)
                    .actionParse()
                    .properties(Wikipedia.TEXT)
                    .pageMain()
                    .start();
        }
    }

    private void buildMainPage(FrontPageSectionData[] sections, @Nullable ArticleSummary randomArticle) {
        boolean insertedRandomArticle = false;
        this.mFrontPageSectionData = sections;
        toggleProgressBar(false);

        int numColors = getResources().getIntArray(R.array.placeholder_colors).length;

        for (int i = 0; i < sections.length; i++) {
            FrontPageSectionData section = sections[i];

            FrontPageSectionData.LayoutType layoutType = FrontPageSectionData.LayoutType.evaluate(section);
            switch (layoutType) {
                case LIST:

                    // Insert random article before the first list of articles
                    // (We're assuming all single listings are listed before article lists)
                    if (!insertedRandomArticle){
                        mImageReloadIcon = ViewFactory.createLandingTopicReload(
                                this.mContainerLayout,
                                getResources().getString(R.string.random_article)
                        );
                        mImageReloadIcon.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestNewRandomArticle();
                            }
                        });
                        mRandomArticle = ViewFactory.createLandingBodyRandom(this.mContainerLayout, this, i % numColors, randomArticle, this);
                        insertedRandomArticle = true;
                    }

                    ViewFactory.createLandingTopic(this.mContainerLayout, section.line);
                    ViewFactory.createLandingList(this.mContainerLayout, section, this, (i + 1) % numColors);
                    break;
                case SINGLE:
                    ViewFactory.createLandingTopic(this.mContainerLayout, section.line);
                    ViewFactory.createLandingBody(this.mContainerLayout, section, this, i % numColors);
            }
        }
    }

    private void requestNewRandomArticle(){
        if (mRandomArticle == null) return;

        if (mNewRandomEnabled ){
            mNewRandomEnabled = false;
            mRandomArticle.requestRandomArticle();

            if (mImageReloadIcon != null){
                mImageReloadIcon.setAlpha(0.5f);
            }
        }

    }

    private void toggleProgressBar(boolean show) {
        this.mProgressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mFrontPageSectionData != null) {
            outState.putParcelableArray(BundleKey.FRONT_PAGE_SECTIONS, this.mFrontPageSectionData);
        }

        if (mRandomArticle != null && mRandomArticle.getSummary() != null){
            outState.putParcelable(BundleKey.RANDOM_ARTICLE_SUMMARY, mRandomArticle.getSummary());
        }
    }

    public void onClick(String link) {
        WikiLinkUtils.defaultLinkBehavior(this, link);
    }

    @Override
    public void onNewSummary(ArticleSummary summary) {
        mNewRandomEnabled = true;

        if (mImageReloadIcon != null){
            mImageReloadIcon.setAlpha(1.0f);
        }
    }
}