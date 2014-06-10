package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.GalleryActivity;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ArticleHeaderView extends LinearLayout {

    private Context mContext;
    private Holder mHolder;
    private View mHeaderView;
    private String mBackgroundUrl;
    private String mIconUrl;
    private GalleryActivity.GalleryPreviewListener mPreviewListener;

    public ArticleHeaderView(@NotNull Context context) {
        this(context, null, 0);
    }

    public ArticleHeaderView(@NotNull Context context, int color) {
        this(context, null, color);
    }

    public ArticleHeaderView(@NotNull Context context, @Nullable AttributeSet attrs, int color) {
        super(context, attrs);

        this.mContext = context;

        init();

        setHeaderBackgroundColor(color);
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mHeaderView = inflater.inflate(R.layout.article_header, this, true);

        this.mHolder = new Holder();
        this.mHolder.backgroundImage = ((ImageView) this.mHeaderView.findViewById(R.id.article_header_image));
        this.mHolder.iconImage = ((ImageView) this.mHeaderView.findViewById(R.id.article_header_icon));
        this.mHolder.labelText = ((TextView) this.mHeaderView.findViewById(R.id.article_header_label));
        this.mHolder.labelText.setTypeface(FontManager.ALEO_LIGHT.getTypeface(this.mContext));
        this.mHolder.gradient = ((LinearLayout) this.mHeaderView.findViewById(R.id.article_header_gradient));
    }

    public void setHeaderBackgroundColor(int color) {

        this.mHolder.backgroundImage.setBackgroundColor(color);
    }

    public void setLabel(String label) {
        assert (this.mHolder != null);

        this.mHolder.labelText.setText(label);
        this.mHolder.labelText.setVisibility(View.VISIBLE);
    }

    public void setIconURL(String url) {
        mIconUrl = url;
        setIconOnClick();

        Picasso.with(this.mContext).load(url).fit().centerCrop().into(this.mHolder.iconImage, new Callback() {
            @Override
            public void onSuccess() {
                mHolder.iconImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError() {

            }
        });
    }

    public void setBackgroundURL(String url) {
        mBackgroundUrl = url;
        setBackgroundOnClick();

        Picasso.with(this.mContext)
                .load(url)
                .fit()
                .centerCrop()
                .into(this.mHolder.backgroundImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        setHeaderBackgroundColor(0);
                        mHolder.gradient.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    public void setGalleryPreviewListener(GalleryActivity.GalleryPreviewListener previewListener){
        mPreviewListener = previewListener;

        setBackgroundOnClick();
    }

    private void setBackgroundOnClick(){
        if (!TextUtils.isEmpty(mBackgroundUrl)){
            mHolder.backgroundImage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPreviewListener.onGalleryPreviewClick(mBackgroundUrl);
                }
            });
        }
    }

    private void setIconOnClick(){
        if (!TextUtils.isEmpty(mIconUrl)){
            mHolder.iconImage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPreviewListener.onGalleryPreviewClick(mIconUrl);
                }
            });
        }
    }

    private class Holder {
        ImageView backgroundImage;
        ImageView iconImage;
        TextView labelText;
        LinearLayout gradient;
    }
}