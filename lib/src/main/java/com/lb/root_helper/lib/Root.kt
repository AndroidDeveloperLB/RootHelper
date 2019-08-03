package com.lb.root_helper.lib

import android.os.*
import androidx.annotation.*
import eu.chainfire.libsuperuser.Shell
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.*

@Suppress("unused")
object Root {
    private var gotRoot: Boolean? = null
    private var rootSession: Shell.Interactive? = null

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
        if (gotRoot != null && gotRoot!! && rootSession!!.isRunning)
            return true
        val handler = Handler(Looper.getMainLooper())
        val countDownLatch = CountDownLatch(1)
        val gotRoot = AtomicBoolean()
        handler.post {
            getRootPrivilege(object : GotRootListener {
                override fun onGotRootResult(hasRoot: Boolean) {
                    gotRoot.set(hasRoot)
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
    fun hasRoot(): Boolean {
        return gotRoot != null && gotRoot!! && rootSession != null && rootSession!!.isRunning
    }

    /**
     * tries to gain root privilege. Will call the listener when it's done
     */
    @UiThread
    fun getRootPrivilege(listener: GotRootListener) {
        if (hasRoot()) {
            listener.onGotRootResult(true)
            return
        }
        val rootSessionRef = AtomicReference<Shell.Interactive>()
        try {
            rootSessionRef.set(Shell.Builder().useSU().setWantSTDERR(true).setWatchdogTimeout(5).setMinimalLogging(true).open { success, reason ->
                val success = reason == Shell.OnResult.SHELL_RUNNING
                if (success)
                    rootSession = rootSessionRef.get()
                gotRoot = success
                listener.onGotRootResult(success)
            })
        } catch (E: Exception) {
            listener.onGotRootResult(false)
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
        val countDownLatch = CountDownLatch(1)
        val resultRef = AtomicReference<List<String>>()
        rootSession!!.addCommand(commands, 0, object : Shell.OnCommandResultListener {
            override fun onCommandResult(commandCode: Int, exitCode: Int, output: MutableList<String>) {
                resultRef.set(output)
                if (exitCode == 0)
                    countDownLatch.countDown()
                else {
                    // failed to re-use root for future commands, so re-aquire it
                    gotRoot = null
                    getRootPrivilege(object : GotRootListener {
                        override fun onGotRootResult(hasRoot: Boolean) {
                            countDownLatch.countDown()
                        }
                    })
                }
            }
        })
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val result = resultRef.get()
        return result
    }
}
