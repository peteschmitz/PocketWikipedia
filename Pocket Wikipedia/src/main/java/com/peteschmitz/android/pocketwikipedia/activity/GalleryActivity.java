package com.peteschmitz.android.pocketwikipedia.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.adapter.GalleryAdapter;
import com.peteschmitz.android.pocketwikipedia.constant.BundleKey;
import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.lucasr.twowayview.TwoWayView;

import java.util.ArrayList;

/**
 * Activity for displaying a gallery of images that relate to a Wikipedia article.
 * <p/>
 * Created by Pete Schmitz on 5/21/2014.
 */
public class GalleryActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String TAG = "pwiki gallery";

    /**
     * in DP
     */
    private static final int IMAGE_PADDING = 20;

    public interface GalleryPreviewListener {
        void onGalleryPreviewClick(String imageUrl);

        void onGalleryPreviewClick(int index);
    }

    public static void launch(@NotNull Context context, ArrayList<ImageEvaluation> images, int startingIndex) {
        Intent intent = new Intent(context, GalleryActivity.class);

        intent.putExtra(BundleKey.IMAGES, images.toArray(new Parcelable[images.size()]));

        if (startingIndex >= 0 && startingIndex < images.size()) {
            intent.putExtra(BundleKey.GALLERY_INDEX, startingIndex);
        }

        context.startActivity(intent);
    }

    private ImageView mImageView;
    private RelativeLayout mImageContainer;
    private ProgressBar mProgressBar;
    private ImageEvaluation mActiveImage;
    private int mMaxImageHeight;
    private int mMaxImageWidth;
    private TwoWayView mListView;
    private GalleryAdapter mAdapter;
    private ArrayList<ImageEvaluation> mImages = new ArrayList<ImageEvaluation>();
    private int mActiveImageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideStatusBar();
        setContentView(R.layout.gallery);
        findIds();
        buildAdapter();
        loadFromBundle(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Picasso.with(this).cancelRequest(mImageView);
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(BundleKey.GALLERY_INDEX, mActiveImageIndex);
        outState.putParcelableArray(BundleKey.IMAGES, mImages.toArray(new Parcelable[mImages.size()]));
    }

    private void buildAdapter() {
        mAdapter = new GalleryAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    private void findIds() {
        mImageView = (ImageView) findViewById(R.id.gallery_image);
        mProgressBar = (ProgressBar) findViewById(R.id.gallery_progress_bar);

        mImageContainer = (RelativeLayout) findViewById(R.id.gallery_image_container);

        mListView = (TwoWayView) findViewById(R.id.gallery_list_view);
        mListView.setHorizontalScrollBarEnabled(false);
    }

    private void findDimensions() {
        int padding = (int) (getResources().getDisplayMetrics().density * IMAGE_PADDING + 0.5f);
        mMaxImageHeight = mImageContainer.getHeight() - padding;
        mMaxImageWidth = mImageContainer.getWidth() - padding;
    }

    private void loadFromBundle(Bundle bundle) {

        mActiveImageIndex = -1;
        if (bundle != null) {
            mActiveImageIndex = bundle.getInt(BundleKey.GALLERY_INDEX, -1);
        }

        if (mActiveImageIndex == -1) {
            mActiveImageIndex = getIntent().getIntExtra(BundleKey.GALLERY_INDEX, 0);
        }

        Parcelable[] evaluations = getIntent().getParcelableArrayExtra(BundleKey.IMAGES);
        if (evaluations == null && bundle != null) {
            evaluations = bundle.getParcelableArray(BundleKey.IMAGES);
        }

        if (evaluations == null || evaluations.length == 0) {
            throw new IllegalStateException("Gallery received an empty or null image bundle");
        }

        mImages = new ArrayList<ImageEvaluation>();
        for (Parcelable parcelable : evaluations) {
            mImages.add((ImageEvaluation) parcelable);
        }

        mAdapter.addAll(mImages);
        mImageContainer.post(new Runnable() {
            @Override
            public void run() {
                findDimensions();
                setImage(mActiveImageIndex, true);
                mListView.setSelection(mActiveImageIndex);
            }
        });

    }

    private void setImage(int index, boolean forceSet) {
        if (!forceSet && index == mActiveImageIndex) return;

        if (index >= mImages.size()) {
            Log.w(TAG, "Out of bounds gallery index (is network available?)");
            return;
        }

        mActiveImageIndex = index;
        mActiveImage = mImages.get(index);

        if (!mActiveImage.hasInfo()) {

            Log.w(TAG, "attempted to set a gallery image without proper info");
            return;
        }

        resizeImageContainer(mActiveImage);

        toggleProgress(true);
        mImageView.post(new Runnable() {
            @Override
            public void run() {
                Picasso.with(GalleryActivity.this)
                        .load(mActiveImage.getScaledImageUrl(mImageView))
                        .fit()
                        .into(mImageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                toggleProgress(false);
                            }

                            @Override
                            public void onError() {
                            }
                        });
            }
        });

    }

    private void toggleProgress(boolean showProgress) {
        mImageView.setVisibility(!showProgress ? View.VISIBLE : View.INVISIBLE);
        mProgressBar.setVisibility(showProgress ? View.VISIBLE : View.INVISIBLE);
    }

    private void resizeImageContainer(ImageEvaluation imageEvaluation) {
        float maxWidth = (float) (imageEvaluation.getWidth()) / (float) (mMaxImageWidth);
        float maxHeight = (float) (imageEvaluation.getHeight()) / (float) (mMaxImageHeight);

        if (maxWidth <= 1.0f && maxHeight <= 1.0f) {
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(
                            imageEvaluation.getWidth(),
                            imageEvaluation.getHeight()
                    );
            params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
            mImageView.setLayoutParams(params);
        } else {
            float scale;
            if (maxWidth > maxHeight) {
                scale = (float) (mMaxImageWidth) / (float) (imageEvaluation.getWidth());
            } else {
                scale = (float) (mMaxImageHeight) / (float) (imageEvaluation.getHeight());
            }

            int scaledWidth = (int) ((float) (imageEvaluation.getWidth()) * scale + 0.5f);
            int scaledHeight = (int) ((float) (imageEvaluation.getHeight()) * scale + 0.5f);

            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(
                            scaledWidth,
                            scaledHeight
                    );
            params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
            mImageView.setLayoutParams(params);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setImage(position, false);
    }

    private void hideStatusBar() {
        // Hide status bar on 4.1+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }
}
