package com.peteschmitz.android.pocketwikipedia.util;

import android.net.Uri;

import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Pete Schmitz on 5/22/2014.
 */
public class ImageEvaluations {

    private ImageEvaluations(){
        throw new IllegalStateException("Class is reserved for static usage only");
    }

    @Nullable
    public static ImageEvaluation withSetName(@NotNull List<ImageEvaluation> list, @NotNull String setName){
        for (ImageEvaluation image : list){
            if (image.getSetName().equals(setName)) return image;
        }

        return null;
    }

    @Nullable
    public static ImageEvaluation withNormalizedName(@NotNull List<ImageEvaluation> list, @NotNull String normalizedName){
        for (ImageEvaluation image : list){
            if (image.getNormalizedName().equals(normalizedName)) return image;
        }

        return null;
    }

    @Nullable
    public static ImageEvaluation withImageUrl(@NotNull List<ImageEvaluation> list, @NotNull String imageUrl){
        for (ImageEvaluation image : list){
            if (image.getImageUrl().equals(imageUrl)) return image;
        }

        return null;
    }

    @Nullable
    public static ImageEvaluation withOriginalName(@NotNull List<ImageEvaluation> list, @NotNull String originalName){
        for (ImageEvaluation image : list){
            if (image.getOriginalName().equals(originalName)) return image;
        }

        return null;
    }

    @NotNull
    public static List<String> setNames(@NotNull List<ImageEvaluation> list){
        List<String> setNames = new LinkedList<String>();

        for (ImageEvaluation image : list){
            setNames.add(Uri.encode(image.getSetName()));
        }

        return setNames;
    }
}
