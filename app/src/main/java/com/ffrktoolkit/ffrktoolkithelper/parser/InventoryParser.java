package com.ffrktoolkit.ffrktoolkithelper.parser;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class InventoryParser {

    private String LOG_TAG = "FFRKToolkitHelper";

    public JSONObject parseJsonToInventoryFormat(String inventoryJson, JSONObject existingInventory, String region) {
        try {
            JSONObject importingInventory = new JSONObject(inventoryJson);
            JSONArray soulbreaks = importingInventory.optJSONArray("soul_strikes");
            JSONArray legendMaterias = importingInventory.optJSONArray("legend_materias");

            // Update soulbreaks
            JSONArray newSoulbreaks = new JSONArray();
            if (soulbreaks != null) {
                for (int i = 0; i < soulbreaks.length(); i++) {
                    JSONObject soulbreak = soulbreaks.getJSONObject(i);
                    Object sbId = soulbreak.opt("id");
                    String sbType = soulbreak.optString("soul_strike_category_name");
                    if (sbId != null && !"STANDARD".equalsIgnoreCase(sbType)) {
                        JSONObject newSb = new JSONObject();
                        newSb.put("id", sbId);
                        newSb.put("region", region);
                        newSb.put("isDenaId", true);
                        newSoulbreaks.put(newSb);
                    }
                }

                if (newSoulbreaks.length() > 0) {
                    existingInventory.put("soulbreaks", newSoulbreaks);
                }
            }

            JSONArray newLegendMaterias = new JSONArray();
            if (legendMaterias != null) {
                for (int i = 0; i < legendMaterias.length(); i++) {
                    JSONObject legendMateria = legendMaterias.getJSONObject(i);
                    Object lmId = legendMateria.opt("id");
                    if (lmId != null) {
                        JSONObject newLm = new JSONObject();
                        newLm.put("id", lmId);
                        newLm.put("region", region);
                        newLm.put("isDenaId", true);
                        newLegendMaterias.put(newLm);
                    }
                }

                if (newLegendMaterias.length() > 0) {
                    existingInventory.put("legendMateria", newLegendMaterias);
                }
            }
        }
        catch (JSONException e) {
            Log.w(LOG_TAG,"Exception while parsing and updating inventory.", e);
        }

        return existingInventory;
    }

    public boolean hasInventoryChanged(JSONObject importingInventory, JSONObject existingInventory, String region) {
        try {
            JSONArray soulbreaks = importingInventory.optJSONArray("soul_strikes");
            JSONArray existingSoulbreaks = existingInventory.optJSONArray("soul_strikes");
            JSONArray legendMaterias = importingInventory.optJSONArray("legend_materias");
            JSONArray existingLegendMaterias = existingInventory.optJSONArray("legend_materias");

            if (soulbreaks != null && existingSoulbreaks != null) {
                for (int i = 0; i < soulbreaks.length(); i++) {
                    JSONObject soulbreak = soulbreaks.getJSONObject(i);
                    Object sbId = soulbreak.opt("id");
                    String sbType = soulbreak.optString("soul_strike_category_name");
                    if (sbId != null && !"STANDARD".equalsIgnoreCase(sbType)) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < existingSoulbreaks.length(); j++) {
                            JSONObject existingSoulbreak = existingSoulbreaks.getJSONObject(j);
                            Object existingSbId = existingSoulbreak.opt("id");
                            if (existingSbId != null && existingSbId.equals(sbId)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            return true;
                        }
                    }
                }
            }
            else if ((soulbreaks != null && existingSoulbreaks == null)
                        || soulbreaks == null && existingSoulbreaks != null) {
                return true;
            }

            if (legendMaterias != null && existingLegendMaterias != null) {
                for (int i = 0; i < legendMaterias.length(); i++) {
                    JSONObject lm = legendMaterias.getJSONObject(i);
                    Object lmId = lm.opt("id");
                    if (lmId != null) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < existingLegendMaterias.length(); j++) {
                            JSONObject existingLm = existingLegendMaterias.getJSONObject(j);
                            Object existingLmId = existingLm.opt("id");
                            if (existingLmId != null && existingLmId.equals(lmId)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            return true;
                        }
                    }
                }
            }
            else if ((legendMaterias != null && existingLegendMaterias == null)
                    || legendMaterias == null && existingLegendMaterias != null) {
                return true;
            }
        }
        catch (JSONException e) {
            Log.w(LOG_TAG,"Exception while checking inventory for changes.", e);
        }

        return false;
    }

    public boolean hasEquipmentChanged(JSONObject importingInventory, JSONObject existingInventory, String region) {
        try {
            JSONArray equipments = importingInventory.optJSONArray("equipments");
            JSONArray existingEquipments = existingInventory.optJSONArray("equipments");

            if (equipments != null && existingEquipments != null) {
                for (int i = 0; i < equipments.length(); i++) {
                    JSONObject equipment = equipments.getJSONObject(i);
                    Object relicId = equipment.opt("equipment_id");
                    if (relicId != null) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < existingEquipments.length(); j++) {
                            JSONObject existingEquipment = existingEquipments.getJSONObject(j);
                            Object existingRelicId = existingEquipment.opt("equipment_id");
                            if (existingRelicId != null && existingRelicId.equals(relicId)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            return true;
                        }
                    }
                }

                for (int i = 0; i < existingEquipments.length(); i++) {
                    JSONObject existingEquipment = existingEquipments.getJSONObject(i);
                    Object existingRelicId = existingEquipment.opt("equipment_id");
                    if (existingRelicId != null) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < equipments.length(); j++) {
                            JSONObject equipment = equipments.getJSONObject(j);
                            Object relicId = equipment.opt("equipment_id");
                            if (existingRelicId.equals(relicId)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            return true;
                        }
                    }
                }
            }
            else if ((equipments != null && existingEquipments == null)
                    || equipments == null && existingEquipments != null) {
                return true;
            }
        }
        catch (JSONException e) {
            Log.w(LOG_TAG,"Exception while checking relic inventory for changes.", e);
        }

        return false;
    }
}
