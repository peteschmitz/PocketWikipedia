package com.peteschmitz.android.pocketwikipedia.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleOverviewView;
import com.peteschmitz.android.pocketwikipedia.view.listener.VerticalSpaceListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lucasr.twowayview.TwoWayView;

/**
 * Simple adapter for displaying {@link com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData.ListItem}s.
 * Only compatible with a {@link org.lucasr.twowayview.TwoWayView}. Automatically resizes height to match largest view.
 * <p/>
 * Created by Pete Schmitz on 3/26/14.
 */
public class HorizontalArticleAdapter extends ArrayAdapter<FrontPageSectionData.ListItem> implements VerticalSpaceListener {

    private Context mContext;
    private int mColorIndex;
    private int mFirstHeight = 0;
    private int mAdjustedHeight = 0;
    private TwoWayView mContainer;
    private WikiLinkUtils.LinkListener mLinkListener;

    public HorizontalArticleAdapter(@NotNull Context context,
                                    @NotNull FrontPageSectionData.ListItem[] items,
                                    @Nullable WikiLinkUtils.LinkListener linkListener,
                                    int colorIndex) {
        super(context, android.R.layout.simple_list_item_1, items);

        mColorIndex = colorIndex;
        mLinkListener = linkListener;
        mContext = context;
    }

    public void setContainer(TwoWayView twoWayView) {
        mContainer = twoWayView;
    }

    @Nullable
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ArticleOverviewView articleOverviewView = (ArticleOverviewView) convertView;
        if (articleOverviewView == null) {
            articleOverviewView = new ArticleOverviewView(mContext, null, mLinkListener, true, 0.35f, this);
            articleOverviewView.setColorByIndex(mColorIndex);
        }
        articleOverviewView.setFrom(getItem(position), position);

        return articleOverviewView;
    }

    @Override
    public void evaluateSpace(int index, int totalHeight) {

        if (mFirstHeight == 0) {
            mFirstHeight = totalHeight;
        } else {
            int differenceFromFirst = totalHeight - mFirstHeight;

            if (differenceFromFirst > 0 && differenceFromFirst > mAdjustedHeight) {
                int neededAdjustment = differenceFromFirst - mAdjustedHeight;

                if (mContainer != null) {

                    mAdjustedHeight = differenceFromFirst;
                    mContainer.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    mContainer.getHeight() + neededAdjustment
                            )
                    );
                }
            }
        }
    }


}
