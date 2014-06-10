package com.peteschmitz.android.pocketwikipedia.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.peteschmitz.android.pocketwikipedia.R;
import com.squareup.picasso.Picasso;

/**
 * Created by Pete on 5/23/2014.
 */
public class ImagePreview extends LinearLayout {

    private Context mContext;
    private Holder mHolder;

    public ImagePreview(Context context) {
        this(context, null);
    }

    public ImagePreview(Context context, AttributeSet attrs){
        super(context, attrs);

        mContext = context;

        init();
    }

    private void init(){
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.article_image_preview, this, true);

        if (view == null){
            throw new IllegalStateException("Inflated image preview is null");
        }

        mHolder = new Holder();
        mHolder.imagePreview = (ImageView) view.findViewById(R.id.article_image_preview);
    }

    public ImageView getImageView(){
        return mHolder.imagePreview;
    }

    public void setImageUrl(String url){
        Picasso.with(mContext)
                .cancelRequest(mHolder.imagePreview);

        if (TextUtils.isEmpty(url)) return;

        Picasso.with(mContext)
                .load(url)
                .fit()
                .centerCrop()
                .into(mHolder.imagePreview);
    }

    private class Holder{
        ImageView imagePreview;
    }
}
