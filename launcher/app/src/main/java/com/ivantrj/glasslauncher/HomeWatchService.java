package com.ivantrj.glasslauncher;

import android.app.AppOpsManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

/** On this API 25 Fire OS build, use foreground usage events instead of Accessibility. */
public class HomeWatchService extends Service {
    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private static final long LOOKBACK_MS = 10_000;
    private static final long POLL_MS = 350;
    private static final String TAG = "GlassHomeWatch";

    private HandlerThread thread;
    private Handler handler;
    private UsageStatsManager usageStats;
    private long lastRedirectedEvent;

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!hasUsageStatsAccess()) {
                stopSelf();
                return;
            }
            Foreground foreground = latestForeground();
            if (foreground != null && AMAZON_LAUNCHER.equals(foreground.packageName)
                    && foreground.timeStamp > lastRedirectedEvent) {
                lastRedirectedEvent = foreground.timeStamp;
                startActivity(new Intent(HomeWatchService.this, HomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
            handler.postDelayed(this, POLL_MS);
        }
    };

    private static final class Foreground {
        final String packageName;
        final long timeStamp;
        Foreground(String packageName, long timeStamp) {
            this.packageName = packageName;
            this.timeStamp = timeStamp;
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread(TAG);
        thread.start();
        handler = new Handler(thread.getLooper());
        usageStats = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || !hasUsageStatsAccess()) {
            Log.w(TAG, "Usage Stats access unavailable; Home redirect watcher stopped");
            stopSelf();
            return START_NOT_STICKY;
        }
        handler.removeCallbacks(poll);
        handler.post(poll);
        return START_STICKY;
    }

    private boolean hasUsageStatsAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return appOps != null && appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private Foreground latestForeground() {
        if (usageStats == null) return null;
        long now = System.currentTimeMillis();
        UsageEvents events = usageStats.queryEvents(now - LOOKBACK_MS, now);
        UsageEvents.Event event = new UsageEvents.Event();
        String latestPackage = null;
        long latestTime = 0;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latestPackage = event.getPackageName();
                latestTime = event.getTimeStamp();
            }
        }
        return latestPackage == null ? null : new Foreground(latestPackage, latestTime);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(poll);
        thread.quitSafely();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
