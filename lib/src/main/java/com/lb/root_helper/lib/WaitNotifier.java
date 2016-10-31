package com.lb.root_helper.lib;

/**
 * a class that allows to wait till a signal was fired . based on:
 * http://tutorials.jenkov.com/java-concurrency/thread-signaling.html <br/>
 * NOTE: it is highly advised to reset its state (or create a new instance) only if there are no other threads that
 * might use it
 */
public class WaitNotifier {
    private final Object monitoredObject = new Object();
    private boolean wasSignalled = false;
    public Object result;

    /**
     * waits till another thread has called doNotify (or if this thread was interrupted), or don't if was already
     * notified before
     *
     * @return true iff was interrupted instead of being notified
     */
    public boolean doWait() {
        boolean wasInterrupted = false;
        synchronized (monitoredObject) {
            while (!wasSignalled)
                try {
                    monitoredObject.wait();
                } catch (final InterruptedException e) {
                    wasInterrupted = true;
                    break;
                }
            wasSignalled = false;
            return wasInterrupted;
        }
    }

    public boolean doWait(final long milliseconds) {
        boolean wasInterrupted = false;
        synchronized (monitoredObject) {
            if (wasSignalled)
                return false;
            try {
                monitoredObject.wait(milliseconds);
            } catch (final InterruptedException e) {
                wasInterrupted = true;
            }
        }
        return wasInterrupted;
    }

    /**
     * notifies the waiting thread . will notify it even if it's not waiting yet
     */
    public void doNotify() {
        synchronized (monitoredObject) {
            wasSignalled = true;
            monitoredObject.notify();
        }
    }

    /**
     * returns true iff the notifier was notified
     */
    public boolean isNotified() {
        synchronized (monitoredObject) {
            return wasSignalled;
        }
    }

    /**
     * resets the notifier to be used again. <br/>
     * NOTE:it is highly advised to call it only if you are sure there is no thread that might use this object while
     * calling it
     */
    public void reset() {
        synchronized (monitoredObject) {
            wasSignalled = false;
        }
    }
}
