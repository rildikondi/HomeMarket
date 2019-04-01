package com.akondi.homemarket.tasks;

import android.content.Context;

public final class IdleTimer extends Thread {

    private IdleTimeListener listener;
    private final int idleTimeoutSeconds;
    private static final String TAG = "IdleTimer";

    public IdleTimer(int idleTimeout, IdleTimeListener listener) {
        this.idleTimeoutSeconds = idleTimeout;
        this.listener = listener;
    }

    @Override
    public void run() {
        for (int second = idleTimeoutSeconds; second >= 0; second--) {

            if (second == 0 ) {
                listener.onTimePassed();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }
}
