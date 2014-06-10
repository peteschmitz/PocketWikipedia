package com.peteschmitz.android.pocketwikipedia.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.view.DrawerTitleView;

import org.jetbrains.annotations.Nullable;

/**
 * Simple adapter for displaying article data titles.
 * <p/>
 * Created by Pete Schmitz on 4/18/2014.
 */
public class ArticleDrawerAdapter
        extends ArrayAdapter<ArticleData> {

    public ArticleDrawerAdapter(Context context) {
        super(context, R.layout.article_drawer_view_one);
    }

    public int getViewTypeCount() {
        return ArticleData.ArticleDataLevel.values().length;
    }

    public int getItemViewType(int position) {
        return getItem(position).level.ordinal();
    }

    @Nullable
    public View getView(int position, View convertView, ViewGroup parent) {
        ArticleData item = getItem(position);
        DrawerTitleView view = (DrawerTitleView) convertView;
        if (view == null) {
            view = new DrawerTitleView(getContext());
        }
        view.setData(item);

        return view;
    }
}
