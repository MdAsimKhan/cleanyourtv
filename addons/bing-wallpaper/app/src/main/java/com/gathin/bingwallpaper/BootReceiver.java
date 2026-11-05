package com.gathin.bingwallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Immediate return: 0 network, 0 I/O. Defers work to JobScheduler after TV settles.
            BingJobService.schedulePeriodic(context);
            BingJobService.scheduleBootDelay(context);
        }
    }
}
