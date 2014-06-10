package com.peteschmitz.android.pocketwikipedia.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Pete Schmitz on 5/13/2014.
 */
public class WikiDisplayUtil {

    @NotNull
    public static DisplayMetrics getDisplayMetrics(@NotNull Context context){
        DisplayMetrics metrics = new DisplayMetrics();

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        return metrics;
    }
}
