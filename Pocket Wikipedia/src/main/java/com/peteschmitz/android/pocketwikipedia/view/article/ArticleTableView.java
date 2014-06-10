package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Pete on 5/26/2014.
 */
public class ArticleTableView extends LinearLayout {

    private Context mContext;
    private View mHeaderView;
    private Holder mHolder;
    private WikiLinkUtils.LinkListener mLinkListener;

    public ArticleTableView(@NotNull Context context, WikiLinkUtils.LinkListener linkListener) {
        this(context, null, linkListener);
    }

    public ArticleTableView(@NotNull Context context, @Nullable AttributeSet attrs, WikiLinkUtils.LinkListener linkListener) {
        super(context, attrs);

        this.mContext = context;
        mLinkListener = linkListener;

        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mHeaderView = inflater.inflate(R.layout.article_embed_view, this, true);

        this.mHolder = new Holder();
        this.mHolder.webView = (WebView) mHeaderView.findViewById(R.id.article_embed_container);
        mHolder.webView.setVerticalScrollbarOverlay(false);
        WebSettings settings = mHolder.webView.getSettings();
        settings.setUseWideViewPort(true);
    }


    public void setTable(ArticleData articleData){
        if (TextUtils.isEmpty(articleData.table)) return;

        mHolder.webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (mLinkListener != null){
                    mLinkListener.onClick(url);
                }
                return true;
            }
        });
        mHolder.webView.loadDataWithBaseURL(Wikipedia.getBaseURL(), articleData.table, "text/html", "utf-8", null);
    }

    private class Holder{
        WebView webView;
    }

}
