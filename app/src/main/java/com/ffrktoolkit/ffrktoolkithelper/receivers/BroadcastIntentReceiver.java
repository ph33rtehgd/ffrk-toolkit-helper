package com.ffrktoolkit.ffrktoolkithelper.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ffrktoolkit.ffrktoolkithelper.R;
import com.ffrktoolkit.ffrktoolkithelper.StaminaService;

public class BroadcastIntentReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null || context == null) {
            return;
        }

        if (intent.getAction().equals(context.getResources().getString(R.string.intent_update_stamina))) {
            Intent notificationIntent = new Intent(context, StaminaService.class);
            notificationIntent.setAction(context.getResources().getString(R.string.intent_update_stamina));
            context.startService(notificationIntent);
        }
    }

}
