package com.ffrktoolkit.ffrktoolkithelper.fragments;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.ffrktoolkit.ffrktoolkithelper.OverlayService;
import com.ffrktoolkit.ffrktoolkithelper.ProxyService;
import com.ffrktoolkit.ffrktoolkithelper.R;
import com.ffrktoolkit.ffrktoolkithelper.parser.InventoryParser;
import com.ffrktoolkit.ffrktoolkithelper.util.DropUtils;
import com.ffrktoolkit.ffrktoolkithelper.util.HttpRequestSingleton;
import com.ffrktoolkit.ffrktoolkithelper.util.JsonUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.CharsetUtil;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ProxyAndDataFragment extends Fragment {

    private String LOG_TAG = "FFRKToolkitHelper";
    private final static int SIGN_IN_REQUEST_CODE = 5000;
    private final static int OVERLAY_REQUEST_CODE = 5001;
    private InventoryParser parser = new InventoryParser();
    private BroadcastReceiver broadcastReceiver;
    private AtomicInteger updatesDone = new AtomicInteger(0);

    public static ProxyAndDataFragment newInstance()
    {
        ProxyAndDataFragment proxyAndDataFragment = new ProxyAndDataFragment();
        proxyAndDataFragment.setArguments(new Bundle());
        return proxyAndDataFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        registerReceiver();
        downloadDataMaps();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View layout = layoutInflater.inflate(R.layout.proxy_settings, viewGroup, false);
        return layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Switch enableProxySwitch = getView().findViewById(R.id.enable_proxy_switch);
        enableProxySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(getContext(), ProxyService.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (isChecked) {
                    intent.setAction(getContext().getString(R.string.intent_start_proxy));
                    prefs.edit().putBoolean("enableProxy", true).commit();
                }
                else {
                    intent.setAction(getContext().getString(R.string.intent_stop_proxy));
                    prefs.edit().putBoolean("enableProxy", false).commit();
                }

                getContext().startService(intent);
            }
        });

        final Switch enableOverlaySwitch = getView().findViewById(R.id.enable_overlay_switch);
        enableOverlaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (isChecked) {
                    checkDrawOverlayPermission();
                    prefs.edit().putBoolean("enableOverlay", true).commit();
                }
                else {
                    closeFloatingWindow();
                    prefs.edit().putBoolean("enableOverlay", false).commit();
                }
            }
        });

        final Button resetOverlayBtn = getView().findViewById(R.id.reset_overlay_btn);
        resetOverlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putInt("overlayX", 0)
                        .putInt("overlayY", 150)
                        .commit();

                if (enableOverlaySwitch.isChecked()) {
                    closeFloatingWindow();
                    checkDrawOverlayPermission();
                }
            }
        });

        final Button openWifiBtn = getView().findViewById(R.id.wifi_settings);
        openWifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("proxy_bypass", getResources().getString(R.string.proxy_bypass_list));
                clipboardManager.setPrimaryClip(clipData);

                Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.proxy_bypass_copied), Toast.LENGTH_SHORT);
                toast.show();

                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        final Button sendInventoryBtn = getView().findViewById(R.id.submit_inventory_btn);
        sendInventoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInventoryBtn.setEnabled(false);
                String region = getSelectedRegion();
                processInventoryData(region);
            }
        });

        final Button googleLoginBtn = getView().findViewById(R.id.google_login_btn);
        googleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), getGoogleSignInOptions());
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext());

                if (account != null) {
                    Log.d(LOG_TAG, "Already signed in.");
                    googleSignInClient.signOut().addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(LOG_TAG, "Signed out.");
                            updateLoginUi();
                        }
                    });
                }
                else {
                    Log.d(LOG_TAG, "Not signed in, starting sign in process.");
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
                }
            }
        });

        final Spinner regionUploadSpinner = getView().findViewById(R.id.region_upload_spinner);
        final ArrayAdapter<CharSequence> regionSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.ffrk_regions, android.R.layout.simple_spinner_item);
        regionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionUploadSpinner.setAdapter(regionSpinnerAdapter);

        regionUploadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkIfInventoryChanged();

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putInt("selected_region", position).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //parent.setSelection(0);
            }
        });

        final EditText proxyPortText = getView().findViewById(R.id.proxy_port);
        proxyPortText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event != null &&
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event == null || !event.isShiftPressed()) {
                        // the user is done typing.
                        String newPort = ((EditText) v).getText().toString();
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        int newPortInt = 0;
                        int originalPort = prefs.getInt("proxyPort", 0);
                        try {
                            newPortInt = Integer.valueOf(newPort);
                            if (newPortInt < 1024 || newPortInt > 49151) {
                                Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.default_port_range_message), Toast.LENGTH_SHORT);
                                toast.show();
                                proxyPortText.setText(getString(R.string.default_proxy_port));
                                newPortInt = Integer.valueOf(getString(R.string.default_proxy_port));
                            }
                        }
                        catch (Exception e) {
                            newPortInt = Integer.valueOf(getString(R.string.default_proxy_port));
                            proxyPortText.setText(getString(R.string.default_proxy_port));
                        }

                        if (originalPort != newPortInt) {
                            prefs.edit().putInt("proxyPort", newPortInt).commit();
                            final Switch enableProxySwitch = (Switch) getView().findViewById(R.id.enable_proxy_switch);
                            if (enableProxySwitch.isChecked()) {
                                Intent intent = new Intent(getContext(), ProxyService.class);
                                intent.setAction(getContext().getString(R.string.intent_change_proxy_port));
                                getContext().startService(intent);
                            }
                        }

                        return true; // consume.
                    }
                }
                return false; // pass on to other listeners.
            }
        });

        checkIfAlreadySignedIn();
        checkIfInventoryChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        switch (requestCode) {
            case OVERLAY_REQUEST_CODE: {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (Settings.canDrawOverlays(getActivity())) {
                        openFloatingWindow();
                    }
                } else {
                    openFloatingWindow();
                }
                break;
            }
            case SIGN_IN_REQUEST_CODE: {
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkIfAlreadySignedIn();
        checkIfInventoryChanged();
        updateDataMaps();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isProxyEnabled = prefs.getBoolean("enableProxy", false);

        final Switch enableProxySwitch = (Switch) getView().findViewById(R.id.enable_proxy_switch);
        enableProxySwitch.setChecked(isProxyEnabled);

        boolean isOverlayEnabled = prefs.getBoolean("enableOverlay", false);
        final Switch enableOverlaySwitch = (Switch) getView().findViewById(R.id.enable_overlay_switch);
        enableOverlaySwitch.setChecked(isOverlayEnabled);

        if (isOverlayEnabled) {
            checkDrawOverlayPermission();
        }

        final EditText proxyPortText = (EditText) getView().findViewById(R.id.proxy_port);
        int proxyPort = prefs.getInt("proxyPort", Integer.valueOf(getString(R.string.default_proxy_port)));
        proxyPortText.setText(String.valueOf(proxyPort));

        final Spinner regionUploadSpinner = getView().findViewById(R.id.region_upload_spinner);
        int selectedRegion = prefs.getInt("selected_region", 0);
        regionUploadSpinner.setSelection(selectedRegion);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(broadcastReceiver != null) {
            try {
                this.getActivity().getApplicationContext().unregisterReceiver(broadcastReceiver);
            }
            catch (Exception e) {}
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            final Button googleLoginBtn = (Button) getView().findViewById(R.id.google_login_btn);
            googleLoginBtn.setText(R.string.pref_title_google_logout);

            final TextView loginStatusText = (TextView) getView().findViewById(R.id.login_status);
            loginStatusText.setText(getString(R.string.pref_description_google_logout) + account.getEmail());

            final Button submitInventoryBtn = (Button) this.getActivity().findViewById(R.id.submit_inventory_btn);
            submitInventoryBtn.setEnabled(true);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(LOG_TAG, "signInResult:failed code=" + e.getStatusCode());
            Log.w(LOG_TAG, e.getMessage());
        }
    }

    private void checkIfInventoryChanged() {
        final String selectedRegion = getSelectedRegion();
        final Button sendInventoryBtn = this.getView().findViewById(R.id.submit_inventory_btn);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Drawable statusIcon = null;
        Log.d(LOG_TAG, "Has inventory changed: " + prefs.getBoolean("hasInventoryChanged_" + selectedRegion, false));
        if (!prefs.contains("hasInventoryChanged_" + selectedRegion)) {
            statusIcon = getResources().getDrawable(R.drawable.ic_clear_black_24dp);
        }
        else if (prefs.getBoolean("hasInventoryChanged_" + selectedRegion, false)) {
            statusIcon = getResources().getDrawable(R.drawable.ic_update_black_24dp);
        }
        else {
            statusIcon = getResources().getDrawable(R.drawable.ic_thumb_up_black_24dp);
        }

        int h = statusIcon.getIntrinsicHeight();
        int w = statusIcon.getIntrinsicWidth();
        statusIcon.setBounds( 0, 0, w, h);

        sendInventoryBtn.setCompoundDrawables(statusIcon, null, null, null);
    }

    private void checkIfAlreadySignedIn() {
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), getGoogleSignInOptions());
        googleSignInClient.silentSignIn().addOnCompleteListener(getActivity(), new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                handleSignInResult(task);
            }
        });
    }

    private void updateLoginUi() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext());

        final Button googleLoginBtn = (Button) this.getActivity().findViewById(R.id.google_login_btn);
        final Button submitInventoryBtn = (Button) this.getActivity().findViewById(R.id.submit_inventory_btn);
        final TextView loginStatusText = (TextView) getView().findViewById(R.id.login_status);
        if (account != null) {
            googleLoginBtn.setText(R.string.pref_title_google_logout);
            loginStatusText.setText(getString(R.string.pref_description_google_logout) + account.getEmail());
            submitInventoryBtn.setEnabled(true);
        }
        else {
            googleLoginBtn.setText(R.string.pref_title_google_login);
            loginStatusText.setText(getString(R.string.logged_out));
            submitInventoryBtn.setEnabled(false);
        }
    }

    private GoogleSignInOptions getGoogleSignInOptions() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.oauth_token))
                .requestEmail()
                .build();
    }

    private void registerReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra("action");
                final Switch enableProxySwitch = (Switch) getActivity().findViewById(R.id.enable_proxy_switch);
                try {
                    if (getString(R.string.intent_stop_proxy).equals(action)) {
                        enableProxySwitch.setChecked(false);
                    }
                    else if (getString(R.string.intent_start_proxy).equals(action)) {
                        enableProxySwitch.setChecked(true);
                    }
                }
                catch (Exception e) {
                    Log.d(LOG_TAG, "Exception while trying to flip proxy switch.", e);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.ffrktoolkit.ffrktoolkithelper");
        this.getActivity().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
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

    private void processInventoryData(final String region) {
        String fileName = "global".equals(region) ? getString(R.string.file_inventory_global_json) : getString(R.string.file_inventory_jp_json);
        File inventoryFile = new File(getActivity().getApplicationContext().getFilesDir(), fileName);

        String relicFileName = "global".equals(region) ? getString(R.string.file_equipment_global_json) : getString(R.string.file_equipment_jp_json);
        File relicFile = new File(getActivity().getApplicationContext().getFilesDir(), relicFileName);

        String vaultFileName = "global".equals(region) ? getString(R.string.file_vault_global_json) : getString(R.string.file_vault_jp_json);
        File vaultFile = new File(getActivity().getApplicationContext().getFilesDir(), vaultFileName);

        String materialFileName = "global".equals(region) ? getString(R.string.file_materials_global_json) : getString(R.string.file_materials_jp_json);
        File materialFile = new File(getActivity().getApplicationContext().getFilesDir(), materialFileName);

        if ((!inventoryFile.exists() || inventoryFile.length() <= 0) && (!relicFile.exists() || relicFile.length() <= 0)
                && (!vaultFile.exists() || vaultFile.length() <= 0) && (!materialFile.exists() || materialFile.length() <= 0)) {
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.exception_no_inventory_data_toast), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext());
        if (account == null) {
            Log.d(LOG_TAG,"Tried to submit without signing in.");
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_not_signed_in), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        final ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setTitle(getString(R.string.upload_progress_dialog_title));
        progress.setMessage(getString(R.string.upload_progress_dialog_loading_message));
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(false);
        progress.setCancelable(false);
        progress.setProgress(0);
        progress.setMax(100);
        progress.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), new DialogInterface.OnClickListener(){
            // Set a click listener for progress dialog cancel button
            @Override
            public void onClick(DialogInterface dialog, int which){
                progress.cancel();
                HttpRequestSingleton.cancelRequests(getActivity());
            }
        });
        updatesDone.set(0);
        progress.show();

        Log.d(LOG_TAG, "Starting inventory calls");

        if (inventoryFile.exists()) {
            String inventoryJsonString = JsonUtils.getStringFromFile(inventoryFile, this.getActivity());
            final String inventoryJson = inventoryJsonString;
            Map<String, String> inventoryParams = new HashMap<>();
            inventoryParams.put("function", "loadUserInventory");
            inventoryParams.put("network", "Android");
            inventoryParams.put("uid", account.getEmail());
            inventoryParams.put("token", account.getIdToken());
            startInventorySave(progress, inventoryJson, region, account, inventoryParams);
        }

        if (relicFile.exists() || vaultFile.exists()) {
            JSONObject relicInventory = new JSONObject();
            JSONArray relics = new JSONArray();
            try {
                relicInventory.put("equipments", relics);
            }
            catch (JSONException e) {}

            if (relicFile.exists()) {
                Log.d(LOG_TAG, "Inside relic gather call.");
                String relicJsonString = JsonUtils.getStringFromFile(relicFile, this.getActivity());
                final String relicJson = relicJsonString;

                try {
                    JSONObject inventoryJson = new JSONObject(relicJson);
                    JSONArray equipments = inventoryJson.optJSONArray("equipments");
                    for (int i = 0, len = equipments.length(); i < len; i++) {
                        relics.put(equipments.get(i));
                    }
                }
                catch (Exception e) {
                    hideProgressWhenComplete(progress);
                    Log.w(LOG_TAG, "Exception while parsing inventory relic JSON.", e);
                }
            }
            else {
                hideProgressWhenComplete(progress);
            }

            if (vaultFile.exists()) {
                Log.d(LOG_TAG, "Inside vault gather call.");
                String relicJsonString = JsonUtils.getStringFromFile(vaultFile, this.getActivity());
                final String relicJson = relicJsonString;

                try {
                    JSONObject inventoryJson = new JSONObject(relicJson);
                    JSONArray equipments = inventoryJson.optJSONArray("equipments");

                    for (int i = 0, len = equipments.length(); i < len; i++) {
                        equipments.getJSONObject(i).put("isVaulted", true);
                        relics.put(equipments.get(i));
                    }
                }
                catch (Exception e) {
                    hideProgressWhenComplete(progress);
                    Log.w(LOG_TAG, "Exception while parsing inventory relic JSON.", e);
                }
            }
            else {
                hideProgressWhenComplete(progress);
            }

            callSaveRelicInventory(account, relicInventory, region, progress);
        }

        if (inventoryFile.exists()) {
            if ("global".equals(region)) {
                try {
                    String materialJsonString = JsonUtils.getStringFromFile(materialFile, this.getActivity());
                    JSONObject transformedJson = parser.parseOrbInventoryToJson(new JSONObject(materialJsonString));
                    callSaveMaterialInventory(account, transformedJson.toString(), region, progress);
                }
                catch (Exception e) {
                    Log.d(LOG_TAG, "Exception while parsing material inventory to send.", e);
                }
            }
            else {
                hideProgressWhenComplete(progress);
            }
        }
        else {
            hideProgressWhenComplete(progress);
        }
    }

    private void startInventorySave(final ProgressDialog progress, final String jsonBody, final String region, final GoogleSignInAccount account, final Map<String, String> paramMap) {
        String url = getString(R.string.user_functions_url);
        StringRequest httpRequest = new StringRequest
                (Request.Method.POST, url, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        Log.d(LOG_TAG, "Response: " + response);
                        progress.setMessage(getString(R.string.upload_progress_dialog_loaded_message));
                        progress.setProgress(progress.getProgress() + 25);
                        JSONObject json = new JSONObject();
                        hideProgressWhenComplete(progress);
                        try {
                            if (response != null && !"false".equals(response) && !"".equals(response)) {
                                if ("[]".equals(response)) {
                                    json = new JSONObject();
                                }
                                else {
                                    json = new JSONObject(response);
                                }
                            }

                            callSaveInventory(account, jsonBody, json, region, progress);
                        }
                        catch (JSONException e) {
                            hideProgressWhenComplete(progress);
                            Log.w(LOG_TAG,"Exception while parsing inventory JSON", e);
                            return;
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressWhenComplete(progress);
                        hideProgressWhenComplete(progress);
                        Log.e(LOG_TAG, "Error in response: " + error.toString());
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.exception_loading_inventory), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                return paramMap;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };

        HttpRequestSingleton.getInstance(getActivity()).addToRequestQueue(httpRequest);
    }

    private void callSaveInventory(final GoogleSignInAccount account, String rawInventoryJson, JSONObject loadedInventoryJson, String region, final ProgressDialog progress) {
        Log.d(LOG_TAG, "Original inventory: " + loadedInventoryJson.toString());
        String url = getString(R.string.user_functions_url);
        final JSONObject mergedInventory = parser.parseJsonToInventoryFormat(rawInventoryJson, loadedInventoryJson, region);

        Log.d(LOG_TAG, "Merged inventory: " + mergedInventory.toString());
        progress.setMessage(getString(R.string.upload_progress_dialog_saving_message));
        StringRequest saveInventoryRequest = new StringRequest
                (Request.Method.POST, url, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        hideProgressWhenComplete(progress);
                        progress.setProgress(progress.getProgress() + 25);
                        Log.d(LOG_TAG, "Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progress.cancel();
                        Log.e(LOG_TAG, "Error in response: " + error.toString());
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.exception_saving_inventory), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String, String> params = new HashMap<>();
                params.put("function", "importInventoryJson");
                params.put("network", "Android");
                params.put("uid", account.getEmail());
                params.put("token", account.getIdToken());
                params.put("inventoryData", mergedInventory.toString());

                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };

        // Access the RequestQueue through your singleton class.
        HttpRequestSingleton.getInstance(getActivity()).addToRequestQueue(saveInventoryRequest);
    }

    private void callSaveRelicInventory(final GoogleSignInAccount account, final JSONObject relicInventory, final String region, final ProgressDialog progress) {
        String url = getString(R.string.user_functions_url);

        Log.d(LOG_TAG, "Inside relic save call.");
        //Log.d(LOG_TAG, "Merged inventory: " + relicInventory.toString());
        try {
            progress.setMessage(getString(R.string.upload_progress_dialog_saving_message));
            StringRequest saveInventoryRequest = new StringRequest
                    (Request.Method.POST, url, new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            Log.d(LOG_TAG, "Response: " + response.toString());
                            int progressToMove = "global".equals(region) ? 25 : 50;
                            progress.setProgress(progress.getProgress() + progressToMove);
                            hideProgressWhenComplete(progress);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            hideProgressWhenComplete(progress);
                            Log.e(LOG_TAG, "Error in response: " + error.toString());
                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.exception_saving_inventory), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("function", "importRelicJson");
                    params.put("network", "Android");
                    params.put("uid", account.getEmail());
                    params.put("token", account.getIdToken());
                    params.put("region", region);
                    params.put("inventoryData", relicInventory.toString());

                    return params;
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> params = new HashMap<>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    return params;
                }
            };

            // Access the RequestQueue through your singleton class.
            HttpRequestSingleton.getInstance(getActivity()).addToRequestQueue(saveInventoryRequest);
        }
        catch (Throwable e){
            Log.e(LOG_TAG, "Exception while sending relic inventory.", e);
        }
    }

    private void callSaveMaterialInventory(final GoogleSignInAccount account, final String materialInventory, final String region, final ProgressDialog progress) {
        String url = getString(R.string.user_functions_url);

        Log.d(LOG_TAG, "Inside material save call.");
        try {
            progress.setMessage(getString(R.string.upload_progress_dialog_saving_message));
            StringRequest saveInventoryRequest = new StringRequest
                    (Request.Method.POST, url, new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            Log.d(LOG_TAG, "Response: " + response.toString());
                            progress.setProgress(progress.getProgress() + 50);
                            hideProgressWhenComplete(progress);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            hideProgressWhenComplete(progress);
                            Log.e(LOG_TAG, "Error in response: " + error.toString());
                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.exception_saving_inventory), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("function", "saveOrbInventory");
                    params.put("network", "Android");
                    params.put("uid", account.getEmail());
                    params.put("token", account.getIdToken());
                    params.put("region", region);
                    params.put("inventoryData", materialInventory);

                    return params;
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> params = new HashMap<>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    return params;
                }
            };

            // Access the RequestQueue through your singleton class.
            HttpRequestSingleton.getInstance(getActivity()).addToRequestQueue(saveInventoryRequest);
        }
        catch (Throwable e){
            Log.e(LOG_TAG, "Exception while sending material inventory.", e);
        }
    }

    private void hideProgressWhenComplete(ProgressDialog progress) {
        int currentFinished = updatesDone.incrementAndGet();
        String selectedRegion = getSelectedRegion();
        if (currentFinished == 4) {
            progress.dismiss();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.edit().putBoolean("hasInventoryChanged_" + selectedRegion, false).commit();
            checkIfInventoryChanged();
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_inventory_saved_successfully), Toast.LENGTH_SHORT);
            toast.show();

            final Button sendInventoryBtn = (Button) getActivity().findViewById(R.id.submit_inventory_btn);
            sendInventoryBtn.setEnabled(true);
        }
    }

    private String getSelectedRegion() {
        try {
            final Spinner regionUploadSpinner = getView().findViewById(R.id.region_upload_spinner);
            String[] underlyingValues = getResources().getStringArray(R.array.ffrk_region_values);
            return underlyingValues[regionUploadSpinner.getSelectedItemPosition()];
        }
        catch (Exception e) {
            Log.d(LOG_TAG, "Exception while finding region, defaulting to global.", e);
            return "global";
        }
    }

    private void downloadDataMaps() {
        String url = getString(R.string.data_maps_url);

        Log.d(LOG_TAG, "Inside update data maps.");
        try {
            StringRequest updateMapsRequest = new StringRequest
                    (Request.Method.GET, url, new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            //Log.d(LOG_TAG, "Response: " + response);
                            if (response != null && !"false".equals(response) && !"".equals(response)) {
                                FileOutputStream outputStream;
                                try {
                                    String fileName = getString(R.string.external_data_map_json);
                                    outputStream = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
                                    outputStream.write(response.getBytes());
                                    outputStream.close();
                                    updateDataMaps();
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "Exception while writing data map json to storage.", e);
                                    return;
                                }
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(LOG_TAG, "Error in response: " + error.toString());
                        }
                    });

            HttpRequestSingleton.getInstance(getActivity()).addToRequestQueue(updateMapsRequest);
        }
        catch (Throwable e){
            Log.e(LOG_TAG, "Exception while sending relic inventory.", e);
        }
    }

    private void updateDataMaps() {
        try {
            String fileName = getString(R.string.external_data_map_json);
            File file = new File(getActivity().getFilesDir(), fileName);

            if (file.exists()) {
                FileInputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);
                String dataMaps = new String(data, CharsetUtil.UTF_8);
                JSONObject dataMapsJson = new JSONObject(dataMaps);

                JSONArray dropIdsList = dataMapsJson.getJSONArray("dropIdsList");
                for (int i = 0, len = dropIdsList.length(); i < len; i++) {
                    JSONObject dropIdMapping = dropIdsList.getJSONObject(i);
                    DropUtils.addDropIdMapping(dropIdMapping.getString("id"), dropIdMapping.getString("name"));
                }

                JSONArray rarityOverrideList = dataMapsJson.getJSONArray("rarityOverrideList");
                for (int i = 0, len = rarityOverrideList.length(); i < len; i++) {
                    JSONObject rarityOverrideMapping = rarityOverrideList.getJSONObject(i);
                    DropUtils.addRarityOverrideMapping(rarityOverrideMapping.getString("id"), rarityOverrideMapping.getInt("rarity"));
                }
            }
        }
        catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing data maps.", e);
        }
    }
}
