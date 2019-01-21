package com.ffrktoolkit.ffrktoolkithelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.ffrktoolkit.ffrktoolkithelper.views.LayoutWrapContentUpdater;

import java.util.List;

public class OverlayService extends Service {

    private String LOG_TAG = "FFRKToolkitHelper";
    private Context mContext;
    private WindowManager mWindowManager;
    private View mView;
    private View.OnTouchListener touchListener;

    public OverlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (intent != null) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "Overlay intent actiom: " + action);
            if ("showOverlay".equalsIgnoreCase(action)) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                prefs.edit().putBoolean("enableOverlay", true).commit();
            }
        }

        if (mView == null) {
            touchListener = new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                long startTime = System.currentTimeMillis();
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (System.currentTimeMillis() - startTime <= 300) {
                        return false;
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = mWindowsParams.x;
                            initialY = mWindowsParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mWindowsParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                            mWindowsParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            mWindowManager.updateViewLayout(mView, mWindowsParams);
                            break;
                    }
                    return false;
                }
            };

            allAboutLayout(intent);
            moveView();
        }

        List<String> drops = intent.getStringArrayListExtra("drops");
        Log.d(LOG_TAG, "Drops broadcast received.");
        final TextView dropView = (TextView) mView.findViewById(R.id.overlay_text);
        final RelativeLayout dropContainer = (RelativeLayout) mView.findViewById(R.id.overlay_container);
        final RelativeLayout overlayContainer = (RelativeLayout) mView.findViewById(R.id.overlay_resizable);
        final LinearLayout rootContainer = (LinearLayout) mView.findViewById(R.id.overlay_main);

        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (19 * scale + 0.5f);
        int containerSize = pixels * 5;
        if (drops != null) {
            String dropList = "";
            for (String drop : drops) {
                if (!"".equals(dropList)) {
                    dropList += "\n";
                }
                dropList += drop;
            }

            containerSize = Math.max(pixels * drops.size(), pixels * 5);
            dropContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, containerSize));
            dropView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, containerSize));
            //dropView.setHeight(containerSize);
            //dropView.setHeight(Math.min(containerSize, pixels * 5));
            //dropView.requestLayout();

            LayoutWrapContentUpdater.wrapContentAgain(rootContainer);

            //rootContainer.invalidate();
            //rootContainer.requestLayout();

            dropView.setText(dropList);
        }
        else {
            dropView.setText("");
            dropContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, containerSize));
            dropView.setHeight(pixels * 5);
            LayoutWrapContentUpdater.wrapContentAgain(rootContainer);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        if (mView != null) {
            mWindowManager.removeView(mView);
        }
        super.onDestroy();
    }

    WindowManager.LayoutParams mWindowsParams;
    private void moveView() {
        //DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        //int width = (int) (metrics.widthPixels * 0.7f);
        //int height = (int) (metrics.heightPixels * 0.25f);

        mWindowsParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,//width,//WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,//height,//WindowManager.LayoutParams.WRAP_CONTENT,
                //WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,

                (Build.VERSION.SDK_INT <= 25) ? WindowManager.LayoutParams.TYPE_PHONE : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                ,

                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH // Not displaying keyboard on bg activity's EditText
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        mWindowsParams.gravity = Gravity.TOP | Gravity.LEFT;
        //params.x = 0;
        mWindowsParams.y = 150;
        mWindowManager.addView(mView, mWindowsParams);

        mView.setOnTouchListener(touchListener);

        final View dropListView = mView.findViewById(R.id.overlay_resizable);
        dropListView.setOnTouchListener(touchListener);
    }

    private void allAboutLayout(Intent intent) {

        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.overlay_layout, null);

        final TextView tvValue = (TextView) mView.findViewById(R.id.overlay_text);
        Button btnClose = (Button) mView.findViewById(R.id.overlay_button_close);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                prefs.edit().putBoolean("enableOverlay", false).commit();
                stopSelf();
            }
        });

    }
}
