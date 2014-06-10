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
import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;
import com.peteschmitz.android.pocketwikipedia.io.network.QueryWikipedia;
import com.peteschmitz.android.pocketwikipedia.io.network.WikiArticleImageTask;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.view.ViewFactory;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

/**
 * Activity for displaying front page information from Wikipedia's language-specific landing page.
 * <p/>
 * Created by Pete Schmitz on 3/6/14
 */
public class LandingActivity
        extends SearchableWikipediaActivity
        implements QueryWikipedia.Callback, WikiLinkUtils.LinkListener {

    private LinearLayout mProgressLayout;
    private LinearLayout mContainerLayout;
    private Handler mHandler = new Handler();
    private FrontPageSectionData[] mFrontPageSectionData;

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

    public void onQuerySuccess(@NotNull JSONObject object, String requestId) {
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
                buildMainPage(sections);
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
            buildMainPage(mFrontPageSectionData);
        } else {
            new QueryWikipedia(this, this, QueryWikipedia.REQUEST_FRONT_PAGE)
                    .actionParse()
                    .properties(Wikipedia.TEXT)
                    .pageMain()
                    .start();
        }
    }

    private void buildMainPage(FrontPageSectionData[] sections) {
        this.mFrontPageSectionData = sections;
        toggleProgressBar(false);

        int numColors = getResources().getIntArray(R.array.placeholder_colors).length;

        for (int i = 0; i < sections.length; i++) {
            FrontPageSectionData section = sections[i];

            ViewFactory.createLandingTopic(this.mContainerLayout, section.line);

            FrontPageSectionData.LayoutType layoutType = FrontPageSectionData.LayoutType.evaluate(section);
            switch (layoutType) {
                case LIST:
                    ViewFactory.createLandingList(this.mContainerLayout, section, this, i % numColors);
                    break;
                case SINGLE:
                    ViewFactory.createLandingBody(this.mContainerLayout, section, this, i % numColors);
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
    }

    public void onClick(String link) {
        WikiLinkUtils.defaultLinkBehavior(this, link);
    }
}