package com.ffrktoolkit.ffrktoolkithelper.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ffrktoolkit.ffrktoolkithelper.R;
import com.ffrktoolkit.ffrktoolkithelper.StaminaService;

public class BroadcastIntentReceiver extends BroadcastReceiver {

    private String LOG_TAG = "FFRKToolkitHelper";

    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "Fired broadcast: " + intent);
        if (intent == null || intent.getAction() == null || context == null) {
            return;
        }

        if (intent.getAction().equals(context.getResources().getString(R.string.intent_update_stamina))) {
            Intent notificationIntent = new Intent(context, StaminaService.class);
            notificationIntent.setAction(context.getResources().getString(R.string.intent_update_stamina));
            context.startService(notificationIntent);
        }
        else if (intent.getAction().equals(context.getResources().getString(R.string.intent_stop_stamina_tracker))) {
            Intent notificationIntent = new Intent(context, StaminaService.class);
            notificationIntent.setAction(context.getResources().getString(R.string.intent_stop_stamina_tracker));
            context.startService(notificationIntent);
        }
    }

}
