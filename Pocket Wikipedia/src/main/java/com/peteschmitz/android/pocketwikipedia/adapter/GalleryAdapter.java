package com.peteschmitz.android.pocketwikipedia.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.peteschmitz.android.pocketwikipedia.view.ImagePreview;

import org.jetbrains.annotations.Nullable;

/**
 * Simple adapter for gallery preview views.
 * <p/>
 * Created by Pete Schmitz on 5/23/2014.
 */
public class GalleryAdapter extends ArrayAdapter<ImageEvaluation> {

    private Context mContext;

    public GalleryAdapter(Context context) {
        super(context, 0);

        mContext = context;
    }

    @Nullable
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImagePreview view = (ImagePreview) convertView;

        if (view == null) {
            view = new ImagePreview(mContext);
        }

        ImageEvaluation image = getItem(position);
        view.setImageUrl(image.getThumbnailUrl());

        return view;
    }
}
