package com.peteschmitz.android.pocketwikipedia.view.article;

import android.app.Service;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.GalleryActivity;
import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.peteschmitz.android.pocketwikipedia.io.network.WikiImageUrlTask;
import com.peteschmitz.android.pocketwikipedia.util.ImageEvaluations;
import com.peteschmitz.android.pocketwikipedia.view.FlowView;
import com.peteschmitz.android.pocketwikipedia.view.ImagePreview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Pete on 5/21/2014.
 */
public class ArticleImagePreviewView extends LinearLayout implements GalleryActivity.GalleryPreviewListener, ImageEvaluation.Container {

    private static final int MAX_IMAGES = 8;

    private Context mContext;
    private List<ImagePreview> mImageViews = new ArrayList<ImagePreview>();
    private LinearLayout mView;
    private FlowView mFlowView;
    private LayoutInflater mInflater;
    private LinearLayout mExtraIcon;
    private int mBackgroundColor;
    private List<String> mImages;
    private List<AsyncTask> mActiveTasks = new LinkedList<AsyncTask>();
    private int mExtraIconIndex;
    private Map<String, Integer> mPreviews = new HashMap<String, Integer>();
    private String mClickQueue;
    private boolean mPreviewsSet = false;
    private ArrayList<ImageEvaluation> mImageEvaluations = new ArrayList<ImageEvaluation>();

    public ArticleImagePreviewView(@NotNull Context context){
        this(context, null);
    }

    public ArticleImagePreviewView(@NotNull Context context, int backgroundColor){
        this(context, null, backgroundColor);
    }

    public ArticleImagePreviewView(@NotNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public ArticleImagePreviewView(@NotNull Context context, @Nullable AttributeSet attrs, int backgroundColor){
        super(context, attrs);

        mContext = context;
        mBackgroundColor = backgroundColor;

        init();
    }

    private void init(){
        mInflater = (LayoutInflater) mContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        mView = (LinearLayout) mInflater.inflate(R.layout.article_image_header, this, true);
        mFlowView = (FlowView) mView.findViewById(R.id.article_image_header_flow);
    }

    public void setImages(List<String> images){
        mImages = images;

        new WikiImageUrlTask(mContext, mImages){
            @Override
            protected void onPostExecute(ArrayList<ImageEvaluation> imageEvaluations) {
                super.onPostExecute(imageEvaluations);

                setPreviews(imageEvaluations);
            }
        }
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void add(ImageEvaluation evaluation){
        ImageEvaluation dupe = ImageEvaluations.withOriginalName(mImageEvaluations, evaluation.getOriginalName());
        if (dupe != null){
            dupe.copyInfo(evaluation);
            return;
        }

        mImageEvaluations.add(evaluation);
    }

    public void setPreviews(ArrayList<ImageEvaluation> imageEvaluations){
        for (ImageEvaluation evaluation : imageEvaluations){
            add(evaluation);
        }
        int previewIndex = 0;


        for (int i = 0; i < imageEvaluations.size(); i++){
            ImageEvaluation image = imageEvaluations.get(i);

            if (TextUtils.isEmpty(image.getThumbnailUrl()) || mPreviews.containsKey(image.getThumbnailUrl())){
                continue;
            }


            mPreviews.put(image.getThumbnailUrl(), i);
            setImageUrl(image, previewIndex);
            ++ previewIndex;

            if (previewIndex == MAX_IMAGES - 1 && i + 2 < imageEvaluations.size()){
                mExtraIconIndex = i + 1;
                setExtraIcon(imageEvaluations.size() - ( i + 1));
                break;
            }
        }

        mPreviewsSet = true;
        if (!TextUtils.isEmpty(mClickQueue)){
            onGalleryPreviewClick(mClickQueue);
        }
    }


    public List<String> getImages(){
        return mImages;
    }

    @Override
    public ArrayList<ImageEvaluation> getEvaluations(){
        return mImageEvaluations;
    }

    private void setImageUrl(final ImageEvaluation image, int index){
        expandImageViews(index);

        ImagePreview imagePreview = mImageViews.get(index);
        ImageView imageView = imagePreview.getImageView();
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onGalleryPreviewClick(image.getImageUrl());
            }
        });

        imagePreview.setImageUrl(image.getThumbnailUrl());
    }

    private void expandImageViews(int to){
        while (mImageViews.size() <= to){

            ImagePreview imagePreview = new ImagePreview(mContext);
            imagePreview.setLayoutParams(new FlowView.LayoutParams(FlowView.LayoutParams.WRAP_CONTENT, FlowView.LayoutParams.WRAP_CONTENT));
            mFlowView.addView(imagePreview);
            mImageViews.add(imagePreview);
        }
    }

    private void setExtraIcon(int amount){
        if (mExtraIcon == null){
            LinearLayout layout = (LinearLayout) mInflater.inflate(R.layout.article_image_extra_icon, null, true);
            layout.setLayoutParams(new FlowView.LayoutParams(FlowView.LayoutParams.WRAP_CONTENT, FlowView.LayoutParams.WRAP_CONTENT));
            mFlowView.addView(layout);

            mExtraIcon = (LinearLayout) layout.findViewById(R.id.article_image_extra_icon);
            mExtraIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onGalleryPreviewClick(mExtraIconIndex);
                }
            });
        }

        TextView label = (TextView) mExtraIcon.findViewById(R.id.article_image_extra_icon_label);
        label.setTypeface(FontManager.ALEO.getTypeface(mContext));
        label.setText("+" + amount);

        mExtraIcon.setBackgroundColor(mBackgroundColor);
    }

    public void dispose(){
        for (AsyncTask task : mActiveTasks){
            task.cancel(true);
        }
    }


    @Override
    public void onGalleryPreviewClick(String imageUrl) {
        if (!mPreviewsSet){
            mClickQueue = imageUrl;
            return;
        }

        ImageEvaluation originalImage = ImageEvaluations.withImageUrl(mImageEvaluations, imageUrl);

        onGalleryPreviewClick(mImageEvaluations.indexOf(originalImage));
    }

    @Override
    public void onGalleryPreviewClick(int index) {
        GalleryActivity.launch(mContext, getEvaluations(), index);
    }
}
