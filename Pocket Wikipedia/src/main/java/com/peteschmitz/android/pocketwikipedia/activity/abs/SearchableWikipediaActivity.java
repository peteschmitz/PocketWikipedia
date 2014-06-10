package com.peteschmitz.android.pocketwikipedia.activity.abs;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.widget.SearchView;
import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.activity.ArticleActivity;
import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.language.LanguageKey;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Pete Schmitz on 3/10/14.
 */
public abstract class SearchableWikipediaActivity extends PocketWikiFragmentActivity {

    private static final String TAG = "pwiki search";

    private AsyncTask<Void, Void, Void> mActiveQuery = null;
    private String[] mActiveSuggestions = null;
    private boolean mSubmitQueryOnSuggestionLoad = false;
    private String mQueuedQuery = null;

    protected SearchView mSearchView;
    private SearchManager mSearchManager;
    private SimpleCursorAdapter mSearchAdapter;

    public void attachWikipediaSearch() {
        Wikipedia.setLanguage(
                LanguageKey.getLanguageKeyFromId(
                        getBaseContext().getString(R.string.language_id)
                ).getLanguageInstance()
        );

        mSearchView = new SearchView(getSupportActionBar().getThemedContext());
        mSearchView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        mSearchManager = (SearchManager) getSystemService(SEARCH_SERVICE);

        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnSuggestionListener(
                new SearchView.OnSuggestionListener() {
                    @Override
                    public boolean onSuggestionSelect(int i) {
                        return false;
                    }

                    @Override
                    public boolean onSuggestionClick(int i) {
                        CursorAdapter adapter = mSearchView.getSuggestionsAdapter();
                        Cursor cursor = adapter.getCursor();

                        mSearchView.setQuery(cursor.getString(1), true);
                        mSearchView.clearFocus();
                        return false;
                    }
                }
        );

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                onSearchTextChange(newText);

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                broadcastQuery(query);
                return true;
            }
        };
        mSearchView.setOnQueryTextListener(queryTextListener);
        getSupportActionBar().setCustomView(mSearchView);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        mSearchView.setQueryHint(getResources().getString(R.string.search_wikipedia));
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
    }

    private void broadcastQuery(String query) {
        onSearchQuery(query);
    }

    protected void onSearchQuery(String query) {
        this.mSearchView.clearFocus();
        if (suggestionsContains(query)) {
            ArticleActivity.launch(this, Uri.encode(getContainingSuggestion(query).replace(" ", "_")));
        } else if (suggestionsInProgress()) {
            submitQueryOnSuggestionLoad(query);
        } else {
            Toast.makeText(
                    this,
                    getResources().getString(R.string.no_article_exists_for) + " '" + query + "'",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    protected void onSearchTextChange(String newText) {
        startWikiArticleSearch(newText);
    }

    protected void startWikiArticleSearch(final String query) {

        if (TextUtils.isEmpty(query)) {
            setAdapterSuggestions(new String[]{});
            return;
        }

        if (mActiveQuery != null) {
            mActiveQuery.cancel(true);
        }

        mActiveQuery = new AsyncTask<Void, Void, Void>() {

            private String[] mResults;

            @Override
            protected Void doInBackground(Void... params) {

                this.mResults = searchArticles(query);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                mActiveSuggestions = this.mResults;
                setAdapterSuggestions(this.mResults);

                mActiveQuery = null;
                if (mSubmitQueryOnSuggestionLoad && !TextUtils.isEmpty(mQueuedQuery)) {
                    mSubmitQueryOnSuggestionLoad = false;
                    broadcastQuery(mQueuedQuery);
                }
            }
        }
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected void setHint(CharSequence hint) {
        mSearchView.setQueryHint(hint);
    }

    private String[] searchArticles(String query) {
        String[] results = null;

        try {
            JSONArray array = new JSONArray(getWikiSearch(query));

            if (array.length() > 1) {
                JSONArray suggestions = (JSONArray) array.get(1);
                results = new String[suggestions.length()];
                for (int i = 0; i < suggestions.length(); i++) {
                    results[i] = suggestions.get(i).toString();
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        return results;
    }

    private String getWikiSearch(String query) {
        query = Uri.encode(query);

        StringBuilder stringBuilder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();

        HttpGet get = new HttpGet(Wikipedia.getOpensearchURL(query));

        try {
            HttpResponse response = client.execute(get);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {

                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

            } else {
                Log.e(TAG, "Http status invalid, code: " + statusCode);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return stringBuilder.toString();
    }

    private void setAdapterSuggestions(String[] suggestions) {
        if (suggestions == null) return;

        checkSearchState();
        MatrixCursor searchCursor = new MatrixCursor(new String[]{"_id", "text"});

        if (mSearchAdapter == null) {

            mSearchAdapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    searchCursor,
                    new String[]{"text"},
                    new int[]{android.R.id.text1},
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
            );
            mSearchView.setSuggestionsAdapter(mSearchAdapter);
        }


        for (int i = 0; i < suggestions.length; i++) {
            searchCursor.addRow(new String[]{
                    Integer.toString(i),
                    suggestions[i]
            });
        }

        mSearchAdapter.changeCursor(searchCursor);
        mSearchAdapter.notifyDataSetChanged();
    }

    private void checkSearchState() {
        if (mSearchManager == null || mSearchView == null) {
            throw new IllegalStateException("Search failed; search hasn't been attached.");
        }
    }

    protected boolean suggestionsContains(String query) {
        if (mActiveSuggestions == null || mActiveSuggestions.length == 0) return false;

        for (String suggestion : mActiveSuggestions) {
            if (query.equalsIgnoreCase(suggestion)) return true;
        }

        return false;
    }

    /**
     * Returns the properly cased suggestion of the provided query; throws an exception
     * if suggestions do not contain the query. Use {@link #suggestionsContains(String)} first.
     */
    @NotNull
    protected String getContainingSuggestion(String query) {
        assert mActiveSuggestions != null;

        for (String suggestion : mActiveSuggestions) {
            if (suggestion.equalsIgnoreCase(query)) {
                return suggestion;
            }
        }

        throw new IllegalArgumentException("Query isn't contained in suggestions");
    }

    protected boolean suggestionsInProgress() {
        return mActiveQuery != null;
    }

    protected void submitQueryOnSuggestionLoad(String query) {
        mSubmitQueryOnSuggestionLoad = true;
        mQueuedQuery = query;
    }
}
