package com.ffrktoolkit.ffrktoolkithelper.fragments;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.ffrktoolkit.ffrktoolkithelper.OverlayService;
import com.ffrktoolkit.ffrktoolkithelper.R;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class OverlayFragment extends Fragment {

    private String LOG_TAG = "FFRKToolkitHelper";
    private final static int OVERLAY_REQUEST_CODE = 5001;

    public static OverlayFragment newInstance()
    {
        OverlayFragment overlayFragment = new OverlayFragment();
        overlayFragment.setArguments(new Bundle());
        return overlayFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View layout = layoutInflater.inflate(R.layout.overlay_settings, viewGroup, false);
        return layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final Button resetOverlayBtn = getView().findViewById(R.id.reset_overlay_btn);
        resetOverlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putInt("overlayX", 0)
                        .putInt("overlayY", 150)
                        .commit();

                Boolean isEnabled = prefs.getBoolean("enableOverlay", false);
                if (isEnabled) {
                    closeFloatingWindow();
                    checkDrawOverlayPermission();
                }
            }
        });

        final Spinner overlayModeSpinner = getView().findViewById(R.id.overlay_mode_spinner);
        final ArrayAdapter<CharSequence> overlaySpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.overlay_modes, android.R.layout.simple_spinner_item);
        overlaySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        overlayModeSpinner.setAdapter(overlaySpinnerAdapter);
        int overlayModePosition = overlayModeByLabel(prefs.getString("overlay_mode", "dynamic"));
        overlayModeSpinner.setSelection(overlayModePosition);

        overlayModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                String overlayMode = getOverlayMode();

                prefs.edit().putString("overlay_mode", overlayMode).commit();

                Boolean isEnabled = prefs.getBoolean("enableOverlay", false);
                if (isEnabled) {
                    closeFloatingWindow();
                    checkDrawOverlayPermission();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //parent.setSelection(0);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(getActivity())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + this.getActivity().getPackageName()));
                startActivityForResult(intent, OVERLAY_REQUEST_CODE);
            } else {
                openFloatingWindow();
            }
        } else {
            openFloatingWindow();
        }
    }

    private void openFloatingWindow() {
        Intent intent = new Intent(getActivity(), OverlayService.class);
        getActivity().stopService(intent);
        getActivity().startService(intent);
    }

    private void closeFloatingWindow() {
        Intent intent = new Intent(getActivity(), OverlayService.class);
        getActivity().stopService(intent);
    }

    private String getOverlayMode() {
        try {
            final Spinner overlayModeSpinner = getView().findViewById(R.id.overlay_mode_spinner);
            String[] underlyingValues = getResources().getStringArray(R.array.overlay_modes);
            return underlyingValues[overlayModeSpinner.getSelectedItemPosition()];
        }
        catch (Exception e) {
            Log.d(LOG_TAG, "Exception while finding overlay mode, defaulting to dynamic.", e);
            return "dynamic";
        }
    }

    private int overlayModeByLabel(String label) {
        try {
            String[] underlyingValues = getResources().getStringArray(R.array.overlay_modes);

            for (int i = 0, len = underlyingValues.length; i < len; i++) {
                if (underlyingValues[i].equalsIgnoreCase(label)) {
                    return i;
                }
            }

            return 1;
        }
        catch (Exception e) {
            Log.d(LOG_TAG, "Exception while finding overlay mode position, defaulting to 1.", e);
            return 1;
        }
    }

}
