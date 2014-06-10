package com.peteschmitz.android.pocketwikipedia.view.article;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Pete on 6/4/2014.
 */
public class ArticleDrawerHeader extends LinearLayout {

    private Context mContext;
    private Holder mHolder;

    public ArticleDrawerHeader(@NotNull Context context, int backgroundColor){
        super(context, null);

        mContext = context;

        init(backgroundColor);
    }

    private void init(int backgroundColor){
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.article_drawer_header, this, true);

        mHolder = new Holder();
        mHolder.image = (ImageView) view.findViewById(R.id.article_drawer_header_image);
        mHolder.text = (TextView) view.findViewById(R.id.article_drawer_header_text);
        mHolder.text.setTypeface(FontManager.ALEO.getTypeface(mContext));

        setBackgroundColor(backgroundColor);
    }

    public void setLabel(String label){
        mHolder.text.setText(label);
    }

    public void setImage(String url){
        if (TextUtils.isEmpty(url)) return;

        Picasso.with(mContext)
                .load(url)
                .fit()
                .centerCrop()
                .into(mHolder.image, new Callback() {
                    @Override
                    public void onSuccess() {
                        setBackgroundColor(0);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    public void setBackgroundColor(int color){
        mHolder.image.setBackgroundColor(color);
    }

    private class Holder{
        ImageView image;
        TextView text;
    }
}
