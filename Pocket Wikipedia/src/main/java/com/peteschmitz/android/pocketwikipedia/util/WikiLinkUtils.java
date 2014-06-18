package com.peteschmitz.android.pocketwikipedia.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.peteschmitz.android.pocketwikipedia.activity.ArticleActivity;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;

/**
 * Created by Pete Schmitz on 5/5/2014.
 */
public class WikiLinkUtils {

    public static String getArticleURL(String article) {
        return Wikipedia.getBaseArticleURL() + Uri.encode(article.replace(" ", "_"));
    }

    public interface LinkListener{
        void onClick(String link);
    }

    public static void defaultLinkBehavior(Activity activity, String url){
        if (url.contains("/wiki/")){
//            if (url.contains("User:") || url.contains("Wikipedia:")){
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Wikipedia.getBaseURL() + url));
//                activity.startActivity(intent);
//                return;
//            }

            ArticleActivity.launch(activity, Uri.decode(url.split("/wiki/")[1]));
        } else if (url.contains("//")){
            if (!url.contains("http://") && !url.contains("https://")){
                url = url.replace("//", "http://");
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
        }
    }

    public static void defaultShareBehavior(Context context, String url){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(intent, "Choose a sharing option..."));
    }

    private WikiLinkUtils(){
        throw new AssertionError("Class is reserved for static usage only.");
    }
}
