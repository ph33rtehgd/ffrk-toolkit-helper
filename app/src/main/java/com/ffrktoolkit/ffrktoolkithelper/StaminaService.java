package com.ffrktoolkit.ffrktoolkithelper;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.ffrktoolkit.ffrktoolkithelper.receivers.BroadcastIntentReceiver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StaminaService extends Service {

    private String LOG_TAG = "FFRKToolkitHelper";

    private long REFRESH_INTERVAL = 180000L;

    private final static int STAMINA_NOTIFICATION_ID = 176123745;

    /*public StaminaService() {
        super("StaminaService");
    }*/

    /*public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, StaminaService.class, 1238, intent);
    }*/

    /*@Override
    protected void onHandleWork(Intent intent) {
        onHandleIntent(intent);
    }*/


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    //@TargetApi(23)
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    //@Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (getString(R.string.intent_update_stamina).equalsIgnoreCase(intent.getAction())) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            final boolean isStaminaNotificationEnabled = sharedPreferences.getBoolean("enableStaminaTracker", false);
            if (isStaminaNotificationEnabled) {
                updateNotification();
            }
        }
        else if (getString(R.string.intent_stop_stamina_tracker).equalsIgnoreCase(intent.getAction())) {
            Log.d(LOG_TAG, "Cancel stamina tracker alarm");
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder notification = new Notification.Builder(this.getApplicationContext())
                    .setSmallIcon(R.drawable.ic_proxy_notification_icon);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification.setChannelId(LOG_TAG);

                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(LOG_TAG, LOG_TAG, importance);
                notificationManager.createNotificationChannel(channel);
            }

            this.startForeground(StaminaService.STAMINA_NOTIFICATION_ID, notification.build());
            notificationManager.cancel(StaminaService.STAMINA_NOTIFICATION_ID);

            AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, BroadcastIntentReceiver.class);
            alarmIntent.setAction(getString(R.string.intent_stop_stamina_tracker));
            PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            alarmManager.cancel(alarmPendingIntent);
            this.stopSelf();
        }
        else {
            Log.d(LOG_TAG, "Received invalid intent action for stamina service: " + intent.getAction());
        }
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
            secondsRemaining = 0;
            staminaRecoveryRemainingTime = 0;
        }
        currentStamina = Math.min(maxStamina - (int) Math.ceil((double) secondsRemaining / (double) staminaRecoveryTime), maxStamina);

        //Log.d(LOG_TAG, "Seconds missing: " + secondsRemaining);
        //Log.d(LOG_TAG, "Recovery missing: " + staminaRecoveryTime);
        //Log.d(LOG_TAG, "Stamina missing: " + (int) Math.ceil((double) secondsRemaining / (double) staminaRecoveryTime));

        String timeRemainingToFull = String.format("%d:%02d:%02d", new Object[] { Integer.valueOf(secondsRemaining / 3600), Integer.valueOf(secondsRemaining % 3600 / 60), Integer.valueOf(secondsRemaining % 60) });
        String timeFullyRecovered = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date((currentSeconds + secondsRemaining) * 1000L));
        //String timeFullyRecovered = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date((currentSeconds + secondsRemaining) * 1000L));

        Notification.Builder notification = new Notification.Builder(this.getApplicationContext())
                .setSmallIcon(R.drawable.ic_proxy_notification_icon)
                .setContentTitle(String.format(getString(R.string.stamina_notification_current), new Object[] { currentStamina, maxStamina } ))
                .setProgress(maxStamina, currentStamina, false);
                //.setContentText(String.format(getString(R.string.stamina_notification_time_left), new Object[] { timeRemainingToFull, timeFullyRecovered } ));

        Log.d(LOG_TAG, timeRemainingToFull);
        Log.d(LOG_TAG, timeFullyRecovered);

        if (currentStamina >= maxStamina) {
            notification
                    .setContentText(getString(R.string.stamina_restored))
                    .setOnlyAlertOnce(true);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(LOG_TAG, "Current: " + currentSeconds);
                Log.d(LOG_TAG, "Remaining: " + secondsRemaining);
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
            }
        }

        long millisUntilNextStaminaTick = (secondsRemaining % staminaRecoveryTime) * 1000;
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, BroadcastIntentReceiver.class);
        alarmIntent.setAction(getString(R.string.intent_update_stamina));

        PendingIntent existingAlarm = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, PendingIntent.FLAG_NO_CREATE);
        boolean isAlarmSet = existingAlarm != null;
        if (existingAlarm != null) {
            alarmManager.cancel(existingAlarm);
        }

        if (currentStamina >= maxStamina) {
            Log.d(LOG_TAG, "Cancel stamina refresh alarm, stamina is full.");
        }
        else {
            Log.d(LOG_TAG, "Setting alarm.");
            //startService(alarmIntent);
            PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, (SystemClock.elapsedRealtime() + millisUntilNextStaminaTick), REFRESH_INTERVAL, alarmPendingIntent);
            Log.d(LOG_TAG, "next alarm: " + (SystemClock.elapsedRealtime() + millisUntilNextStaminaTick));
            Log.d(LOG_TAG, "current clock: " + SystemClock.elapsedRealtime());
        }

        /*if (!isAlarmSet) {

        }
        else {
            Log.d(LOG_TAG, "Alarm is set: " + existingAlarm);
            if (currentStamina >= maxStamina) {
                alarmManager.cancel(existingAlarm);
                Log.d(LOG_TAG, "Cancel stamina refresh alarm, stamina is full.");
            }
        }*/

        Log.d(LOG_TAG, "Extra millis: " + millisUntilNextStaminaTick);
        Log.d(LOG_TAG, "Refresh interval" + REFRESH_INTERVAL);

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

        Notification builtNotification = notification.build();
        this.startForeground(StaminaService.STAMINA_NOTIFICATION_ID, builtNotification);
        //notificationManager.notify(StaminaService.STAMINA_NOTIFICATION_ID, builtNotification);
    }
}
