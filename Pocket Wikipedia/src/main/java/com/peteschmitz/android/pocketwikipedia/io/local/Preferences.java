package com.peteschmitz.android.pocketwikipedia.io.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Pete Schmitz on 5/19/2014.
 */
public class Preferences {

    private static final String SHARED_PREFERENCE_NAME = "com.peteschmitz.android.pocketwikipedia";
    private static final String SHOW_ARTICLE = "showArticle";

    private static SharedPreferences sPreferences;

    private static void set(Context context){
        if (sPreferences == null){
            sPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
    }

    public static boolean getShowArticleDrawer(Context context){
        set(context);

        return sPreferences.getBoolean(SHOW_ARTICLE, true);
    }

    public static void setShowArticleDrawer(Context context, boolean show){
        set(context);

        sPreferences.edit().putBoolean(SHOW_ARTICLE, show).commit();
    }
}
