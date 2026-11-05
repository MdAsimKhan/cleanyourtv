package com.gathin.bingwallpaper;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BingJobService extends JobService {
    private static final String TAG = "BingJobService";
    private static final int JOB_ID_PERIODIC = 1001;
    private static final int JOB_ID_BOOT = 1002;
    private static final long INTERVAL_MS = 24 * 60 * 60 * 1000L;
    private static final long BOOT_DELAY_MS = 10 * 60 * 1000L; // 10 minutes

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public boolean onStartJob(JobParameters params) {
        executor.execute(() -> {
            boolean success = BingApi.fetchToday(this);
            jobFinished(params, !success);
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    public static void schedulePeriodic(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo existing = js.getPendingJob(JOB_ID_PERIODIC);
        if (existing != null) return;

        JobInfo job = new JobInfo.Builder(JOB_ID_PERIODIC, new ComponentName(context, BingJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(INTERVAL_MS)
                .setPersisted(true)
                .build();
        js.schedule(job);
        Log.i(TAG, "Scheduled periodic 24-hour daily wallpaper job");
    }

    public static void scheduleBootDelay(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null) return;

        JobInfo job = new JobInfo.Builder(JOB_ID_BOOT, new ComponentName(context, BingJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(BOOT_DELAY_MS)
                .build();
        js.schedule(job);
        Log.i(TAG, "Scheduled delayed post-boot wallpaper check (10 min)");
    }
}
