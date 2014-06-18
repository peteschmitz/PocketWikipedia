package com.peteschmitz.android.pocketwikipedia.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Pair;

import com.peteschmitz.android.pocketwikipedia.util.WikiTextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Pete Schmitz on 6/17/2014.
 */
public class ArticleSummary implements Parcelable {

    public static final Creator<ArticleSummary> CREATOR = new Creator<ArticleSummary>() {
        @Nullable
        @Override
        public ArticleSummary createFromParcel(Parcel source) {
            return new ArticleSummary(source);
        }

        @Override
        public ArticleSummary[] newArray(int size) {
            return new ArticleSummary[size];
        }
    };

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("<p>(.*?)</p>");
    private static final Pattern STRONG_PATTERN = Pattern.compile("<strong(.*?)>(.*?)</strong>");

    private Spanned mText = new SpannedString("");
    private String mImageUrl = "";
    private JSONArray mImages;

    public ArticleSummary(@NotNull JSONObject object){
        try {
            setText(object.getJSONObject("parse").getJSONObject("text").getString("*"));
            mImages = object.getJSONObject("parse").getJSONArray("images");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ArticleSummary(Parcel in){
        mText = new SpannedString(in.readString());
        mImageUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mText.toString());
        dest.writeString(mImageUrl);
    }

    @NotNull
    public Spanned getText(){
        return mText;
    }

    @NotNull
    public String getImageURL(){
        return mImageUrl;
    }

    private void setText(@NotNull String text){
        String splitText = "";

        List<Pair<String, Boolean>> tables = WikiTextUtils.removeTags("table", text);
        for (Pair<String, Boolean> split : tables){
            if (!split.second && !TextUtils.isEmpty(split.first.replace("<div>", ""))){
                Matcher matcher = PARAGRAPH_PATTERN.matcher(split.first);
                if (matcher.find()){
                    splitText = matcher.group(1).replaceAll("\\[(.*?)]", "");
                    break;
                } else {
                    splitText = split.first.replaceAll("\\[(.*?)]", "");
                }
            }
        }

        Matcher strongMatch = STRONG_PATTERN.matcher(splitText);
        if (strongMatch.find()){
            splitText = splitText.substring(0, strongMatch.start()) + splitText.substring(strongMatch.end(), splitText.length());
        }
        mText = Html.fromHtml(splitText);
    }

    public void setImageUrl(String imageUrl){
        mImageUrl = imageUrl;
    }

    @Nullable
    public JSONArray getImages(){
        return mImages;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
