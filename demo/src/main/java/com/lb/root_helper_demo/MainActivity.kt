package com.lb.root_helper_demo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import com.lb.root_helper.lib.Root
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        normalApiButton.setOnClickListener {
            val list = File(PROTECTED_PATH_TO_TEST).list()
            val listSize = list?.size ?: 0
            Toast.makeText(this@MainActivity, "files count found on protected path:$listSize", Toast.LENGTH_SHORT).show()
        }
        initLoader(false)
        rootButton!!.setOnClickListener { initLoader(true) }
    }

    private fun initLoader(forceStart: Boolean) {
        val loaderManager = LoaderManager.getInstance(this)
        val previousLoader = loaderManager.getLoader<Any>(LOADER_ID) as RootLoader?
        if (previousLoader != null && forceStart)
            loaderManager.destroyLoader(LOADER_ID)
        rootButton!!.isEnabled = true
        progressBar!!.visibility = View.GONE
        if (forceStart || previousLoader != null) {
            rootButton!!.isEnabled = false
            progressBar!!.visibility = View.VISIBLE
            loaderManager.initLoader(LOADER_ID, null, object : LoaderCallbacks<Int> {
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Int> {
                    return RootLoader(this@MainActivity)
                }

                override fun onLoadFinished(loader: Loader<Int>, result: Int?) {
                    loaderManager.destroyLoader(LOADER_ID)
                    rootButton!!.isEnabled = true
                    progressBar!!.visibility = View.GONE
                    if (result == null)
                        Toast.makeText(this@MainActivity, "root not acquired, so cannot perform test", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this@MainActivity, "files count found on protected path:$result", Toast.LENGTH_SHORT).show()
                }

                override fun onLoaderReset(loader: Loader<Int>) {}
            }).forceLoad()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url = "https://github.com/AndroidDeveloperLB/RootHelper"
        }
        if (url == null)
            return true
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(intent)
        return true
    }

    private class RootLoader(context: Context) : AsyncTaskLoader<Int>(context) {

        override fun loadInBackground(): Int? {
            val root = Root.instance
            val gotRoot = root.root
            if (!gotRoot)
                return null
            val result = root.runCommands("ls $PROTECTED_PATH_TO_TEST") ?: return 0
//Log.d("AppLog", "result of the command:");
            //for (String line : result)
            //    Log.d("AppLog", line);
            return result.size
        }
    }

    companion object {
        private val PROTECTED_PATH_TO_TEST = "/data/"
        val LOADER_ID = 1
    }


}
