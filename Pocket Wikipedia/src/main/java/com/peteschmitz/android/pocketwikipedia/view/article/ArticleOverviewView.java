package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.peteschmitz.android.pocketwikipedia.io.network.WikiArticleImageTask;
import com.peteschmitz.android.pocketwikipedia.util.WikiDisplayUtil;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.util.WikiTextUtils;
import com.peteschmitz.android.pocketwikipedia.view.listener.VerticalSpaceListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lucasr.twowayview.TwoWayView;

/**
 * Created by Pete on 5/12/2014.
 */
public class ArticleOverviewView extends LinearLayout implements Callback {

    private static final float DEFAULT_RESTRICTED_WIDTH_PERCENTAGE = 0.75f;

    protected WikiLinkUtils.LinkListener mLinkListener;
    private VerticalSpaceListener mSpaceListener;
    private WikiArticleImageTask.FullQueryListener mFullQueryListener;
    protected Holder mHolder;
    private Context mContext;
    private View mHeaderView;
    private int mLinkColor;
    private String mTargetArticle;
    private int mPlaceholderColor;

    public ArticleOverviewView(@NotNull Context context,
                               @Nullable AttributeSet attrs,
                               @Nullable WikiLinkUtils.LinkListener linkListener,
                               boolean restrictWidth,
                               float heightPercentage) {

        this(context, attrs, linkListener, restrictWidth, heightPercentage, null);
    }

    public ArticleOverviewView(@NotNull Context context,
                               @Nullable AttributeSet attrs,
                               @Nullable WikiLinkUtils.LinkListener linkListener,
                               boolean restrictWidth,
                               float heightPercentage,
                               @Nullable VerticalSpaceListener spaceListener) {

        super(context, attrs);

        mLinkListener = linkListener;
        mSpaceListener = spaceListener;
        mContext = context;
        mFullQueryListener = getFullQueryListener();

        init();
        if (restrictWidth) setWidthPercentage(DEFAULT_RESTRICTED_WIDTH_PERCENTAGE);
        setImageHeightPercentage(heightPercentage);
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mHeaderView = inflater.inflate(R.layout.article_overview_view, this, true);

        mHolder = new Holder();

        mHolder.imagePlaceholder = (LinearLayout) mHeaderView.findViewById(R.id.article_overview_image_placeholder);
        mHolder.imageContainer = (ImageView) mHeaderView.findViewById(R.id.article_overview_image_container);
        mHolder.textView = (TextView) mHeaderView.findViewById(R.id.article_overview_text);
        mHolder.textView.setTypeface(FontManager.ALEO.getTypeface(mContext));
        mHolder.textView.setMovementMethod(LinkMovementMethod.getInstance());
        mHolder.layout = (RelativeLayout) mHeaderView.findViewById(R.id.article_overview_layout);
    }

    public void setImageHeightPercentage(float percentage){
        assert mHolder.imagePlaceholder.getLayoutParams() != null;
        assert mHolder.imageContainer.getLayoutParams() != null;

        DisplayMetrics metrics = WikiDisplayUtil.getDisplayMetrics(mContext);

        int targetHeight = (int) (metrics.heightPixels * percentage);
        int targetWidth = mHolder.imagePlaceholder.getLayoutParams().width;
        mHolder.imagePlaceholder.setLayoutParams(new RelativeLayout.LayoutParams(targetWidth, targetHeight));

        targetWidth = mHolder.imageContainer.getLayoutParams().width;
        mHolder.imageContainer.setLayoutParams(new LinearLayout.LayoutParams(targetWidth, targetHeight));
    }

    public void setWidthPercentage(float percentage){

        DisplayMetrics metrics = WikiDisplayUtil.getDisplayMetrics(mContext);

        int targetWidth = (int) (percentage * metrics.widthPixels);
        mHeaderView.setLayoutParams(new TwoWayView.LayoutParams(targetWidth, TwoWayView.LayoutParams.WRAP_CONTENT));
    }

    public void setColorByIndex(int colorByIndex) {
        mLinkColor = mContext.getResources().getIntArray(R.array.text_colors)[colorByIndex];
        mPlaceholderColor = mContext.getResources().getIntArray(R.array.placeholder_colors)[colorByIndex];

        setPlaceholderColor(mPlaceholderColor);
    }

    public void setPlaceholderColor(int color){

        mHolder.imagePlaceholder.setBackgroundColor(color);
    }

    protected void resetPlaceholderAndImage(){
        setPlaceholderColor(mPlaceholderColor);
        Picasso.with(mContext)
                .cancelRequest(mHolder.imageContainer);
        mHolder.imageContainer.setImageDrawable(null);
    }

    public void setFrom(FrontPageSectionData.ListItem item, final int position) {
        setText(item.spanned);

        setImageFromArticle(item.getSuggestedArticle());

        if (mSpaceListener != null){
            mHolder.textView.post(new Runnable() {
                @Override
                public void run() {
                    mSpaceListener.evaluateSpace(position, mHolder.textView.getMeasuredHeight());
                }
            });

        }
    }

    public void setText(final Spanned text){
        mHolder.textView.setText(text);
        WikiTextUtils.applySpannableLinkStyle(mHolder.textView, mLinkColor, mLinkListener);
    }

    public void setImageFromArticle(final String encodedArticle){
        resetPlaceholderAndImage();

        mTargetArticle = encodedArticle;
        if (!TextUtils.isEmpty(encodedArticle)){

            WikiArticleImageTask imageTask = WikiArticleImageTask.getActiveFullQuery(encodedArticle);

            // Grab URL from cache
            if (WikiArticleImageTask.ARTICLE_CACHE.containsKey(encodedArticle)){
                String url = WikiArticleImageTask.ARTICLE_CACHE.get(encodedArticle);

                if (!TextUtils.isEmpty(url)){
                    setBackgroundURL(url);
                }

            // Check if there's already a query retrieving the request article; listen for the result
            } else if (imageTask != null){
                imageTask.addFullQueryListener(mFullQueryListener);

            // Start a new query for the requested article
            } else {
                new WikiArticleImageTask(encodedArticle, mFullQueryListener)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }


        }
    }

    private WikiArticleImageTask.FullQueryListener getFullQueryListener(){
        return new WikiArticleImageTask.FullQueryListener() {
            @Override
            public void onSuccess(String encodedArticleName, String url) {
                if (TextUtils.isEmpty(url) || !encodedArticleName.equals(mTargetArticle)) return;

                setBackgroundURL(url);
            }

            @Override
            public void onFailure(String encodedArticleName) {

            }
        };
    }

    public void setBackgroundURL(String url){

        Picasso.with(mContext)
                .load(url)
                .fit()
                .centerCrop()
                .into(mHolder.imageContainer, this);
    }

    public void setFrom(FrontPageSectionData section) {

        mHolder.textView.setText(section.spanned);
        WikiTextUtils.applySpannableLinkStyle(mHolder.textView, mLinkColor, mLinkListener);

        if (!section.images.isEmpty()){
            setBackgroundURL(section.images.get(0));
        } else if (!TextUtils.isEmpty(section.getSuggestedArticle())){
            setImageFromArticle(section.getSuggestedArticle());
        }
    }

    @Override
    public void onSuccess() {
        setPlaceholderColor(0);
    }

    @Override
    public void onError() {

    }

    protected class Holder{
        LinearLayout imagePlaceholder;
        ImageView imageContainer;
        TextView textView;
        RelativeLayout layout;
    }
}
