package com.ffrktoolkit.ffrktoolkithelper.fragments;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ffrktoolkit.ffrktoolkithelper.DropOverlayService;
import com.ffrktoolkit.ffrktoolkithelper.R;
import com.ffrktoolkit.ffrktoolkithelper.SettingsActivity;
import com.ffrktoolkit.ffrktoolkithelper.parser.InventoryParser;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.CharsetUtil;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ProxyAndDataFragment extends PreferenceFragment {

    private String LOG_TAG = "FFRKToolkitHelper";
    private int RC_SIGN_IN = 5000;
    private InventoryParser parser = new InventoryParser();
    private BroadcastReceiver broadcastReceiver;
    private AtomicInteger updatesDone = new AtomicInteger(0);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver();
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(true);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("enable_switch"));

        Preference submitInventoryButton = findPreference(getString(R.string.pref_id_submit_inventory));
        submitInventoryButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Preference sendInventoryButton = findPreference(getString(R.string.pref_id_submit_inventory));
                sendInventoryButton.setEnabled(false);

                processInventoryData("global");
                return true;
            }
        });

        Preference googleLoginButton = findPreference(getString(R.string.pref_id_google_login));
        googleLoginButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Build a GoogleSignInClient with the options specified by gso.
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
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }

                return true;
            }
        });

        checkIfAlreadySignedIn();
        checkIfInventoryChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkIfAlreadySignedIn();
        checkIfInventoryChanged();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isProxyEnabled = prefs.getBoolean("enableProxy", false);

        Preference proxySwitch = findPreference(getString(R.string.pref_id_enable_proxy));
        ((TwoStatePreference) proxySwitch).setChecked(isProxyEnabled);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkIfAlreadySignedIn();
        checkIfInventoryChanged();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isProxyEnabled = prefs.getBoolean("enableProxy", false);

        Preference proxySwitch = findPreference(getString(R.string.pref_id_enable_proxy));
        ((TwoStatePreference) proxySwitch).setChecked(isProxyEnabled);
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

    /*@Override
    public void onReceive(Context context, Intent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
        String log = sb.toString();
        Log.d(LOG_TAG, log);

        TwoStatePreference sendInventoryPref = (TwoStatePreference) findPreference(getString(R.string.pref_id_enable_proxy));
        if (getString(R.string.intent_stop_proxy).equals(intent.getAction())) {
            sendInventoryPref.setChecked(false);
        }
        else if (getString(R.string.intent_start_proxy).equals(intent.getAction())) {
            sendInventoryPref.setChecked(true);
        }
    }*/

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            Preference loginPref = findPreference(getString(R.string.pref_id_google_login));
            loginPref.setTitle(R.string.pref_title_google_logout);
            loginPref.setSummary(getString(R.string.pref_description_google_logout) + " " + account.getEmail());
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(LOG_TAG, "signInResult:failed code=" + e.getStatusCode());
            Log.w(LOG_TAG, e.getMessage());
        }
    }

    private void checkIfInventoryChanged() {
        Preference sendInventoryPref = findPreference(getString(R.string.pref_id_submit_inventory));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!prefs.contains("hasInventoryChanged")) {
            sendInventoryPref.setIcon(R.drawable.ic_clear_black_24dp);
        }
        else if (prefs.getBoolean("hasInventoryChanged", false)) {
            sendInventoryPref.setIcon(R.drawable.ic_update_black_24dp);
        }
        else {
            sendInventoryPref.setIcon(R.drawable.ic_thumb_up_black_24dp);
        }
    }

    private void checkIfAlreadySignedIn() {
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), getGoogleSignInOptions());
        googleSignInClient.silentSignIn().addOnCompleteListener(getActivity(), new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                handleSignInResult(task);
            }
        });

        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext());

        /*Preference loginPref = findPreference(getString(R.string.pref_id_google_login));
        if (account != null) {
            loginPref.setTitle(R.string.pref_title_google_logout);
            loginPref.setSummary(getString(R.string.pref_description_google_logout) + " " + account.getEmail());
        }
        else {
            loginPref.setTitle(R.string.pref_title_google_login);
            loginPref.setSummary(R.string.pref_description_google_login);
        }*/

    }

    private void updateLoginUi() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext());

        Preference loginPref = findPreference(getString(R.string.pref_id_google_login));
        if (account != null) {
            loginPref.setTitle(R.string.pref_title_google_logout);
            loginPref.setSummary(getString(R.string.pref_description_google_logout) + " " + account.getEmail());
        }
        else {
            loginPref.setTitle(R.string.pref_title_google_login);
            loginPref.setSummary(R.string.pref_description_google_login);
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
                try {
                    if (getString(R.string.intent_stop_proxy).equals(action)) {
                        TwoStatePreference proxySwitch = (TwoStatePreference) findPreference(getString(R.string.pref_id_enable_proxy));
                        proxySwitch.setChecked(false);
                    }
                    else if (getString(R.string.intent_start_proxy).equals(action)) {
                        TwoStatePreference proxySwitch = (TwoStatePreference) findPreference(getString(R.string.pref_id_enable_proxy));
                        proxySwitch.setChecked(true);
                    }
                }
                catch (Exception e) {
                    Log.d(LOG_TAG, "Exception while trying to flip proxy switch.", e);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.ffrktoolkit.ffrktoolkithelper");
        //intentFilter.addAction(getString(R.string.intent_stop_proxy));
        //intentFilter.addAction(getString(R.string.intent_start_proxy));
        this.getActivity().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        Boolean preferenceValue = PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getBoolean("enableProxy", false);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, preferenceValue);
    }
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                //preference.setSummary(stringValue);
            }

            if ("enable_switch".equals(preference.getKey())) {
                Boolean toggle = (Boolean) value;
                Intent intent = new Intent(preference.getContext(), DropOverlayService.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                if (toggle != null && toggle) {
                    intent.setAction(preference.getContext().getString(R.string.intent_start_proxy));
                    prefs.edit().putBoolean("enableProxy", true).commit();
                }
                else {
                    intent.setAction(preference.getContext().getString(R.string.intent_stop_proxy));
                    prefs.edit().putBoolean("enableProxy", false).commit();
                }

                preference.getContext().startService(intent);
            }

            return true;
        }
    };

    private void processInventoryData(final String region) {
        String fileName = "global".equals(region) ? getString(R.string.file_inventory_global_json) : getString(R.string.file_inventory_jp_json);
        File inventoryFile = new File(getActivity().getApplicationContext().getFilesDir(), fileName);

        String relicFileName = "global".equals(region) ? getString(R.string.file_equipment_global_json) : getString(R.string.file_equipment_jp_json);
        File relicFile = new File(getActivity().getApplicationContext().getFilesDir(), relicFileName);

        String vaultFileName = "global".equals(region) ? getString(R.string.file_vault_global_json) : getString(R.string.file_vault_jp_json);
        File vaultFile = new File(getActivity().getApplicationContext().getFilesDir(), vaultFileName);

        if ((!inventoryFile.exists() || inventoryFile.length() <= 0) && (!relicFile.exists() || relicFile.length() <= 0)
                && (!vaultFile.exists() || vaultFile.length() <= 0)) {
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
                    JSONArray inventoryJson = new JSONArray(relicJson);
                    for (int i = 0, len = inventoryJson.length(); i < len; i++) {
                        relics.put(inventoryJson.get(i));
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
                    JSONArray inventoryJson = new JSONArray(relicJson);
                    for (int i = 0, len = inventoryJson.length(); i < len; i++) {
                        inventoryJson.getJSONObject(i).put("isVaulted", true);
                        relics.put(inventoryJson.get(i));
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
                                json = new JSONObject(response);
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
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        //progress.dismiss();
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

    private void callSaveRelicInventory(final GoogleSignInAccount account, final JSONObject relicInventory, String region, final ProgressDialog progress) {
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
                    params.put("function", "importRelicJson");
                    params.put("network", "Android");
                    params.put("uid", account.getEmail());
                    params.put("token", account.getIdToken());
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

    private void hideProgressWhenComplete(ProgressDialog progress) {
        int currentFinished = updatesDone.incrementAndGet();
        if (currentFinished == 3) {
            progress.dismiss();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.edit().putBoolean("hasInventoryChanged", false).commit();
            checkIfInventoryChanged();
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_inventory_saved_successfully), Toast.LENGTH_SHORT);
            toast.show();

            Preference sendInventoryButton = findPreference(getString(R.string.pref_id_submit_inventory));
            sendInventoryButton.setEnabled(true);
        }
    }
}
