package com.peteschmitz.android.pocketwikipedia.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Representational hierarchy of an {@link com.peteschmitz.android.pocketwikipedia.data.ArticleData} .
 * <p/>
 * Created by Pete Schmitz on 5/7/2014.
 */
public class SectionIndex implements Parcelable {

    public static final Creator<SectionIndex> CREATOR
            = new Creator<SectionIndex>() {

        public SectionIndex createFromParcel(Parcel in) {
            return new SectionIndex(in);
        }

        public SectionIndex[] newArray(int size) {
            return new SectionIndex[size];
        }
    };

    private int[] tiers = new int[ArticleData.ArticleDataLevel.values().length];

    public SectionIndex(Parcel in) {
        in.readIntArray(tiers);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(tiers);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public SectionIndex(@NotNull ArticleData.ArticleDataLevel currentLevel, @Nullable SectionIndex parentIndex, int index) {

        // Adjustments so level zero and one share the same index slot
        int levelIndex = currentLevel == ArticleData.ArticleDataLevel.ZERO ? 1 : currentLevel.ordinal();
        if (currentLevel == ArticleData.ArticleDataLevel.ONE) ++index;

        if (parentIndex != null) {
            int[] parentTiers = parentIndex.getTiers();

            System.arraycopy(parentTiers, 0, tiers, 0, parentTiers.length);
        }

        tiers[levelIndex] = index;

        if (levelIndex != -3) {

        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < tiers.length; i++) {
            if (tiers[i] == 0) break;

            if (i != 1) builder.append(".");

            builder.append(tiers[i]);
        }

        return builder.toString();
    }

    public int[] getTiers() {
        return tiers;
    }
}
