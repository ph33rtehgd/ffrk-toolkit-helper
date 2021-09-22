package com.ffrktoolkit.ffrktoolkithelper.util;

import android.util.Log;

import com.ffrktoolkit.ffrktoolkithelper.ProxyService;
import com.google.common.net.HttpHeaders;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.apache.commons.lang3.StringUtils;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.CharsetUtil;

public class FfrkFilterAdapter extends HttpFiltersAdapter {

    private ProxyService proxyService;
    private String LOG_TAG = "FFRKToolkitHelper";
    private String uri;

    public FfrkFilterAdapter(HttpRequest originalRequest, ProxyService proxyService, String uri) {
        super(originalRequest);
        this.proxyService = proxyService;
        this.uri = uri;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        // TODO: implement your filtering here
        //Log.d(LOG_TAG, "Client to proxy: " + uri);
        if (isUrlForFfrk(uri) && (httpObject instanceof HttpRequest)) {
            Log.d(LOG_TAG, "Sending request to " + ((HttpRequest) httpObject).getUri());
        }

        return null;
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

        //Log.d(LOG_TAG, httpObject.getClass().getName());
        if (httpObject instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) httpObject;
            String responseContent = response.content().toString(CharsetUtil.UTF_8);

            if (isUrlForFfrk(uri)) {
                Log.d(LOG_TAG, "Received response for " + uri);

                try {
                    Log.i(LOG_TAG, "Content length: " + response.headers().get(HttpHeaders.CONTENT_LENGTH));
                    Log.i(LOG_TAG, "Actual length: " + response.content().readableBytes());
                    response.headers().set(HttpHeaders.CONTENT_LENGTH, response.content().readableBytes());
                    Iterator<Map.Entry<String, String>> it = response.headers().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> i = it.next();
                        Log.i(LOG_TAG, i.getKey() + ": " + i.getValue());
                    }
                }
                catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }

                try {
                    URL urlPath = new URL(uri);
                    Log.d(LOG_TAG, "Response path: " + urlPath.getPath());
                    proxyService.parseFfrkResponse(originalRequest, responseContent);
                    Log.d(LOG_TAG, StringUtils.truncate(responseContent, 1000));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error in response processing.", e);
                    crashlytics.log("Exception while parsing response content.");
                    crashlytics.recordException(e);
                }
            }
        }
        else {
            Log.d(LOG_TAG, "Got response of type: " + httpObject.getClass());
        }

        return super.serverToProxyResponse(httpObject);
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (isUrlForFfrk(uri)) {
            Log.d(LOG_TAG, "Sending response to client for " + uri);
            Log.d(LOG_TAG, httpObject.toString());
        }
        return super.proxyToClientResponse(httpObject);
    }

    private boolean isUrlForFfrk(String url) {
        return url != null && ((url.contains("ffrk.denagames.com")) || (url.contains("dff.sp.mbga.jp")));
    }

}
