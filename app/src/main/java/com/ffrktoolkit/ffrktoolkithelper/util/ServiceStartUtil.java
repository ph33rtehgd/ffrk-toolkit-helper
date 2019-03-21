package com.ffrktoolkit.ffrktoolkithelper.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceStartUtil {

    public static void startService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        }
        else {
            context.startService(intent);
        }
    }


}
