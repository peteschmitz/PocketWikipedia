package com.peteschmitz.android.pocketwikipedia.io.local;

import android.content.Context;
import android.graphics.Typeface;

import org.jetbrains.annotations.NotNull;

public enum FontManager {
    ALEO("fonts/aleo.otf"),
    ALEO_LIGHT("fonts/aleo-light.otf");

    private Typeface mTypeface;
    private String mPath;

    private FontManager(String path) {
        this.mPath = path;
    }

    @NotNull
    public Typeface getTypeface(@NotNull Context context) {
        if (this.mTypeface == null) {
            this.mTypeface = Typeface.createFromAsset(context.getAssets(), this.mPath);
        }
        return this.mTypeface;
    }
}