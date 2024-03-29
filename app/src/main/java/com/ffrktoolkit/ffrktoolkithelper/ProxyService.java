package com.ffrktoolkit.ffrktoolkithelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ffrktoolkit.ffrktoolkithelper.parser.InventoryParser;
import com.ffrktoolkit.ffrktoolkithelper.util.DropUtils;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.CharsetUtil;

public class ProxyService extends Service implements View.OnTouchListener, View.OnClickListener {
    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private HttpProxyServer server;
    private String LOG_TAG = "FFRKToolkitHelper";
    private InventoryParser inventoryParser;
    private final static int PROXY_NOTIFICATION_ID = 176123744;
    private JSONObject labyrinthDataFinishChestHolder = null;
    private boolean wasLastCallToOpenChest = false;
    private List<JSONObject> buddyParts = new ArrayList<>();
    //private Context appContext;

    public ProxyService() {
        Log.d(LOG_TAG, "Service created.");
        inventoryParser = new InventoryParser();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //appContext = getBaseContext();
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();
        if (getString(R.string.intent_start_proxy).equals(action)) {
            if (server == null) {
                startProxy();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
                prefs.edit().putBoolean("enableProxy", true).commit();
            }
        } else if (getString(R.string.intent_stop_proxy).equals(action)) {
            if (this.server != null) {
                Log.d(LOG_TAG, "Stopping proxy.");
                this.server.abort();

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(ProxyService.PROXY_NOTIFICATION_ID);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
                prefs.edit().putBoolean("enableProxy", false).commit();

                Intent broadcastIntent = new Intent("com.ffrktoolkit.ffrktoolkithelper");
                Bundle extras = new Bundle();
                extras.putString("action", intent.getAction());
                broadcastIntent.putExtras(extras);
                getApplicationContext().sendBroadcast(broadcastIntent);

                this.server = null;
                this.stopSelf();
            }
        } else if (getString(R.string.intent_change_proxy_port).equals(action)) {
            if (this.server != null) {
                Log.d(LOG_TAG, "Restarting proxy with new port.");
                this.server.abort();
                startProxy();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        startProxy();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(LOG_TAG, "onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        if (this.server != null) {
            Log.d(LOG_TAG, "Stopping proxy before destroying service");
            server.abort();
            server = null;
        }
    }

    public void startProxy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d(LOG_TAG, String.valueOf(prefs.getBoolean("enableProxy", false)));
        if (!prefs.getBoolean("enableProxy", false)) {
            Log.d(LOG_TAG, "Proxy preference set to false, not starting.");
            return;
        }

        if (this.server != null) {
            Log.d(LOG_TAG, "Proxy already started, this is likely a stop request from the notification.");
            server.abort();
            server = null;
            return;
        }

        createProxyNotification();
        Log.d(LOG_TAG, "startProxy");
        int port = PreferenceManager.getDefaultSharedPreferences(this).getInt("proxyPort", 8081);

        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        try {
            this.server = DefaultHttpProxyServer.bootstrap().withPort(port)
                    .withFiltersSource(new HttpFiltersSourceAdapter() {
                        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                            return new HttpFiltersAdapter(originalRequest) {
                                @Override
                                public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                    // TODO: implement your filtering here
                                    if (isUrlForFfrk(originalRequest.getUri()) && (httpObject instanceof HttpRequest)) {
                                        Log.d(LOG_TAG, "Sending request to " + ((HttpRequest) httpObject).getUri());
                                    }

                                    return null;
                                }

                                @Override
                                public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                    Log.d(LOG_TAG, httpObject.getClass().getName());
                                    if (isUrlForFfrk(originalRequest.getUri()) && httpObject instanceof FullHttpResponse) {
                                        FullHttpResponse response = (FullHttpResponse) httpObject;
                                        Log.d(LOG_TAG, "Received response for " + originalRequest.getUri());

                                        try {
                                            URL urlPath = new URL(originalRequest.getUri());
                                            Log.d(LOG_TAG, "Response path: " + urlPath.getPath());
                                            String responseContent = response.content().toString(CharsetUtil.UTF_8);
                                            parseFfrkResponse(originalRequest, responseContent);
                                            //Log.d(LOG_TAG, responseContent);
                                        } catch (Exception e) {
                                            crashlytics.log("Exception while parsing response content.");
                                            crashlytics.recordException(e);
                                        }
                                    }

                                    return httpObject;
                                }
                            };
                        }

                        public int getMaximumResponseBufferSizeInBytes() {
                            return 10485760;
                        }
                    })
                    .start();
        } catch (Exception e) {
            crashlytics.log("Exception while trying to start proxy server.");
            crashlytics.recordException(e);
            int proxyPort = prefs.getInt("proxyPort", Integer.valueOf(getString(R.string.default_proxy_port)));
            Toast.makeText(this,  getString(R.string.error_proxy_already_started) + proxyPort, Toast.LENGTH_SHORT);
        }
    }

    private void parseFfrkResponse(HttpRequest request, String response) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String requestUri = request.getUri();
        boolean isDebugEnabled = prefs.getBoolean("enableDebug", false);
        Log.d(LOG_TAG, "Request URI: " + requestUri);

        try {
            if (requestUri.contains("/dff/party/list_buddy")) {
                Log.i(LOG_TAG, "Entering /dff/party/list_buddy");
                JSONObject json = new JSONObject(response);
                parsePartyData(requestUri, json);
            } else if (requestUri.contains("/dff/party/list_equipment")) {
                Log.i(LOG_TAG, "Entering /dff/party/list_equipment");
                JSONObject json = new JSONObject(response);
                parseInventoryData("inventory", requestUri, json);
            } else if (requestUri.contains("/dff/party/list_other")) {
                Log.i(LOG_TAG, "Entering /dff/party/list_other");
                JSONObject json = new JSONObject(response);
                parseMaterialData(requestUri, json);
            } else if (requestUri.contains("/dff/party/list")) {
                Log.i(LOG_TAG, "Entering /dff/party/list");
                JSONObject json = new JSONObject(response);
                parsePartyData(requestUri, json);
                parseInventoryData("inventory", requestUri, json);
                parseStamina(json);
            } else if (requestUri.contains("/dff/warehouse/get_equipment_list")) {
                Log.i(LOG_TAG, "Entering get_equipment_list");
                JSONObject json = new JSONObject(response);
                parseInventoryData("vault", requestUri, json);
            } else if (requestUri.contains("get_battle_init_data")) {
                Log.i(LOG_TAG, "Entering battle");
                JSONObject json = new JSONObject(response);
                parseBattleData(json);
            } else if (requestUri.contains("win_battle") || requestUri.contains("escape_battle")
                    || requestUri.contains("enter_multi") || requestUri.contains("enter_dungeon")
                    || requestUri.contains("/dff/battle/escape") || requestUri.contains("/dff/battle/win")
                    || requestUri.contains("/escape_battle") || requestUri.contains("/win_battle")
                    || requestUri.endsWith("/dungeons") || requestUri.contains("/dungeons?world_id")
                    || requestUri.endsWith("/battles") || requestUri.endsWith("/recover_stamina")) {
                Log.i(LOG_TAG, "Entering battle over");
                JSONObject json = new JSONObject(response);
                parseStamina(json);
            } else if (requestUri.endsWith("/dff/")) {
                Log.i(LOG_TAG, "Entering /dff/");
                String responseJson = StringUtils.substringBetween(response, "<script data-app-init-data type=\"application/json\">", "</script>");
                JSONObject json = new JSONObject(responseJson);
                parseStamina(json);
            } else if (requestUri.endsWith("select_painting") || requestUri.endsWith("choose_explore_painting")) {
                Log.i(LOG_TAG, "Entering select_painting/choose_explore_painting");
                JSONObject json = new JSONObject(response);
                processLabyrinthSelectExploreResponse(json);
            } else if (requestUri.endsWith("get_display_paintings")) {
                Log.i(LOG_TAG, "Entering get_display_paintings");
                JSONObject json = new JSONObject(response);
                processLabyrinthDisplayResponse(json);
            } else if (requestUri.endsWith("finish_current_painting")) {
                Log.i(LOG_TAG, "Entering finish_current_painting");
                if (wasLastCallToOpenChest) {
                    wasLastCallToOpenChest = false;
                    processLabyrinthTreasureChestResponse(labyrinthDataFinishChestHolder);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while parsing FFRK response.", e);
            Log.e(LOG_TAG, response);
            crashlytics.log("Exception while parsing FFRK response.");
            crashlytics.recordException(e);
        }

        if (isDebugEnabled) {
            Log.d(LOG_TAG, "Sending request result to ACRA.");
            ACRA.getErrorReporter().handleSilentException(null);
        }
    }

    private synchronized void parsePartyData(String requestUri, JSONObject json) throws JSONException {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        /*boolean isDebugEnabled = prefs.getBoolean("enableDebugToasts", false);
        Crashlytics.log("Parsing soulbreaks/legend materia from " + requestUri);
        if (isDebugEnabled) {
            Crashlytics.logException(new RuntimeException("Logging parsePartyData"));
        }*/

        buddyParts.add(json);
        if (StringUtils.contains(requestUri, "split")) {
            String[] getArguments = StringUtils.split(StringUtils.substring(requestUri, StringUtils.indexOf(requestUri, "?")), "=");
            boolean readNextIndex = false;
            Integer numberOfPieces = 0;
            for (int i = 0; i < getArguments.length; i++) {
                Log.d(LOG_TAG, getArguments[i]);
                if (readNextIndex) {
                    String splitSize = StringUtils.replace(getArguments[i], "&", "");
                    numberOfPieces = Integer.valueOf(splitSize);
                    break;
                }
                if (StringUtils.containsIgnoreCase(getArguments[i], "split")) {
                    readNextIndex = true;
                }
            }

            //Log.d(LOG_TAG, "Number of pieces: " + numberOfPieces);
            //Log.d(LOG_TAG, "Parts size: " + buddyParts.size());
            if (buddyParts.size() < numberOfPieces) {
                Log.d(LOG_TAG, "Only received " + buddyParts.size() + " out of " + numberOfPieces + " expected list_party responses.");
                return;
            }
        }

        JSONArray soulbreaks = new JSONArray();
        JSONArray legendMateria = new JSONArray();
        for (final JSONObject jsonPart : buddyParts) {
            JSONArray partSoulbreaks = jsonPart.getJSONArray("soul_strikes");
            for (int j = 0; j < partSoulbreaks.length(); j++) {
                soulbreaks.put(partSoulbreaks.getJSONObject(j));
            }

            JSONArray partLegendMateria = jsonPart.getJSONArray("legend_materias");
            for (int j = 0; j < partLegendMateria.length(); j++) {
                legendMateria.put(partLegendMateria.getJSONObject(j));
            }
        }

        JSONObject filteredJson = new JSONObject();
        filteredJson.put("soul_strikes", soulbreaks);
        filteredJson.put("legend_materias", legendMateria);
        buddyParts.clear();
        // Check for an existing inventory
        String existingInventory;
        try {
            String region = isGlobalUrl(requestUri) ? "global" : "japan";
            String fileName = isGlobalUrl(requestUri) ? getString(R.string.file_inventory_global_json) : getString(R.string.file_inventory_jp_json);
            File file = new File(getApplicationContext().getFilesDir(), fileName);

            if (file.exists()) {
                FileInputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);
                existingInventory = new String(data, CharsetUtil.UTF_8);
                JSONObject existingInventoryJson = new JSONObject(existingInventory);

                boolean hasInventoryChanged = inventoryParser.hasInventoryChanged(json, existingInventoryJson, region);
                if (hasInventoryChanged) {
                    prefs.edit().putBoolean("hasInventoryChanged_" + region, true).commit();
                }
            } else {
                prefs.edit().putBoolean("hasInventoryChanged_" + region, true).commit();
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing existing inventory, ignoring.", e);
            crashlytics.recordException(e);
        }

        FileOutputStream outputStream;
        try {
            String fileName = isGlobalUrl(requestUri) ? getString(R.string.file_inventory_global_json) : getString(R.string.file_inventory_jp_json);
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(filteredJson.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while writing inventory json to storage.", e);
            crashlytics.recordException(e);
            return;
        }

        Log.d(LOG_TAG, "Inventory saved to file.");
    }

    private void parseInventoryData(String inventoryType, String requestUri, JSONObject json) throws JSONException {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        /*boolean isDebugEnabled = prefs.getBoolean("enableDebugToasts", false);
        Crashlytics.log("Parsing inventory from " + requestUri);
        if (isDebugEnabled) {
            Crashlytics.logException(new RuntimeException("Logging parseInventoryData"));
        }*/

        JSONArray equipments = json.getJSONArray("equipments");
        JSONObject filteredResponse = new JSONObject();
        filteredResponse.put("equipments", equipments);

        for (int i = 0, len = equipments.length(); i < len; i++) {
            JSONObject equipment = equipments.getJSONObject(i);
            if (equipment.has("evol_max_level_of_base_rarity")) {
                equipment.remove("evol_max_level_of_base_rarity");
            }

            if (equipment.has("hyper_evolve_recipe")) {
                equipment.remove("hyper_evolve_recipe");
            }
        }

        // Check for an existing inventory
        String existingInventory;
        String fileName = null;
        if ("inventory".equals(inventoryType)) {
            fileName = isGlobalUrl(requestUri) ? getString(R.string.file_equipment_global_json) : getString(R.string.file_equipment_jp_json);
        } else if ("vault".equals(inventoryType)) {
            fileName = isGlobalUrl(requestUri) ? getString(R.string.file_vault_global_json) : getString(R.string.file_vault_jp_json);
        }

        try {
            String region = isGlobalUrl(requestUri) ? "global" : "japan";
            File file = new File(getApplicationContext().getFilesDir(), fileName);

            if (file.exists()) {
                FileInputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);
                existingInventory = new String(data, CharsetUtil.UTF_8);
                JSONObject existingInventoryJson = new JSONObject(existingInventory);

                boolean hasInventoryChanged = inventoryParser.hasEquipmentChanged(json, existingInventoryJson, region);
                if (hasInventoryChanged) {
                    prefs.edit().putBoolean("hasInventoryChanged_" + region, true).commit();
                }
            } else {
                prefs.edit().putBoolean("hasInventoryChanged_" + region, true).commit();
            }
        } catch (Exception e) {
            crashlytics.log("Exception while parsing existing inventory, ignoring.");
            crashlytics.recordException(e);
        }

        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(filteredResponse.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            crashlytics.log("Exception while writing inventory json to storage.");
            crashlytics.recordException(e);
            return;
        }

        Log.d(LOG_TAG, "Equipment inventory saved to file.");
    }

    private void parseMaterialData(String requestUri, JSONObject json) throws JSONException {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        /*boolean isDebugEnabled = prefs.getBoolean("enableDebugToasts", false);
        Crashlytics.log("Parsing material data from " + requestUri);
        if (isDebugEnabled) {
            Crashlytics.logException(new RuntimeException("Logging parseMaterialData"));
        }*/

        JSONArray materials = json.getJSONArray("materials");
        JSONObject filteredJson = new JSONObject();
        filteredJson.put("materials", materials);

        // Check for an existing inventory
        String existingInventory;
        try {
            String region = isGlobalUrl(requestUri) ? "global" : "japan";
            String fileName = isGlobalUrl(requestUri) ? getString(R.string.file_materials_global_json) : getString(R.string.file_materials_jp_json);
            File file = new File(getApplicationContext().getFilesDir(), fileName);

            if (file.exists()) {
                FileInputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);
                existingInventory = new String(data, CharsetUtil.UTF_8);
                JSONObject existingInventoryJson = new JSONObject(existingInventory);

                boolean hasInventoryChanged = inventoryParser.hasMaterialsChanged(json, existingInventoryJson, region);
                if (hasInventoryChanged && "global".equals(region)) {
                    prefs.edit().putBoolean("hasInventoryChanged_" + region, true).commit();
                }
            } else {
                if ("global".equals(region)) {
                    prefs.edit().putBoolean("hasInventoryChanged_" + region, true).commit();
                }
            }
        } catch (Exception e) {
            crashlytics.log("Exception while parsing existing inventory, ignoring.");
            crashlytics.recordException(e);
        }

        FileOutputStream outputStream;
        try {
            String fileName = isGlobalUrl(requestUri) ? getString(R.string.file_materials_global_json) : getString(R.string.file_materials_jp_json);
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(filteredJson.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            crashlytics.log("Exception while writing inventory json to storage.");
            crashlytics.recordException(e);
            return;
        }

        Log.d(LOG_TAG, "Materials saved to file.");
    }

    private void parseBattleData(JSONObject json) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        List<JSONObject> drops = new ArrayList<>();
        try {
            JSONObject battleData = json.getJSONObject("battle");
            JSONArray rounds = battleData.getJSONArray("rounds");

            for (int i = 0, roundsLen = rounds.length(); i < roundsLen; i++) {
                JSONObject round = rounds.getJSONObject(i);
                JSONArray enemies = round.getJSONArray("enemy");

                for (int j = 0, enemyLen = enemies.length(); j < enemyLen; j++) {
                    JSONObject enemy = enemies.getJSONObject(j);
                    JSONArray children = enemy.getJSONArray("children");

                    for (int k = 0, childrenLen = children.length(); k < childrenLen; k++) {
                        JSONObject child = children.getJSONObject(k);
                        JSONArray dropItemList = child.getJSONArray("drop_item_list");

                        for (int l = 0, dropsLen = dropItemList.length(); l < dropsLen; l++) {
                            JSONObject drop = dropItemList.getJSONObject(l);
                            drop.put("rarity", DropUtils.overrideRarity(drop.optString("item_id"), drop.optInt("rarity")));
                            drops.add(drop);
                        }
                    }
                }
            }

            Collections.sort(drops, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject d1, JSONObject d2) {
                    String r1 = d1.optString("rarity");
                    String r2 = d2.optString("rarity");

                    try {
                        int rarity1 = Integer.parseInt(r1);
                        int rarity2 = Integer.parseInt(r2);

                        return (rarity1 - rarity2) * -1;
                    } catch (Exception e) {
                        return 1;
                    }
                }
            });

            Log.d(LOG_TAG, "Drop list size " + drops.size());
            ArrayList<String> dropTexts = getDropsString(drops);
            Intent dropsIntent = new Intent(this.getApplicationContext(), OverlayService.class);
            dropsIntent.putStringArrayListExtra("drops", dropTexts);
            getApplicationContext().startService(dropsIntent);
        } catch (Exception e) {
            crashlytics.log("Exception while parsing battle data.");
            crashlytics.recordException(e);
        }
    }

    private ArrayList<String> getDropsString(List<JSONObject> drops) throws JSONException {
        Log.d(LOG_TAG, "Starting drop parsing");
        Map<String, Integer> dropMap = new LinkedHashMap<>();
        for (JSONObject drop : drops) {
            String itemId = drop.optString("item_id");
            String rarity = drop.optString("rarity");
            int type = drop.optInt("type");
            Integer quantity = null;
            String mapKey = null;
            if (itemId != null && !"".equals(itemId.trim())) {
                Log.d(LOG_TAG, "Item ID: " + itemId);
                String dropName = DropUtils.getDropName(itemId);
                mapKey = rarity + "\u2605 " + dropName;
                quantity = dropMap.get(mapKey);
            } else {
                String stringType = String.valueOf(type);

                if (type == 11) {
                    stringType = "Gil";
                }

                mapKey = stringType;
                quantity = dropMap.get(mapKey);
            }

            if (quantity == null) {
                quantity = 0;
            }

            String quantityInDrop = drop.optString("num");
            if (quantityInDrop != null && !"".equals(quantityInDrop.trim())) {
                quantity += Integer.parseInt(quantityInDrop);
            }

            String amountInDrop = drop.optString("amount"); // used for Gil
            if (amountInDrop != null && !"".equals(amountInDrop.trim())) {
                quantity += Integer.parseInt(amountInDrop);
            }

            Log.d(LOG_TAG, "Parsed " + mapKey + " quan " + quantity);
            dropMap.put(mapKey, quantity);
        }

        ArrayList<String> dropsText = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dropMap.entrySet()) {
            String dropName = entry.getKey();
            Integer quantity = entry.getValue();

            String dropText = null;
            if (dropName.equalsIgnoreCase("gil")) {
                dropText = quantity + " " + dropName;
            } else {
                dropText = dropName + " (" + quantity + ")";
            }

            dropsText.add(dropText);
            Log.d(LOG_TAG, "Parsed drop: " + dropText);
        }


        return dropsText;
    }

    private ArrayList<String> getChestString(List<JSONObject> drops) throws JSONException {
        Log.d(LOG_TAG, "Starting chest parsing");

        ArrayList<String> chestTexts = new ArrayList<>();
        for (int i = 0; i < drops.size(); i++) {
            JSONObject drop = drops.get(i);
            String itemId = drop.getString("item_id");
            String dropName = "" + i + ". ";
            String itemType = "";
            if (StringUtils.startsWith(itemId, "5")) {
                itemType = "Hero Artifact";
            }
            else if (StringUtils.startsWith(itemId, "4")) {
                itemType = "Chance for Anima Lense/Bookmark"; // Unconfirmed
            }
            else if (StringUtils.startsWith(itemId, "3")) {
                itemType = "Chance for 6* Mote/Magic Key";
            }
            else if (StringUtils.startsWith(itemId, "2")) {
                itemType = "Chance for 6* Crystal/Rainbow Crystal";
            }
            else if (StringUtils.startsWith(itemId, "1")) {
                itemType = "Chance for 5* Orb/5* Mote";
            }
            else {
                dropName = DropUtils.getDropName(itemId);
            }

            String dropText = dropName + itemType;
            chestTexts.add(dropText);
            Log.d(LOG_TAG, "Parsed chest: " + dropText);
        }

        return chestTexts;
    }

    private void createProxyNotification() {
        Intent intent = new Intent(getApplicationContext(), ProxyService.class);
        intent.setAction(getString(R.string.intent_stop_proxy));

        int random = (int) System.nanoTime();
        PendingIntent stopProxyIntent = PendingIntent.getService(
                getApplicationContext(),
                random,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        random = (int) System.nanoTime();
        Intent overlayIntent = new Intent(getApplicationContext(), OverlayService.class);
        overlayIntent.setAction("showOverlay");
        PendingIntent startOverlayIntent = PendingIntent.getService(
                getApplicationContext(),
                random,
                overlayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        random = (int) System.nanoTime();
        Intent settingsIntent = new Intent(getApplicationContext(), FFRKToolkitHelperActivity.class);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                random,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder notificationBuilder = new Notification.Builder(this.getApplicationContext())
                .setSmallIcon(R.drawable.ic_proxy_notification_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_proxy_enabled))
                .setContentIntent(settingsPendingIntent)
                .addAction(R.drawable.ic_stop_black_24dp, getString(R.string.notification_stop_proxy), stopProxyIntent)
                .addAction(R.drawable.ic_show_overlay_black_24dp, getString(R.string.pref_title_enable_overlay), startOverlayIntent)
                .setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(LOG_TAG);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(LOG_TAG, LOG_TAG, importance);
            notificationManager.createNotificationChannel(channel);
        }

        //notificationManager.notify(ProxyService.PROXY_NOTIFICATION_ID, notificationBuilder.build());
        startForeground(ProxyService.PROXY_NOTIFICATION_ID, notificationBuilder.build());
    }

    private boolean isUrlForFfrk(String url) {
        return url != null && ((url.contains("ffrk.denagames.com")) || (url.contains("dff.sp.mbga.jp")));
    }

    private boolean isGlobalUrl(String url) {
        return url != null && url.contains("ffrk.denagames.com");
    }

    private void parseStamina(JSONObject json) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        final boolean isStaminaNotificationEnabled = sharedPreferences.getBoolean("enableStaminaTracker", false);
        try {
            Log.d(LOG_TAG, "Inside parse stamina");
            JSONObject user = json.optJSONObject("user");
            if (user != null) {
                Log.d(LOG_TAG, "User found");
                //JSONObject staminaInfo = user.getJSONObject("stamina_info");
                int recoveryTime = user.optInt("stamina_recovery_time");
                int recoveryTimeRemaining = user.optInt("stamina_recovery_remaining_time");
                int maxStamina = user.getInt("max_stamina");
                int stamina = user.getInt("stamina");
                long serverTime = json.optLong("SERVER_TIME");

                if (serverTime == 0) {
                    serverTime = json.optLong("current_time");
                }

                if (serverTime == 0) {
                    return;
                }

                prefsEditor.putLong("serverTime", serverTime);
                prefsEditor.putInt("currentStamina", stamina);
                prefsEditor.putInt("maxStamina", maxStamina);
                prefsEditor.putInt("staminaRecoveryRemainingTime", recoveryTimeRemaining);
                prefsEditor.putInt("staminaRecoveryTime", recoveryTime);
                prefsEditor.commit();

                if (isStaminaNotificationEnabled) {
                    Intent staminaNotification = new Intent(getApplicationContext(), StaminaService.class);
                    staminaNotification.setAction(getString(R.string.intent_update_stamina));
                    startService(staminaNotification);
                }
            }

            return;
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing stamina from JSON.", e);
            crashlytics.recordException(e);
        }
    }

    private void processLabyrinthSelectExploreResponse(JSONObject json) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        List<JSONObject> drops = new ArrayList<>();

        try {
            Log.d(LOG_TAG, "Inside processLabyrinthSelectExploreResponse");

            JSONObject labyrinthDungeonSession = json.optJSONObject("labyrinth_dungeon_session");
            if (labyrinthDungeonSession == null) {
                return;
            }

            Integer paintingStatus = labyrinthDungeonSession.optInt("current_painting_status");
            if (paintingStatus == null) {
                return;
            }

            Log.d(LOG_TAG, "Current painting status: " + paintingStatus);
            if (paintingStatus == 3) {
                drops.addAll(parseLabyrinthRootBattles(labyrinthDungeonSession));
            }
            else if (paintingStatus == 2 || paintingStatus == 4) {
                drops.addAll(parseLabyrinthChests(labyrinthDungeonSession));
            }
            else if (paintingStatus == 1) {
                // NOP
            }
            else {
                drops.addAll(parseNestedBattles(labyrinthDungeonSession));
            }

            Log.d(LOG_TAG, "Text list size " + drops.size());
            ArrayList<String> dropTexts = getChestString(drops);
            Intent dropsIntent = new Intent(this.getApplicationContext(), OverlayService.class);
            dropsIntent.putStringArrayListExtra("drops", dropTexts);
            getApplicationContext().startService(dropsIntent);
        }
        catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing labyrinth select/explore response from JSON.", e);
            crashlytics.recordException(e);
        }
    }

    private void processLabyrinthTreasureChestResponse(JSONObject labyrinthDungeonSession) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        List<JSONObject> drops = new ArrayList<>();

        if (labyrinthDataFinishChestHolder == null) {
            return;
        }

        labyrinthDataFinishChestHolder = null;
        try {
            Log.d(LOG_TAG, "Inside processLabyrinthTreasureChestResponse");

            Integer paintingStatus = labyrinthDungeonSession.optInt("current_painting_status");
            if (paintingStatus == null) {
                return;
            }

            Log.d(LOG_TAG, "Current painting status: " + paintingStatus);
            drops.addAll(parseNestedBattles(labyrinthDungeonSession));

            Log.d(LOG_TAG, "Text list size " + drops.size());
            ArrayList<String> dropTexts = getChestString(drops);
            Intent dropsIntent = new Intent(this.getApplicationContext(), OverlayService.class);
            dropsIntent.putStringArrayListExtra("drops", dropTexts);
            getApplicationContext().startService(dropsIntent);
        }
        catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing labyrinth chests from JSON.", e);
            crashlytics.recordException(e);
        }
    }

    private void processLabyrinthDisplayResponse(JSONObject json) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        List<JSONObject> drops = new ArrayList<>();

        try {
            Log.d(LOG_TAG, "Inside processLabyrinthDisplayResponse");
            JSONObject labyrinthDungeonSession = json.optJSONObject("labyrinth_dungeon_session");
            if (labyrinthDungeonSession == null) {
                return;
            }

            Integer paintingStatus = labyrinthDungeonSession.optInt("current_painting_status");
            if (paintingStatus == null) {
                return;
            }

            Log.d(LOG_TAG, "Current painting status: " + paintingStatus);
            if (paintingStatus == 3) {
                drops.addAll(parseLabyrinthRootBattles(labyrinthDungeonSession));
            }
            else if (paintingStatus == 2 || paintingStatus == 4) {
                drops.addAll(parseLabyrinthChests(labyrinthDungeonSession));
            }
            else {
                drops.addAll(parseNestedBattles(labyrinthDungeonSession));
            }

            Log.d(LOG_TAG, "Text list size " + drops.size());
            ArrayList<String> dropTexts = getChestString(drops);
            Intent dropsIntent = new Intent(this.getApplicationContext(), OverlayService.class);
            dropsIntent.putStringArrayListExtra("drops", dropTexts);
            getApplicationContext().startService(dropsIntent);
        }
        catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing labyrinth chests from JSON.", e);
            crashlytics.recordException(e);
        }
    }

    /*
    private void parseLabyrinthBattles(JSONObject json) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        List<JSONObject> drops = new ArrayList<>();

        try {
            Log.d(LOG_TAG, "Inside parseLabyrinthBattles");
            JSONObject labyrinthDungeonSession = json.optJSONObject("labyrinth_dungeon_session");
            if (labyrinthDungeonSession == null) {
                return;
            }

            drops.addAll(parsePaintingBattles(labyrinthDungeonSession));

            JSONArray treasureChestIds = labyrinthDungeonSession.optJSONArray("treasure_chest_ids");
            if (treasureChestIds != null && treasureChestIds.length() != 0) {
                JSONObject header = new JSONObject();
                header.put("item_id", "Chest contents (left to right)");
                drops.add(header);
                for (int i = 0, treasureLen = treasureChestIds.length(); i < treasureLen; i++) {
                    Integer treasureId = treasureChestIds.getInt(i);
                    JSONObject drop = new JSONObject();
                    drop.put("item_id", String.valueOf(treasureId));
                    //drop.put("rarity", DropUtils.overrideRarity(String.valueOf(treasureId)));
                    drops.add(drop);
                }
            }

            if (drops.size() == 0) {
                Log.d(LOG_TAG, "No chest or explore data");
                return;
            }

            Log.d(LOG_TAG, "Battle list size " + drops.size());
            ArrayList<String> dropTexts = getChestString(drops);
            Intent dropsIntent = new Intent(this.getApplicationContext(), OverlayService.class);
            dropsIntent.putStringArrayListExtra("drops", dropTexts);
            getApplicationContext().startService(dropsIntent);
        }
        catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing labyrinth battles from JSON.", e);
            crashlytics.recordException(e);
        }
    }*/

    /*
    private void parseLabyrinthChestsOLD(JSONObject json) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        List<JSONObject> drops = new ArrayList<>();

        try {
            Log.d(LOG_TAG, "Inside parseLabyrinthChests");
            JSONObject labyrinthDungeonSession = json.optJSONObject("labyrinth_dungeon_session");
            if (labyrinthDungeonSession == null) {
                return;
            }

            JSONArray treasureChestIds = labyrinthDungeonSession.optJSONArray("treasure_chest_ids");
            JSONObject explorePaintingEvent = labyrinthDungeonSession.optJSONObject("explore_painting_event");
            //if ((treasureChestIds == null || treasureChestIds.length() == 0)
                    //&& explorePaintingEvent == null) {
            //if (treasureChestIds == null || treasureChestIds.length() == 0) {
            //    Log.d(LOG_TAG, "No chest or explore data");
            //    return;
            //}

            drops.addAll(parsePaintingBattles(labyrinthDungeonSession));
            if (drops.size() == 0) {
                drops.addAll(parsePaintings(labyrinthDungeonSession));
            }

            if (treasureChestIds != null && treasureChestIds.length() != 0) {
                JSONObject header = new JSONObject();
                header.put("item_id", "Chest contents (left to right)");
                drops.add(header);
                for (int i = 0, treasureLen = treasureChestIds.length(); i < treasureLen; i++) {
                    Integer treasureId = treasureChestIds.getInt(i);
                    JSONObject drop = new JSONObject();
                    drop.put("item_id", String.valueOf(treasureId));
                    //drop.put("rarity", DropUtils.overrideRarity(String.valueOf(treasureId)));
                    drops.add(drop);
                }
            }

            if (drops.size() == 0) {
                Log.d(LOG_TAG, "No chest or explore data");
                //return;
            }

            //////
            if (explorePaintingEvent != null) {
                JSONObject id = new JSONObject();
                id.put("item_id", String.valueOf(explorePaintingEvent.getInt("id")));
                drops.add(id);

                JSONObject type = new JSONObject();
                type.put("item_id", String.valueOf(explorePaintingEvent.getInt("type")));
                drops.add(type);

                JSONObject textMasterId = new JSONObject();
                textMasterId.put("item_id", explorePaintingEvent.getString("text_master_id"));
                drops.add(textMasterId);

                JSONObject subTextMaster1 = new JSONObject();
                subTextMaster1.put("item_id", explorePaintingEvent.getString("sub_text_master_id_1"));
                drops.add(subTextMaster1);

                JSONObject subTextMaster2 = new JSONObject();
                subTextMaster2.put("item_id", explorePaintingEvent.getString("sub_text_master_id_2"));
                drops.add(subTextMaster2);

                JSONObject subTextMaster3 = new JSONObject();
                subTextMaster3.put("item_id", explorePaintingEvent.getString("sub_text_master_id_3"));
                drops.add(subTextMaster3);
            }
            //////
            Log.d(LOG_TAG, "Chest list size " + drops.size());
            ArrayList<String> dropTexts = getChestString(drops);
            Intent dropsIntent = new Intent(this.getApplicationContext(), OverlayService.class);
            dropsIntent.putStringArrayListExtra("drops", dropTexts);
            getApplicationContext().startService(dropsIntent);
        }
        catch (Exception e) {
            Log.w(LOG_TAG, "Exception while parsing labyrinth chests from JSON.", e);
            crashlytics.recordException(e);
        }
    }
    */

    private ArrayList<JSONObject> parseLabyrinthRootBattles(JSONObject labyrinthDungeonSession) throws JSONException {
        ArrayList<JSONObject> paintingResults = new ArrayList<>();
        JSONObject dungeon = labyrinthDungeonSession.optJSONObject("dungeon");
        if (dungeon != null) {
            Log.d(LOG_TAG, "Inside dungeon");
            JSONArray dungeonCaptures = dungeon.optJSONArray("captures");
            if (dungeonCaptures != null) {
                Log.d(LOG_TAG, "Inside dungeon capture");
                for (int i = 0; i < dungeonCaptures.length(); i++) {
                    JSONObject dungeonCapture = dungeonCaptures.optJSONObject(i);
                    if (dungeonCapture != null) {
                        JSONObject tipBattle = dungeonCapture.optJSONObject("tip_battle");
                        if (tipBattle != null) {
                            Log.d(LOG_TAG, "Inside tip battle, title: " + i + tipBattle.optString("title"));
                            JSONObject paintingResult = new JSONObject();
                            paintingResult.put("item_id", "Forced battle: " + tipBattle.optString("title"));
                            paintingResults.add(paintingResult);
                        }
                    }
                }
            }
        }
        return paintingResults;
    }

    private ArrayList<JSONObject> parseNestedBattles(JSONObject labyrinthDungeonSession) throws JSONException {
        ArrayList<JSONObject> paintingResults = new ArrayList<>();
        JSONArray displayPaintings = labyrinthDungeonSession.optJSONArray("display_paintings");
        if (displayPaintings != null) {
            Log.d(LOG_TAG, "Inside display paintings");
            for (int i = 0, length = displayPaintings.length(); i < length; i++) {
                Log.d(LOG_TAG, "Inside display paintings loop: " + i);
                if (i > 2) {
                    break;
                }

                JSONObject painting = displayPaintings.getJSONObject(i);
                if (painting == null) {
                    Log.d(LOG_TAG, "Inside display paintings loop: painting null");
                    continue;
                }

                Log.d(LOG_TAG, "Painting ID: " + painting.optInt("painting_id"));
                JSONObject dungeon = painting.optJSONObject("dungeon");
                if (dungeon == null) {
                    Log.d(LOG_TAG, "Inside display paintings loop: dungeon null");
                    continue;
                }

                JSONArray dungeonCaptures = dungeon.optJSONArray("captures");
                if (dungeonCaptures == null) {
                    Log.d(LOG_TAG, "Inside display paintings loop: captures null");
                    continue;
                }
                for (int j = 0; j < dungeonCaptures.length(); j++) {
                    JSONObject dungeonCapture = dungeonCaptures.optJSONObject(j);
                    if (dungeonCapture == null) {
                        continue;
                    }

                    JSONObject tipBattle = dungeonCapture.optJSONObject("tip_battle");
                    if (tipBattle == null) {
                        Log.d(LOG_TAG, "Inside display paintings loop: tip_battle");
                        continue;
                    }

                    Log.d(LOG_TAG, "Inside display paintings loop, canvas title: " + tipBattle.optString("title"));
                    JSONObject paintingResult = new JSONObject();
                    paintingResult.put("item_id", "Canvas " + (i + 1) + ": " + tipBattle.optString("title"));
                    paintingResults.add(paintingResult);
                }
            }
        }
        return paintingResults;
    }

    private ArrayList<JSONObject> parseLabyrinthChests(JSONObject labyrinthDungeonSession) throws JSONException {
        ArrayList<JSONObject> drops = new ArrayList<>();
        Log.d(LOG_TAG, "Inside parseLabyrinthChests");

        JSONArray treasureChestIds = labyrinthDungeonSession.optJSONArray("treasure_chest_ids");
        if (treasureChestIds != null && treasureChestIds.length() != 0) {
            JSONObject header = new JSONObject();
            header.put("item_id", "Chest contents (left to right)");
            drops.add(header);
            for (int i = 0, treasureLen = treasureChestIds.length(); i < treasureLen; i++) {
                Integer treasureId = treasureChestIds.getInt(i);
                JSONObject drop = new JSONObject();
                drop.put("item_id", String.valueOf(treasureId));
                //drop.put("rarity", DropUtils.overrideRarity(String.valueOf(treasureId)));
                drops.add(drop);
            }
        }

        wasLastCallToOpenChest = true;
        labyrinthDataFinishChestHolder = labyrinthDungeonSession;
        return drops;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ProxyService getService() {
            // Return this instance of ProxyService so clients can call public methods
            return ProxyService.this;
        }
    }

    //Use this method to show toast
    /*private void showToast(final String toastMessage) {
        if (null != appContext) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(appContext, toastMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }*/
}
