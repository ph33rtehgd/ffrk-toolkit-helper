package com.ffrktoolkit.ffrktoolkithelper.util;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class HttpRequestSingleton {
    private static HttpRequestSingleton mInstance;
    private RequestQueue requestQueue;
    private static Context ctx;
    private static final int TIMEOUT_MILLIS = 30000;

    private HttpRequestSingleton(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized HttpRequestSingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new HttpRequestSingleton(context);
        }

        return mInstance;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        setRetryPolicy(req);
        getRequestQueue().add(req);
    }

    private static <T> void setRetryPolicy(Request<T> req) {
        req.setRetryPolicy(new DefaultRetryPolicy(
                HttpRequestSingleton.TIMEOUT_MILLIS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    public static void cancelRequests(Context context) {
        HttpRequestSingleton.getInstance(context).getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }
}
