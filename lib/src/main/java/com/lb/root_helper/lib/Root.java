package com.lb.root_helper.lib;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import eu.chainfire.libsuperuser.Shell;

public class Root {
    private static final Root _instance = new Root();
    private Boolean _hasRoot = null;
    private Shell.Interactive _rootSession;

    public interface IGotRootListener {
        void onGotRootResult(boolean hasRoot);
    }

    private Root() {
    }

    public static Root getInstance() {
        return _instance;
    }

    public boolean hasRoot() {
        return _hasRoot != null && _hasRoot && _rootSession != null && _rootSession.isRunning();
    }

    @WorkerThread
    public boolean getRoot() {
        if (_hasRoot != null && _hasRoot && _rootSession.isRunning())
            return true;
        final Handler handler = new Handler(Looper.getMainLooper());
        final WaitNotifier waitNotifier = new WaitNotifier();
        final AtomicBoolean gotRoot = new AtomicBoolean();
        handler.post(new Runnable() {
            @Override
            public void run() {
                getRoot(new IGotRootListener() {
                    @Override
                    public void onGotRootResult(final boolean hasRoot) {
                        gotRoot.set(hasRoot);
                        waitNotifier.doNotify();
                    }
                });
            }
        });
        waitNotifier.doWait();
        return gotRoot.get();
    }

    @UiThread
    public void getRoot(final IGotRootListener listener) {
        if (hasRoot()) {
            listener.onGotRootResult(true);
            return;
        }
        final AtomicReference<Shell.Interactive> rootSessionRef = new AtomicReference<>();
        rootSessionRef.set(new Shell.Builder().useSU().setWantSTDERR(true).setWatchdogTimeout(5).setMinimalLogging(true).open(//
                new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(final int commandCode, final int exitCode, final List<String> output) {
                        final boolean success = exitCode == Shell.OnCommandResultListener.SHELL_RUNNING;
                        if (success)
                            _rootSession = rootSessionRef.get();
                        _hasRoot = success;
                        listener.onGotRootResult(success);
                    }
                }));
    }

    @Nullable
    public List<String> runCommands(final List<String> commands) {
        return runCommands(commands.toArray(new String[commands.size()]));
    }

    @Nullable
    public List<String> runCommands(final String... commands) {
        if (commands == null || commands.length == 0 || !hasRoot())
            return null;
        final WaitNotifier waitNotifier = new WaitNotifier();
        _rootSession.addCommand(commands, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(final int commandCode, final int exitCode, final List<String> output) {
                waitNotifier.result = output;
                if (exitCode == 0)
                    waitNotifier.doNotify();
                else {
                    // failed to re-use root for future commands, so re-aquire it
                    _hasRoot = null;
                    getRoot(new IGotRootListener() {
                        @Override
                        public void onGotRootResult(final boolean hasRoot) {
                            waitNotifier.doNotify();
                        }
                    });
                }
            }
        });
        waitNotifier.doWait();
        @SuppressWarnings("unchecked")
        final List<String> result = (List<String>) waitNotifier.result;
        return result;
    }
}
