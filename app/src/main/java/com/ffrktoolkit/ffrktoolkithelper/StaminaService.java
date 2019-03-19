package com.ffrktoolkit.ffrktoolkithelper;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ffrktoolkit.ffrktoolkithelper.receivers.BroadcastIntentReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StaminaService extends IntentService {

    private String LOG_TAG = "FFRKToolkitHelper";

    private long REFRESH_INTERVAL = 900000L;

    private final static int STAMINA_NOTIFICATION_ID = 176123745;

    public StaminaService() {
        super("StaminaService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!getString(R.string.intent_update_stamina).equalsIgnoreCase(intent.getAction())) {
            Log.d(LOG_TAG, "Received invalid intent action for stamina service: " + intent.getAction());
            return;
        }

        updateNotification();
    }

    private void updateNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int random = (int)System.nanoTime();
        long serverTime = new Long(prefs.getLong("serverTime", 0)).intValue();
        int currentStamina = prefs.getInt("currentStamina", 0);
        int maxStamina = prefs.getInt("maxStamina", 0);
        int staminaRecoveryRemainingTime = prefs.getInt("staminaRecoveryRemainingTime", 0);
        int staminaRecoveryTime = prefs.getInt("staminaRecoveryTime", 180);

        Log.d(LOG_TAG, "Server time: " + serverTime);
        Log.d(LOG_TAG, "Current stamina: " + currentStamina);
        Log.d(LOG_TAG, "Max stamina: " + maxStamina);
        Log.d(LOG_TAG, "Stamina recovery remaining time: " + staminaRecoveryRemainingTime);
        Log.d(LOG_TAG, "Stamina recovery time: " + staminaRecoveryTime);

        long currentSeconds = (int)(System.currentTimeMillis() / 1000L);
        int secondsRemaining = staminaRecoveryRemainingTime - (int) (currentSeconds - serverTime);
        if (secondsRemaining < 0) {
            staminaRecoveryRemainingTime = 0;
        }
        currentStamina = maxStamina - (int) Math.ceil((double) secondsRemaining / (double) staminaRecoveryTime);

        //Log.d(LOG_TAG, "Seconds missing: " + secondsRemaining);
        //Log.d(LOG_TAG, "Recovery missing: " + staminaRecoveryTime);
        //Log.d(LOG_TAG, "Stamina missing: " + (int) Math.ceil((double) secondsRemaining / (double) staminaRecoveryTime));

        String timeRemainingToFull = String.format("%d:%02d:%02d", new Object[] { Integer.valueOf(staminaRecoveryRemainingTime / 3600), Integer.valueOf(staminaRecoveryRemainingTime % 3600 / 60), Integer.valueOf(staminaRecoveryRemainingTime % 60) });
        String timeFullyRecovered = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date((currentSeconds + staminaRecoveryRemainingTime) * 1000L));
        Notification.Builder notification = new Notification.Builder(this.getApplicationContext())
                .setSmallIcon(R.drawable.ic_proxy_notification_icon)
                .setContentTitle(String.format(getString(R.string.stamina_notification_current), new Object[] { currentStamina, maxStamina } ))
                .setProgress(maxStamina, currentStamina, false);
                //.setContentText(String.format(getString(R.string.stamina_notification_time_left), new Object[] { timeRemainingToFull, timeFullyRecovered } ));

        Log.d(LOG_TAG, timeRemainingToFull);
        Log.d(LOG_TAG, timeFullyRecovered);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(LOG_TAG, "Current: " + currentSeconds);
            Log.d(LOG_TAG, "Remaning: " + secondsRemaining);
            Log.d(LOG_TAG, "When: " + (currentSeconds + secondsRemaining) * 1000);

            notification
                    .setWhen((currentSeconds + secondsRemaining) * 1000)  // the time stamp, you will probably use System.currentTimeMillis() for most scenario
                    .setShowWhen(true)
                    .setContentText(String.format(getString(R.string.stamina_notification_time_left_24), new Object[] { timeFullyRecovered } ))
                    .setUsesChronometer(true)
                    .setOnlyAlertOnce(true)
                    .setChronometerCountDown(true);
        }
        else {
            notification.setContentText(String.format(getString(R.string.stamina_notification_time_left), new Object[] { timeRemainingToFull, timeFullyRecovered } ));

            AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, StaminaService.class);
            alarmIntent.setAction(getString(R.string.intent_update_stamina));
            startService(alarmIntent);

            PendingIntent alarmPendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    random,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), REFRESH_INTERVAL, alarmPendingIntent);
        }

        random = (int)System.nanoTime();
        Intent settingsIntent = new Intent(getApplicationContext(), FFRKToolkitHelperActivity.class);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                random,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        notification.setContentIntent(settingsPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.setChannelId(LOG_TAG);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(LOG_TAG, LOG_TAG, importance);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(StaminaService.STAMINA_NOTIFICATION_ID, notification.build());
        //startForeground(StaminaService.STAMINA_NOTIFICATION_ID, notification.build());
    }
}
