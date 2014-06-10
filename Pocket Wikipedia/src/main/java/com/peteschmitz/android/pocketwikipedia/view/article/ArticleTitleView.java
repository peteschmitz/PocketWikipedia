package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Pete on 5/28/2014.
 */
public class ArticleTitleView extends LinearLayout implements View.OnClickListener {

    private Context mContext;
    private Holder mHolder;
    private View mView;
    private ArticleData mActiveData;
    private String mCurrentArticle;

    public ArticleTitleView(@NotNull Context context, String currentArticle){
        super(context, null);

        mContext = context;
        mCurrentArticle = currentArticle;
    }

    public void setData(@NotNull ArticleData data){
        mActiveData = data;

        if (mView == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = inflater.inflate(data.level.getHeaderLayoutResource(), this, true);

            mHolder = new Holder();
            mHolder.index = (TextView) mView.findViewById(R.id.article_data_index_text);
            mHolder.index.setTypeface(FontManager.ALEO_LIGHT.getTypeface(mContext));

            mHolder.title = (TextView) mView.findViewById(R.id.article_data_title_text);
            mHolder.title.setTypeface(FontManager.ALEO_LIGHT.getTypeface(mContext));

            mHolder.shareButton = (ImageButton) mView.findViewById(R.id.article_data_share_button);
            if (mHolder.shareButton != null){
                mHolder.shareButton.setOnClickListener(this);
            }
        }

        mHolder.title.setText(data.title);
        mHolder.index.setText(data.index.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.article_data_share_button:
                if (mActiveData != null){
                    WikiLinkUtils.defaultShareBehavior(mContext, getFullURL());
                }
                break;
        }
    }

    public String getFullURL(){
        String articleURL = WikiLinkUtils.getArticleURL(mCurrentArticle);

        if (mActiveData != null && !TextUtils.isEmpty(mActiveData.getURL())){
            articleURL += "#" + mActiveData.getURL();
        }

        return articleURL;
    }

    private class Holder{
        TextView index;
        TextView title;
        ImageButton shareButton;
    }
}
