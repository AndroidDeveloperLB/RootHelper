package com.lb.root_helper_demo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.lb.root_helper.lib.Root;

import java.io.File;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

public class MainActivity extends AppCompatActivity {
    private static final String PROTECTED_PATH_TO_TEST = "/data/";
    public static final int LOADER_ID = 1;
    private final Root mRoot = Root.getInstance();
    private View mProgressBar;
    private View mRootButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.normalApiButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                final String[] list = new File(PROTECTED_PATH_TO_TEST).list();
                int listSize = list == null ? 0 : list.length;
                Toast.makeText(MainActivity.this, "files count found on protected path:" + listSize, Toast.LENGTH_SHORT).show();
            }
        });
        mProgressBar = findViewById(R.id.progressBar);
        mRootButton = findViewById(R.id.rootButton);
        initLoader(false);
        mRootButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                initLoader(true);
            }
        });
    }

    private void initLoader(boolean forceStart) {
        final LoaderManager loaderManager = getSupportLoaderManager();
        final RootLoader previousLoader = (RootLoader) (Loader<?>) loaderManager.getLoader(LOADER_ID);
        if (previousLoader != null && forceStart)
            loaderManager.destroyLoader(LOADER_ID);
        mRootButton.setEnabled(true);
        mProgressBar.setVisibility(View.GONE);
        if (forceStart || previousLoader != null) {
            mRootButton.setEnabled(false);
            mProgressBar.setVisibility(View.VISIBLE);
            loaderManager.initLoader(LOADER_ID, null, new LoaderCallbacks<Integer>() {
                @Override
                public Loader<Integer> onCreateLoader(final int id, final Bundle args) {
                    return new RootLoader(MainActivity.this);
                }

                @Override
                public void onLoadFinished(final Loader<Integer> loader, final Integer result) {
                    loaderManager.destroyLoader(LOADER_ID);
                    mRootButton.setEnabled(true);
                    mProgressBar.setVisibility(View.GONE);
                    if (result == null)
                        Toast.makeText(MainActivity.this, "root not acquired, so cannot perform test", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this, "files count found on protected path:" + result, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLoaderReset(final Loader<Integer> loader) {
                }
            }).forceLoad();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        String url = null;
        switch (item.getItemId()) {
            case R.id.menuItem_all_my_apps:
                url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB";
                break;
            case R.id.menuItem_all_my_repositories:
                url = "https://github.com/AndroidDeveloperLB";
                break;
            case R.id.menuItem_current_repository_website:
                url = "https://github.com/AndroidDeveloperLB/RootHelper";
                break;
        }
        if (url == null)
            return true;
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
        return true;
    }

    private static class RootLoader extends AsyncTaskLoader<Integer> {

        public RootLoader(final Context context) {
            super(context);
        }

        @Override
        public java.lang.Integer loadInBackground() {
            final Root root = Root.getInstance();
            final boolean gotRoot = root.getRoot();
            if (!gotRoot)
                return null;
            final List<String> result = root.runCommands("ls " + PROTECTED_PATH_TO_TEST);
            if (result == null)
                return 0;
            //Log.d("AppLog", "result of the command:");
            //for (String line : result)
            //    Log.d("AppLog", line);
            return result.size();
        }
    }


}
