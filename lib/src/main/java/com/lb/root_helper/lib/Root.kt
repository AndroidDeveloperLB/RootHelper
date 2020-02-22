package com.lb.root_helper.lib

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.topjohnwu.superuser.Shell
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
object Root {
    private var rootSession: Shell? = null

    private class SuResult(private val succeeded: Boolean, private val resultCode: Int, private val output: MutableList<String>, private val error: MutableList<String>) : Shell.Result() {
        override fun getCode(): Int = resultCode
        override fun getOut(): MutableList<String> = output
        override fun isSuccess(): Boolean = succeeded
        override fun getErr(): MutableList<String> = error
    }


    interface GotRootListener {
        /**
         * called when we know if you got root or not
         *
         * @param hasRoot true iff you got root.
         */
        fun onGotRootResult(hasRoot: Boolean)
    }

    /**
     * tries to gain root privilege.
     *
     * @return true iff got root
     */
    @WorkerThread
    @Synchronized
    fun getRootPrivilege(): Boolean {
        if (rootSession?.isRoot == true)
            return true
        val handler = Handler(Looper.getMainLooper())
        val countDownLatch = CountDownLatch(1)
        val gotRoot = AtomicBoolean()
        handler.post {
            getRootPrivilege(object : GotRootListener {
                override fun onGotRootResult(hasRoot: Boolean) {
                    gotRoot.set(hasRoot)
                    if (hasRoot)
                        rootSession = Shell.getShell()
                    countDownLatch.countDown()
                }
            })
        }
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return gotRoot.get()
    }

    /**
     * @return true iff you currently have root privilege and can perform root operations using this class
     */
    @AnyThread
    fun hasRoot(): Boolean = rootSession?.isRoot == true

    /**
     * tries to gain root privilege. Will call the listener when it's done
     */
    @UiThread
    fun getRootPrivilege(listener: GotRootListener) {
        if (hasRoot()) {
            listener.onGotRootResult(true)
            return
        }
        Shell.getShell {
            val success = it.isRoot
            listener.onGotRootResult(success)
        }
    }

    /**
     * perform root operations.
     *
     * @return null if error or root not gained. Otherwise, a list of the strings that are the output of the commands
     */
    @WorkerThread
    @Synchronized
    fun runCommands(commands: List<String>?): List<String>? {
        return if (commands == null) null else runCommands(*commands.toTypedArray())
    }

    /**
     * perform root operations.
     *
     * @return null if error or root not gained. Otherwise, a list of the strings that are the output of the commands
     */
    @WorkerThread
    @Synchronized
    fun runCommands(vararg commands: String): List<String>? {
        if (commands.isEmpty() || !hasRoot())
            return null
        return Shell.su(*commands).exec().out
    }

    /**
     * perform root operations.
     *
     * @return null if error or root not gained. Otherwise, a list of the strings that are the output of the commands, including errors if exists
     */
    @WorkerThread
    @Synchronized
    fun runCommand(inputStream: InputStream, vararg commands: String): List<String>? {
        if (commands.isEmpty() || !hasRoot())
            return null
        val process: Process = Runtime.getRuntime().exec(commands)
        try {
            process.outputStream.use { outputStream -> inputStream.copyTo(outputStream) }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                process.destroyForcibly() else process.destroy()
            throw RuntimeException(e)
        }
        process.waitFor()
        val inputStr = process.inputStream.reader(Charset.defaultCharset()).readLines()
        val errStr = process.errorStream.reader(Charset.defaultCharset()).readLines()
        return ArrayList<String>(inputStr.size + errStr.size).apply {
            addAll(inputStr)
            addAll(errStr)
        }
    }

    /**
     * perform root operations.
     *
     * @return null if error or root not gained. Otherwise, a list of the strings that are the output of the commands, including detailed and specific result
     */
    @WorkerThread
    @Synchronized
    fun runCommandWithDetailedResult(inputStream: InputStream? = null, vararg commands: String): Shell.Result? {
        if (commands.isEmpty() || !hasRoot())
            return null
        if (inputStream == null)
            return Shell.su(*commands).exec()
        val process: Process = Runtime.getRuntime().exec(commands)
        try {
            process.outputStream.use { outputStream -> inputStream.copyTo(outputStream) }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                process.destroyForcibly() else process.destroy()
            throw RuntimeException(e)
        }
        process.waitFor()
        val inputStr = process.inputStream.reader(Charset.defaultCharset()).readLines().toMutableList()
        val errStr = process.errorStream.reader(Charset.defaultCharset()).readLines().toMutableList()
        val exitValue = process.exitValue()
        val isSucceeded = exitValue == 0
        return SuResult(isSucceeded, exitValue, inputStr, errStr)
    }

}
