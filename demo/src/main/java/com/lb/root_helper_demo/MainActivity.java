package com.lb.root_helper_demo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
                Toast.makeText(MainActivity.this,  "files count found on protected path:"+listSize, Toast.LENGTH_SHORT).show();
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
}
