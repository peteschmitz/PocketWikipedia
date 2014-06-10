package com.peteschmitz.android.pocketwikipedia.io.network;

import android.content.Context;
import android.os.AsyncTask;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.data.ImageEvaluation;
import com.peteschmitz.android.pocketwikipedia.util.ImageEvaluations;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Asynctask for retrieving information for Wikipedia images.
 * <p/>
 * Created by Pete Schmitz on 5/22/2014.
 */
public class WikiImageUrlTask extends AsyncTask<Void, Void, ArrayList<ImageEvaluation>> {

    private final ArrayList<ImageEvaluation> mImages = new ArrayList<ImageEvaluation>();
    private final Context mContext;
    private int mThumbnailSize;

    public WikiImageUrlTask(@NotNull Context context, @NotNull List<String> images) {
        for (String image : images) {
            mImages.add(new ImageEvaluation(image));
        }

        mThumbnailSize = context.getResources().getDimensionPixelSize(R.dimen.thumbnail_size);
        mContext = context;
    }

    @Override
    protected ArrayList<ImageEvaluation> doInBackground(Void... params) {
        new QueryWikipedia(mContext, new QueryWikipedia.Callback() {
            @Override
            public void onQuerySuccess(@NotNull JSONObject object, String requestId) {

                try {
                    JSONObject query = object.getJSONObject("query");

                    if (query.has("normalized")) {
                        JSONArray normalizedNames = query.getJSONArray("normalized");
                        for (int i = 0; i < normalizedNames.length(); i++) {
                            JSONObject normal = normalizedNames.getJSONObject(i);
                            ImageEvaluation image = ImageEvaluations.withSetName(mImages, normal.getString("from"));

                            if (image != null) {
                                image.setNormalizedName(normal.getString("to"));
                            }
                        }
                    }

                    JSONObject pages = query.getJSONObject("pages");

                    for (Iterator<String> pageIterator = pages.keys(); pageIterator.hasNext(); ) {
                        JSONObject page = pages.getJSONObject(pageIterator.next());

                        JSONObject imageInfo = page.getJSONArray("imageinfo").getJSONObject(0);

                        String url = imageInfo.getString("url");
                        int width = imageInfo.getInt("width");
                        int height = imageInfo.getInt("height");

                        ImageEvaluation image = ImageEvaluations.withNormalizedName(mImages, page.getString("title"));

                        if (image != null) {
                            image.setImageUrl(url);
                            image.setSize(width, height);
                            image.setThumbnail(mThumbnailSize);
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onQueryFailure(Exception exception, String requestId) {

            }
        })
                .actionQuery()
                .titles(ImageEvaluations.setNames(mImages))
                .properties(Wikipedia.IMAGE_INFO)
                .imageProperties(Wikipedia.URL, Wikipedia.SIZE)
                .run();

        return mImages;
    }
}
