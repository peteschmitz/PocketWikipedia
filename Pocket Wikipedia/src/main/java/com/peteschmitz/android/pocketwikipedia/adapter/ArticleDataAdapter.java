package com.peteschmitz.android.pocketwikipedia.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.GalleryActivity;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleDataView;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleDivView;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleTableView;
import com.peteschmitz.android.pocketwikipedia.view.article.ArticleTitleView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Merged adapter for displaying all major article data views: {@link com.peteschmitz.android.pocketwikipedia.view.article.ArticleDataView},
 * {@link com.peteschmitz.android.pocketwikipedia.view.article.ArticleDivView}, {@link com.peteschmitz.android.pocketwikipedia.view.article.ArticleTitleView},
 * and {@link com.peteschmitz.android.pocketwikipedia.view.article.ArticleTableView}.
 * <p/>
 * Created by Pete Schmitz on 4/18/2014.
 */
public class ArticleDataAdapter extends ArrayAdapter<ArticleData> {

    private static final int TITLE_TYPES = ArticleData.ArticleDataLevel.values().length - 1;
    private static final int TYPE_TEXT = TITLE_TYPES + 1;
    private static final int TYPE_EMBED = TITLE_TYPES + 2;
    private static final int TYPE_DIV = TITLE_TYPES + 3;

    private static final int TYPE_COUNT = ArticleData.ArticleDataLevel.values().length + 3;

    private List<ArticleData> mData = new LinkedList<ArticleData>();
    private String mCurrentArticle;
    private Map<ArticleData, ArticleTableView> mEmbedMap = new HashMap<ArticleData, ArticleTableView>();
    private WikiLinkUtils.LinkListener mLinkListener;
    private int mHighlightColor;
    private ImageEvaluation.Container mEvaluations;
    private GalleryActivity.GalleryPreviewListener mGalleryListener;

    public ArticleDataAdapter(Context context,
                              String currentArticle,
                              WikiLinkUtils.LinkListener linkListener,
                              @NotNull ImageEvaluation.Container evaluations,
                              GalleryActivity.GalleryPreviewListener galleryListener,
                              int colorIndex) {
        super(context, R.layout.article_data_text);

        mCurrentArticle = currentArticle;
        mLinkListener = linkListener;
        mHighlightColor = context.getResources().getIntArray(R.array.highlight_colors)[colorIndex];
        mEvaluations = evaluations;
        mGalleryListener = galleryListener;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        ArticleData data = getItem(position);

        if (!TextUtils.isEmpty(data.title)) {
            return data.level.ordinal();
        } else if (!TextUtils.isEmpty(data.table)) {
            return TYPE_EMBED;
        } else if (!TextUtils.isEmpty(data.div)) {
            return TYPE_DIV;
        }

        return TYPE_TEXT;
    }

    @Nullable
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        int type = getItemViewType(position);
        final ArticleData item = getItem(position);

        if (type <= TITLE_TYPES) {
            return getTitleView(item, convertView);
        }

        if (type == TYPE_TEXT) {
            return getTextView(item, convertView);
        }

        if (type == TYPE_EMBED) {
            return getEmbedView(item);
        }

        if (type == TYPE_DIV) {
            return getDivView(item, convertView);
        }


        return null;
    }

    @NotNull
    private View getTextView(ArticleData item, View convertView) {
        ArticleDataView view = (ArticleDataView) convertView;

        if (view == null) {
            view = new ArticleDataView(getContext());
        }

        view.setData(item);

        return view;
    }

    @NotNull
    private View getTitleView(ArticleData item, View convertView) {
        ArticleTitleView view = (ArticleTitleView) convertView;
        if (view == null) {
            view = new ArticleTitleView(getContext(), mCurrentArticle);
        }

        view.setData(item);

        return view;
    }

    @NotNull
    private ArticleTableView getEmbedView(ArticleData item) {

        if (mEmbedMap.containsKey(item)) {
            return mEmbedMap.get(item);
        }

        ArticleTableView embedView = new ArticleTableView(getContext(), mLinkListener);
        embedView.setTable(item);
        mEmbedMap.put(item, embedView);

        return embedView;
    }

    @NotNull
    private ArticleDivView getDivView(ArticleData item, View convertView) {
        ArticleDivView view = (ArticleDivView) convertView;

        if (view == null) {
            view = new ArticleDivView(getContext(), mHighlightColor, mEvaluations);
            view.setGalleryPreviewListener(mGalleryListener);
        }

        view.setDiv(item);

        return view;
    }

    @Override
    public void clear() {
        super.clear();
        mData.clear();
        mEmbedMap.clear();
    }

    @Override
    public void remove(ArticleData object) {
        super.remove(object);
        mData.remove(object);
        mEmbedMap.remove(object);
    }

    @Override
    public void add(ArticleData object) {
        super.add(object);
        mData.add(object);
        getEmbedView(object);
    }

    public ArticleData[] getItems() {
        return mData.toArray(new ArticleData[mData.size()]);
    }
}
