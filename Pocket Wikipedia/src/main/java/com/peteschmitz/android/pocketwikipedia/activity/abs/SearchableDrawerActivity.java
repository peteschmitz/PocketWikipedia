package com.peteschmitz.android.pocketwikipedia.activity.abs;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.view.MenuItem;
import com.peteschmitz.android.pocketwikipedia.R;

/**
 * Created by Pete Schmitz on 4/28/2014.
 */
public abstract class SearchableDrawerActivity<T> extends SearchableWikipediaActivity {

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private void findIds() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
    }

    protected abstract ArrayAdapter<T> getDrawerAdapter();

    protected ListView getDrawerListView() {
        findIds();

        return mDrawerList;
    }

    protected void initDrawer() {
        findIds();

        mDrawerList.setAdapter(getDrawerAdapter());

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onDrawerItemClick(parent, view, position, id);
            }
        });

        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.open_navigation,
                R.string.close_navigation
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {

            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
            } else {
                mDrawerLayout.openDrawer(mDrawerList);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onDrawerItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    protected void closeDrawer() {
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    protected void openDrawer() {
        mDrawerLayout.openDrawer(mDrawerList);
    }
}
