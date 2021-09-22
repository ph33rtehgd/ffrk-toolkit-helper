package com.ffrktoolkit.ffrktoolkithelper;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import androidx.multidex.MultiDex;

public class FFRKToolkitHelperApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
        //core configuration:
        builder
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withEnabled(true);
        //each plugin you chose above can be configured with its builder like this:
        builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
                .withUri("https://acrarium.thisisourcorner.net/report")
                //optional. Enables http basic auth
                .withBasicAuthLogin("CSAXoCsGeQyW8MV5")
                //required if above set
                .withBasicAuthPassword("lVRqtrjRXar3OIvs")
                // defaults to POST
                .withHttpMethod(HttpSender.Method.POST)
                //defaults to 5000ms
                .withConnectionTimeout(5000)
                //defaults to 20000ms
                .withSocketTimeout(20000)
                // defaults to false
                .withDropReportsOnTimeout(false)
                //defaults to false. Recommended if your backend supports it
                .withCompress(false)
                .withEnabled(true);
        ACRA.init(this, builder);
    }

}
