package com.lb.root_helper_demo;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.lb.root_helper.lib.Root;
import com.lb.root_helper.lib.Root.IGotRootListener;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PROTECTED_PATH_TO_TEST = "/data/";
    private final Root mRoot = Root.getInstance();

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
        findViewById(R.id.rootButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                mRoot.getRoot(new IGotRootListener() {
                    @Override
                    public void onGotRootResult(final boolean hasRoot) {
                        if (!hasRoot) {
                            Toast.makeText(MainActivity.this, "root not acquired, so cannot perform test", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AsyncTask<Void, Void, Integer>() {
                            @Override
                            protected Integer doInBackground(final Void... voids) {
                                final List<String> result = mRoot.runCommands("ls " + PROTECTED_PATH_TO_TEST);
                                if (result == null)
                                    return 0;
                                Log.d("AppLog", "result of the command:");
                                for (String line : result)
                                    Log.d("AppLog", line);
                                return result.size();
                            }

                            @Override
                            protected void onPostExecute(final Integer filesCount) {
                                super.onPostExecute(filesCount);
                                Toast.makeText(MainActivity.this, "files count found on protected path:" + filesCount, Toast.LENGTH_SHORT).show();
                            }
                        }.execute();
                    }
                });
            }
        });
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
}
