package com.ffrktoolkit.ffrktoolkithelper;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
    private WindowManager.LayoutParams mWindowsParams;
    private int currentPage = 1;

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
    //@TargetApi(23)
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String overlayMode = prefs.getString("overlay_mode", "dynamic");
        List<String> drops = null;
        if (intent != null) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "Overlay intent action: " + action);
            if ("showOverlay".equalsIgnoreCase(action)) {
                prefs.edit().putBoolean("enableOverlay", true).commit();
            }

            drops = intent.getStringArrayListExtra("drops");
        }

        final boolean isOverlayEnabled = prefs.getBoolean("enableOverlay", false);
        Log.d(LOG_TAG, "overlay enabled: " + isOverlayEnabled);
        if (!isOverlayEnabled) {
            return super.onStartCommand(intent, flags, startId);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent overlayGrantIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                mContext.startActivity(overlayGrantIntent);
                //getApplicationContext().startActivity(overlayGrantIntent);
                return super.onStartCommand(intent, flags, startId);
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
                            prefs.edit()
                                    .putInt("overlayX", mWindowsParams.x)
                                    .putInt("overlayY", mWindowsParams.y)
                                    .commit();
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

            allAboutLayout(intent, overlayMode);
            moveView();
        }

        Log.d(LOG_TAG, "Drops broadcast received.");

        final TextView dropView = mView.findViewById(R.id.overlay_text);
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (19 * scale + 0.5f);
        if (drops != null) {
            String dropList = "";
            for (String drop : drops) {
                if (!"".equals(dropList)) {
                    dropList += "\n";
                }
                dropList += drop;
            }

            if ("".equals(dropList)) {
                dropList += getString(R.string.overlay_no_drops);
            }


            if ("dynamic".equalsIgnoreCase(overlayMode)) {
                final RelativeLayout dropContainer = mView.findViewById(R.id.overlay_container);
                final LinearLayout rootContainer = mView.findViewById(R.id.overlay_main);
                //final RelativeLayout overlayContainer = mView.findViewById(R.id.overlay_resizable);

                int containerSize = Math.max(pixels * drops.size(), pixels);
                dropContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, containerSize));
                dropView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, containerSize));
                LayoutWrapContentUpdater.wrapContentAgain(rootContainer);
            }

            dropView.setText(dropList);
        }
        else {
            dropView.setText(getString(R.string.overlay_waiting_for_data));

            if ("dynamic".equalsIgnoreCase(overlayMode)) {
                final RelativeLayout dropContainer = mView.findViewById(R.id.overlay_container);
                final LinearLayout rootContainer = mView.findViewById(R.id.overlay_main);
                dropContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, pixels));
                LayoutWrapContentUpdater.wrapContentAgain(rootContainer);
            }
        }

        if ("scrolling".equalsIgnoreCase(overlayMode)) {
            updateScrollViews();
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

    private void moveView() {
        //DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        //int width = (int) (metrics.widthPixels * 0.7f);
        //int height = (int) (metrics.heightPixels * 0.25f);

        mWindowsParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,//width,//WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,//height,//WindowManager.LayoutParams.WRAP_CONTENT,
                //WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,

                (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) ? WindowManager.LayoutParams.TYPE_PHONE : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                ,


                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH // Not displaying keyboard on bg activity's EditText
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);


        mWindowsParams.gravity = Gravity.TOP | Gravity.LEFT;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mWindowsParams.x = prefs.getInt("overlayX", 0);
        mWindowsParams.y = prefs.getInt("overlayY", 150);
        mWindowManager.addView(mView, mWindowsParams);
        mView.setOnTouchListener(touchListener);

        final View dropListView = mView.findViewById(R.id.overlay_resizable);
        dropListView.setOnTouchListener(touchListener);
    }

    private void allAboutLayout(Intent intent, String overlayMode) {

        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if ("scrolling".equalsIgnoreCase(overlayMode)) {
            mView = layoutInflater.inflate(R.layout.overlay_layout, null);
        }
        else {
            mView = layoutInflater.inflate(R.layout.overlay_layout_noscroll, null);
        }


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

    private void updateScrollViews() {
        if (mView != null) {
            Button upArrow = mView.findViewById(R.id.overlay_button_up_arrow);
            upArrow.setEnabled(false);
            upArrow.setAlpha(0.0f);
            Button downArrow = mView.findViewById(R.id.overlay_button_down_arrow);
            downArrow.setEnabled(false);
            downArrow.setAlpha(0.0f);

            TextView overlayText = mView.findViewById(R.id.overlay_text);
            overlayText.measure(0, View.MeasureSpec.UNSPECIFIED);

            RelativeLayout relativeLayout = mView.findViewById(R.id.overlay_container);
            relativeLayout.measure(0, View.MeasureSpec.UNSPECIFIED);

            int layoutHeight = relativeLayout.getMeasuredHeight() - relativeLayout.getBaseline();

            int lineHeight = overlayText.getLineHeight();
            int lineCount = overlayText.getLineCount();
            int textHeight = lineHeight * lineCount;
            if (textHeight > layoutHeight) {
                upArrow.setAlpha(0.1f);
                downArrow.setEnabled(true);
                downArrow.setAlpha(1.0f);
            }
        }
    }

    public void closeButtonClicked(View view) {
        //Toast t = Toast.makeText(this, "Should close", Toast.LENGTH_LONG);
        //t.show();
    }

    private int getLineHeight() {
        if (mView != null) {
            TextView overlayText = mView.findViewById(R.id.overlay_text);
            overlayText.measure(0, View.MeasureSpec.UNSPECIFIED);
            return overlayText.getLineHeight();
        }

        return 15;
    }

    private int getLinesPerPage() {
        if (mView != null) {
            RelativeLayout relativeLayout = mView.findViewById(R.id.overlay_container);
            relativeLayout.measure(0, View.MeasureSpec.UNSPECIFIED);

            int lineHeight = this.getLineHeight();
            int overlayHeight = relativeLayout.getMeasuredHeight();
            Log.d(LOG_TAG, "Lines per page: " + (int)Math.floor(overlayHeight/(double)lineHeight));
            return (int)Math.floor(overlayHeight/(double)lineHeight);
        }

        return 5;
    }

    private int getPageCount() {
        if (mView != null) {
            TextView overlayText = mView.findViewById(R.id.overlay_text);
            overlayText.measure(0, View.MeasureSpec.UNSPECIFIED);

            RelativeLayout relativeLayout = mView.findViewById(R.id.overlay_container);
            relativeLayout.measure(0, View.MeasureSpec.UNSPECIFIED);

            int lineHeight = overlayText.getLineHeight();
            int lineCount = overlayText.getLineCount();

            return lineHeight * lineCount - overlayText.getBaseline();
        }

        Log.d(LOG_TAG, "Returning default page count");

        return 1;
    }

    private void nextPage() {
        ++currentPage;
        int pageCount = getPageCount();
        if (currentPage >= pageCount) {
            currentPage = pageCount;
        }
    }

    private void prevPage() {
        --currentPage;
        if (currentPage < 1) {
            currentPage = 1;
        }
    }

    public void upButtonClicked(View view) {
        if (mView != null) {
            prevPage();

            if (currentPage == 1) {
                Button upArrow = mView.findViewById(R.id.overlay_button_up_arrow);
                upArrow.setEnabled(false);
                upArrow.setAlpha(0.1f);
            }
            Button downArrow = mView.findViewById(R.id.overlay_button_down_arrow);
            downArrow.setEnabled(true);
            downArrow.setAlpha(1.0f);

            int linesPerPage = this.getLinesPerPage();
            int lineHeight = this.getLineHeight();

            TextView overlayText = mView.findViewById(R.id.overlay_text);
            overlayText.scrollTo(0, lineHeight*linesPerPage*(currentPage - 1));
        }
    }

    public void downButtonClicked(View view) {

        if (mView != null) {
            nextPage();

            if (currentPage == this.getPageCount()) {
                Button downArrow = mView.findViewById(R.id.overlay_button_down_arrow);
                downArrow.setEnabled(false);
                downArrow.setAlpha(0.1f);
            }
            Button upArrow = mView.findViewById(R.id.overlay_button_up_arrow);
            upArrow.setEnabled(true);
            upArrow.setAlpha(1.0f);

            int linesPerPage = this.getLinesPerPage();
            int lineHeight = this.getLineHeight();

            TextView overlayText = mView.findViewById(R.id.overlay_text);
            overlayText.scrollTo(0, lineHeight*linesPerPage*(currentPage - 1) + 12);
        }

    }
}
