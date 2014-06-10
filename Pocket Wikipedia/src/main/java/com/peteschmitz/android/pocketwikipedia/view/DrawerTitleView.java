package com.peteschmitz.android.pocketwikipedia.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DrawerTitleView
        extends LinearLayout {

    private View mDataView;
    private Context mContext;
    private Holder mHolder;

    public DrawerTitleView(@NotNull Context context) {
        this(context, null);
    }

    public DrawerTitleView(@NotNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.mContext = context;
    }

    public void setData(ArticleData data) {
        if (this.mDataView == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mDataView = inflater.inflate(data.level.getDrawerLayoutResource(), this, true);

            this.mHolder = new Holder();
            this.mHolder.title = ((TextView) this.mDataView.findViewById(R.id.article_drawer_title_text));
            this.mHolder.title.setTypeface(FontManager.ALEO_LIGHT.getTypeface(this.mContext));

            this.mHolder.index = ((TextView) this.mDataView.findViewById(R.id.article_drawer_index_text));
            this.mHolder.index.setTypeface(FontManager.ALEO_LIGHT.getTypeface(this.mContext));
        }
        if (TextUtils.isEmpty(data.title)) {
            this.mHolder.title.setVisibility(View.GONE);
        } else {
            this.mHolder.title.setText(data.title);
            this.mHolder.index.setText(data.index.toString());
            this.mHolder.title.setVisibility(View.VISIBLE);
        }
    }

    private class Holder {
        private TextView title;
        private TextView index;
    }
}