package com.peteschmitz.android.pocketwikipedia.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.adapter.HorizontalArticleAdapter;
import com.peteschmitz.android.pocketwikipedia.data.ArticleSummary;
import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleOverviewView;
import com.peteschmitz.android.pocketwikipedia.view.article.RandomArticleOverviewView;

import junit.framework.Assert;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lucasr.twowayview.TwoWayView;

/**
 *
 * Created by Pete on 3/27/14.
 */
public class ViewFactory {

    private ViewFactory(){
        throw new AssertionError("This class is reserved for static usage only.");
    }

    public static View createLandingTopicReload(@NotNull ViewGroup viewGroup, @Nullable String label){
        Assert.assertNotNull(viewGroup.getContext());

        View view = View.inflate(viewGroup.getContext(), R.layout.topic_layout_reload, null);
        TextView textView = (TextView) (view.findViewById(R.id.topic_layout_label));
        textView.setTypeface(FontManager.ALEO_LIGHT.getTypeface(viewGroup.getContext()));
        textView.setText(label);

        ImageView reloadIcon = (ImageView) view.findViewById(R.id.topic_layout_reload);

        viewGroup.addView(view);

        return reloadIcon;
    }

    public static View createLandingTopic(@NotNull ViewGroup viewGroup, @Nullable String label){
        Assert.assertNotNull(viewGroup.getContext());

        View view = View.inflate(viewGroup.getContext(), R.layout.topic_layout, null);
        TextView textView = (TextView) (view.findViewById(R.id.topic_layout_label));
        textView.setTypeface(FontManager.ALEO_LIGHT.getTypeface(viewGroup.getContext()));
        textView.setText(label);

        viewGroup.addView(view);

        return view;
    }

    public static TwoWayView createLandingList(@NotNull LinearLayout mContainerLayout,
                                               @NotNull FrontPageSectionData section,
                                               @Nullable WikiLinkUtils.LinkListener linkListener,
                                               int colorIndex) {

        Assert.assertNotNull(mContainerLayout.getContext());

        TwoWayView horizontalList = new TwoWayView(mContainerLayout.getContext());

        horizontalList.setLayoutParams(
                new TwoWayView.LayoutParams(
                        TwoWayView.LayoutParams.WRAP_CONTENT,
                        TwoWayView.LayoutParams.WRAP_CONTENT
                )
        );

        HorizontalArticleAdapter adapter = new HorizontalArticleAdapter(
                mContainerLayout.getContext(),
                section.items.toArray(new FrontPageSectionData.ListItem[section.items.size()]),
                linkListener,
                colorIndex
        );

        horizontalList.setAdapter(adapter);

        horizontalList.setOrientation(TwoWayView.Orientation.HORIZONTAL);
        horizontalList.setHorizontalScrollBarEnabled(false);

        mContainerLayout.addView(horizontalList);
        adapter.setContainer(horizontalList);

        return horizontalList;
    }

    public static ArticleOverviewView createLandingBody(@NotNull LinearLayout mContainerLayout,
                                                        @NotNull FrontPageSectionData section,
                                                        @Nullable WikiLinkUtils.LinkListener linkListener,
                                                        int colorIndex) {

        Assert.assertNotNull(mContainerLayout.getContext());

        ArticleOverviewView articleOverviewView = new ArticleOverviewView(mContainerLayout.getContext(), null, linkListener, false, 0.45f);
        articleOverviewView.setColorByIndex(colorIndex);
        articleOverviewView.setFrom(section);

        mContainerLayout.addView(articleOverviewView);

        return articleOverviewView;
    }

    public static RandomArticleOverviewView createLandingBodyRandom(@NotNull LinearLayout mContainerLayout,
                                                        @Nullable WikiLinkUtils.LinkListener linkListener,
                                                        int colorIndex,
                                                        @Nullable ArticleSummary articleSummary,
                                                        @Nullable RandomArticleOverviewView.Callback summaryCallback) {

        Assert.assertNotNull(mContainerLayout.getContext());

        RandomArticleOverviewView articleOverviewView =
                new RandomArticleOverviewView(
                        mContainerLayout.getContext(),
                        null,
                        linkListener,
                        false,
                        0.45f,
                        articleSummary,
                        summaryCallback
                );
        articleOverviewView.setColorByIndex(colorIndex);

        mContainerLayout.addView(articleOverviewView);

        return articleOverviewView;
    }
}
