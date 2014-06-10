package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.GalleryActivity;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.peteschmitz.android.pocketwikipedia.util.ImageEvaluations;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Pete on 5/30/2014.
 */
public class ArticleDivView extends LinearLayout {

    private static final float MAX_WIDTH_PERCENTAGE = 0.45f;
    private static final int IMAGE_MARGIN_TOP_DP = 20;

    private static int MAX_SIZE;
    private static int IMAGE_MARGIN_TOP_PX;
    private Context mContext;
    private View mView;
    private Holder mHolder;
    private int mHighlightColor;
    private ImageEvaluation.Container mEvaluations;
    private GalleryActivity.GalleryPreviewListener mPreviewListener;
    private ImageEvaluation mActiveImage;

    public ArticleDivView(@NotNull Context context, int color, @NotNull ImageEvaluation.Container evaluations){
        super(context);

        mContext = context;
        mHighlightColor = color;
        mEvaluations = evaluations;

        if (IMAGE_MARGIN_TOP_PX == 0){
            float scale = mContext.getResources().getDisplayMetrics().density;
            IMAGE_MARGIN_TOP_PX = (int) ((float) (IMAGE_MARGIN_TOP_DP) * scale + 0.5f);
        }
    }

    public void setDiv(ArticleData data){
        if (TextUtils.isEmpty(data.div)) return;

        if (mView == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = inflater.inflate(R.layout.article_div_view, this, true);

            mHolder = new Holder();
            mHolder.background = (LinearLayout) mView.findViewById(R.id.article_div_background);
            mHolder.background.setBackgroundColor(mHighlightColor);
            if (MAX_SIZE == 0){
                mHolder.background.post(new Runnable() {
                    @Override
                    public void run() {
                        MAX_SIZE = (int) ((float) (mHolder.background.getWidth()) * MAX_WIDTH_PERCENTAGE);
                    }
                });
            }

            mHolder.image = (ImageView) mView.findViewById(R.id.article_div_image);

            mHolder.text = (TextView) mView.findViewById(R.id.article_div_text);
            mHolder.text.setTypeface(FontManager.ALEO_LIGHT.getTypeface(mContext));
            mHolder.text.setMovementMethod(LinkMovementMethod.getInstance());

            setImageOnClick();
        }

        final ImageEvaluation image = ImageEvaluations.withOriginalName(mEvaluations.getEvaluations(), data.div);

        if (image == null) return;

        mHolder.text.setVisibility(TextUtils.isEmpty(image.getDescription()) ? View.GONE : View.VISIBLE);
        mHolder.text.setText(image.getDescription());

        mHolder.background.post(new Runnable() {
            @Override
            public void run() {
                setImage(image);
            }
        });
    }

    private void setImage(@NotNull ImageEvaluation image){
        mActiveImage = image;

        int width = image.getScaledWidth(MAX_SIZE);
        float percentage = (float) (width) / (float) (image.getWidth());
        int height = (int) ((float) (image.getHeight()) * percentage);

        LayoutParams params = new LayoutParams(width, height);
        params.setMargins(0, IMAGE_MARGIN_TOP_PX, 0, IMAGE_MARGIN_TOP_PX);
        mHolder.image.setLayoutParams(params);
        mHolder.image.setImageDrawable(null);

        Picasso.with(mContext)
                .cancelRequest(mHolder.image);

        Picasso.with(mContext)
                .load(image.getScaledImageUrl(MAX_SIZE))
        .into(mHolder.image);
    }

    public void setGalleryPreviewListener(GalleryActivity.GalleryPreviewListener previewListener){
        mPreviewListener = previewListener;

        setImageOnClick();
    }

    private void setImageOnClick() {
        if (mPreviewListener == null || mHolder == null) return;

        mHolder.image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreviewListener.onGalleryPreviewClick(getActiveUrl());
            }
        });
    }

    private String getActiveUrl(){
        return mActiveImage == null ? "" : mActiveImage.getImageUrl();
    }

    private class Holder{
        LinearLayout background;
        ImageView image;
        TextView text;
    }
}
