package com.peteschmitz.android.pocketwikipedia.view.article;

import android.app.Service;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.data.ArticleData;
import com.peteschmitz.android.pocketwikipedia.io.local.FontManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Pete on 4/25/2014.
 */
public class ArticleDataView extends LinearLayout {

    private View mDataView;
    private Context mContext;
    private Holder mHolder;

    public ArticleDataView(@NotNull Context context){
        this(context, null);
    }

    public ArticleDataView(@NotNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
    }

    public void setData(ArticleData data){

        if (mDataView == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
            mDataView = inflater.inflate(R.layout.article_data_text, this, true);

            mHolder = new Holder();

            mHolder.text = (TextView) mDataView.findViewById(R.id.article_data_text);
            mHolder.text.setTypeface(FontManager.ALEO.getTypeface(mContext));
            mHolder.text.setMovementMethod(LinkMovementMethod.getInstance());
        }

        mHolder.text.setText(data.span);
    }

    private class Holder{
        private TextView text;
    }
}
