package com.peteschmitz.android.pocketwikipedia.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data container for a Wikipedia image.
 * <p/>
 * Created by Pete Schmitz on 5/22/2014.
 */
public class ImageEvaluation implements Parcelable {

    public interface Container {
        ArrayList<ImageEvaluation> getEvaluations();
    }

    public interface BuildListener {
        void onEvaluationBuilt(@NotNull ImageEvaluation evaluation);
    }

    public static final Creator<ImageEvaluation> CREATOR = new Creator<ImageEvaluation>() {
        @Nullable
        @Override
        public ImageEvaluation createFromParcel(Parcel source) {
            return new ImageEvaluation(source);
        }

        @Override
        public ImageEvaluation[] newArray(int size) {
            return new ImageEvaluation[size];
        }
    };

    private static final String TAG = "pwiki image evaluation";

    private static String THUMB_REGEX = "wikimedia\\.org/wikipedia/(.*?)/";
    private static Pattern THUMB_PATTERN = Pattern.compile(THUMB_REGEX);
    private static Pattern DIV_IMAGE_SOURCE_PATTERN = Pattern.compile("<img(.*?)src=\"(.*?)\"");
    private static Pattern DIV_IMAGE_FILE_WIDTH = Pattern.compile("data-file-width=\"(.*?)\"");
    private static Pattern DIV_IMAGE_FILE_HEIGHT = Pattern.compile("data-file-height=\"(.*?)\"");

    private String mOriginalName = "";
    private String mNormalizedName = "";
    private String mImageUrl = "";
    private String mThumbnailUrl = "";
    private SpannableString mDescription = new SpannableString("");
    private boolean mIsSvg = false;

    private int mWidth;
    private int mHeight;

    public ImageEvaluation(@NotNull String originalName) {
        mOriginalName = originalName;

        String[] fileSplits = originalName.split("\\.");
        if (fileSplits.length != 0) {
            mIsSvg = fileSplits[fileSplits.length - 1].toLowerCase().contains("svg");
        }
    }

    public ImageEvaluation(Parcel in) {
        mOriginalName = in.readString();
        mNormalizedName = in.readString();
        mImageUrl = in.readString();
        mThumbnailUrl = in.readString();
        mDescription = new SpannableString(in.readString());
        mWidth = in.readInt();
        mHeight = in.readInt();
        mIsSvg = in.readByte() != 0;
    }

    public boolean isSvg() {
        return mIsSvg;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mOriginalName);
        out.writeString(mNormalizedName);
        out.writeString(mImageUrl);
        out.writeString(mThumbnailUrl);
        out.writeString(mDescription.toString());
        out.writeInt(mWidth);
        out.writeInt(mHeight);
        out.writeByte((byte) (mIsSvg ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getNormalizedName() {
        if (TextUtils.isEmpty(mNormalizedName)) return getSetName();

        return mNormalizedName;
    }

    public String getOriginalName() {
        return mOriginalName;
    }

    public String getSetName() {
        return Wikipedia.FILE + mOriginalName;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    public void setNormalizedName(String normalizedName) {
        mNormalizedName = normalizedName;
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getThumbnailUrl() {
        if (TextUtils.isEmpty(mThumbnailUrl)) {
            return mImageUrl;
        }

        return mThumbnailUrl;
    }

    public boolean hasInfo() {
        return mWidth != 0 && mHeight != 0 && !TextUtils.isEmpty(mImageUrl);
    }

    public String getScaledImageUrl(@NotNull View view) {
        return getScaledImageUrl(view.getWidth());
    }

    public int getScaledWidth(int maxSize) {
        return maxSize >= mWidth ? mWidth : maxSize;
    }

    public String getScaledImageUrl(int width) {
        if (!hasInfo()) {
            throw new IllegalStateException("Image Evaluation doesn't have proper info set for scaled url call.");
        }

        if (width >= mWidth) {
            if (isSvg()) {
                return generateThumbnailUrl(mWidth);
            } else {
                return mImageUrl;
            }
        }

        return generateThumbnailUrl(width);
    }

    private String generateThumbnailUrl(int width) {
        String url = "";
        Matcher matcher = THUMB_PATTERN.matcher(mImageUrl);
        if (matcher.find()) {
            int end = matcher.end();

            String pre = mImageUrl.substring(0, end);
            String post = mImageUrl.substring(end, mImageUrl.length());

            String[] fileNameSplits = post.split("/");
            String fileName = fileNameSplits[fileNameSplits.length - 1];

            if (!TextUtils.isEmpty(fileName)) {
                url = pre + "thumb/" + post + "/" + width + "px-" + fileName;

                if (mIsSvg) url += ".png";
            }
        }

        if (TextUtils.isEmpty(url)) {
            Log.w(TAG, "failed to create scaled url for " + mOriginalName);
        }

        return url;
    }

    public void setThumbnail(float size) {
        if (!hasInfo()) {
            Log.w(TAG, "attempted to create thumbnail without proper info");
            return;
        }

        float imageWidth = mWidth;
        float imageHeight = mHeight;

        float widthToHeight = imageWidth / imageHeight;

        float scale;
        if (widthToHeight >= 1.0f) {
            scale = size / imageHeight;
        } else {
            scale = size / imageWidth;
        }

        if (scale > 1.0f) return;

        int thumbnailWidth = (int) (imageWidth * scale + 0.5f);
        int thumbnailHeight = (int) (imageHeight * scale + 0.5f);

        if (thumbnailWidth >= mWidth || thumbnailHeight >= mHeight) return;

        mThumbnailUrl = generateThumbnailUrl(thumbnailWidth);
    }

    public void setDescription(String description) {
        mDescription = new SpannableString(Html.fromHtml(description));
    }

    public SpannableString getDescription() {
        return mDescription;
    }

    public void copyInfo(ImageEvaluation other) {
        if (TextUtils.isEmpty(mDescription) && !TextUtils.isEmpty(other.getDescription())) {
            mDescription = other.getDescription();
        }

        if (TextUtils.isEmpty(mNormalizedName)) {
            mNormalizedName = other.getNormalizedName();
        }

        if (mWidth == 0) {
            mWidth = other.getWidth();
        }

        if (mHeight == 0) {
            mHeight = other.getHeight();
        }
    }

    @Nullable
    public static ImageEvaluation createFromDiv(@NotNull String div) {
        Matcher matcher = DIV_IMAGE_SOURCE_PATTERN.matcher(div);
        String src;
        if (matcher.find()) {
            src = matcher.group(2);
        } else {
            Log.w(TAG, "couldn't find src during image evaluation from div");
            return null;
        }

        matcher = DIV_IMAGE_FILE_WIDTH.matcher(div);
        int width;
        if (matcher.find()) {

            width = Integer.parseInt(matcher.group(1));
        } else {
            Log.w(TAG, "couldn't find width during image evaluation from div");
            return null;
        }

        matcher = DIV_IMAGE_FILE_HEIGHT.matcher(div);
        int height;
        if (matcher.find()) {
            height = Integer.parseInt(matcher.group(1));
        } else {
            Log.w(TAG, "couldn't find height during image evaluation from div");
            return null;
        }

        String[] originalNameSplits = src.split("/");
        String originalName = null;
        if (originalNameSplits.length >= 2) {
            originalName = Uri.decode(originalNameSplits[originalNameSplits.length - 2]);
        }
        if (originalName == null) {
            Log.w(TAG, "couldn't find original name during image evaluation from div");
            return null;
        }

        String[] fullUrlSplits = src.split("(?=/)");
        String fullUrl = "";
        for (int i = 0; i < fullUrlSplits.length - 1; i++) {
            if (fullUrlSplits[i].equals("/thumb")) continue;

            fullUrl += fullUrlSplits[i];
        }
        if (TextUtils.isEmpty(fullUrl)) {
            Log.w(TAG, "couldn't find full url");
            return null;
        }

        ImageEvaluation image = new ImageEvaluation(originalName);
        image.setImageUrl(fullUrl);
        image.setSize(width, height);

        // remove magnify
        matcher = Pattern.compile("<div class=\"magnify\">(.*?)</div>").matcher(div);
        if (matcher.find()) {
            div = div.substring(0, matcher.start()) + div.substring(matcher.end(), div.length());
        }

        // Find description
        String[] descriptionSplit = div.split("<div class=\"thumbcaption\">");
        if (descriptionSplit.length >= 2) {
            image.setDescription(descriptionSplit[descriptionSplit.length - 1].replaceAll("</div>|\n", ""));
        }

        return image;
    }
}
