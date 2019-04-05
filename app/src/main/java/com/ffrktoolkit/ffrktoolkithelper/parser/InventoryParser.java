package com.ffrktoolkit.ffrktoolkithelper.parser;

import android.util.Log;

import com.ffrktoolkit.ffrktoolkithelper.util.DropUtils;
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

                JSONArray existingSoulbreaks = existingInventory.optJSONArray("soulbreaks");
                if (existingSoulbreaks != null) {
                    for (int i = 0; i < existingSoulbreaks.length(); i++) {
                        JSONObject existingSoulbreak = existingSoulbreaks.getJSONObject(i);
                        Object sbId = existingSoulbreak.opt("id");
                        String existingRegion = existingSoulbreak.optString("region");
                        if (existingRegion == null) {
                            existingRegion = "global";
                            existingSoulbreak.put("region", existingRegion);
                        }

                        if (sbId != null && !existingRegion.equalsIgnoreCase(region)) {
                            newSoulbreaks.put(existingSoulbreak);
                        }
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

                JSONArray existingLms = existingInventory.optJSONArray("legendMateria");
                if (existingLms != null) {
                    for (int i = 0; i < existingLms.length(); i++) {
                        JSONObject existingLm = existingLms.getJSONObject(i);
                        Object lmId = existingLm.opt("id");
                        String existingRegion = existingLm.optString("region");
                        if (existingRegion == null) {
                            existingRegion = "global";
                            existingLm.put("region", existingRegion);
                        }

                        if (lmId != null && !existingRegion.equalsIgnoreCase(region)) {
                            newLegendMaterias.put(existingLm);
                        }
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

    public JSONObject parseOrbInventoryToJson(JSONObject inventoryJson) {
        JSONObject result = new JSONObject();
        JSONArray orbArray = inventoryJson.optJSONArray("materials");
        if (orbArray != null) {
            try {
                for (int i = 0, len = orbArray.length(); i <len; i++) {
                    JSONObject orb = orbArray.getJSONObject(i);
                    String dropName = DropUtils.getDropName(String.valueOf(orb.getInt("id")));

                    if (dropName != null && dropName.length() != 0 && (dropName.contains("Orb") || dropName.contains("Crystal"))) {
                        dropName = dropName.replace("Crystal", "");
                        dropName = dropName.replace("Orb", "");
                        dropName = dropName.replace(" ", "");

                        if (orb.has("rarity") && orb.getInt("rarity") == 3) {
                            dropName = "normal" + dropName;
                        }
                        else if (orb.has("rarity") && orb.getInt("rarity") == 6) {
                            dropName = "crystal" + dropName;
                        }
                        else {
                            char c[] = dropName.toCharArray();
                            c[0] = Character.toLowerCase(c[0]);
                            dropName = dropName.substring(0, 1).toLowerCase() + dropName.substring(1);
                        }

                        dropName += "Inv";

                        result.put(dropName, orb.getInt("num"));
                    }
                }
            }
            catch(JSONException e) {
                Log.d(LOG_TAG, "Exception while reading materials inventory", e);
            }
        }

        Log.d(LOG_TAG, "Orb results: " + result.toString());
        return result;
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

    public boolean hasMaterialsChanged(JSONObject importingInventory, JSONObject existingInventory, String region) {
        try {
            JSONArray materials = importingInventory.optJSONArray("materials");
            JSONArray existingMaterials = existingInventory.optJSONArray("materials");

            if (materials != null && existingMaterials != null) {
                for (int i = 0; i < materials.length(); i++) {
                    JSONObject material = materials.getJSONObject(i);
                    Object materialId = material.opt("id");
                    if (materialId != null) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < existingMaterials.length(); j++) {
                            JSONObject existingMaterial = existingMaterials.getJSONObject(j);
                            Object existingMaterialId = existingMaterial.opt("id");
                            if (existingMaterialId != null && existingMaterialId.equals(materialId) && material.getInt("num") == existingMaterial.getInt("num")) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            return true;
                        }
                    }
                }

                for (int i = 0; i < existingMaterials.length(); i++) {
                    JSONObject existingMaterial = existingMaterials.getJSONObject(i);
                    Object existingMaterialId = existingMaterial.opt("id");
                    if (existingMaterialId != null) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < materials.length(); j++) {
                            JSONObject material = materials.getJSONObject(j);
                            Object materialId = material.opt("id");
                            if (existingMaterialId.equals(materialId)) {
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
            else if ((materials != null && existingMaterials == null)
                    || materials == null && existingMaterials != null) {
                return true;
            }
        }
        catch (JSONException e) {
            Log.w(LOG_TAG,"Exception while checking material inventory for changes.", e);
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
                    int existingRarity = existingEquipment.optInt("rarity");
                    int existingLevel = existingEquipment.optInt("level");
                    int existingHammering = existingEquipment.optInt("hammering_num");
                    if (existingRelicId != null) {
                        boolean alreadyExists = false;
                        for (int j = 0; j < equipments.length(); j++) {
                            JSONObject equipment = equipments.getJSONObject(j);
                            Object relicId = equipment.opt("equipment_id");
                            if (existingRelicId.equals(relicId)) {
                                int newLevel = equipment.optInt("level");
                                int newHammering = equipment.optInt("hammering_num");
                                int newRarity = equipment.optInt("rarity");
                                if (newLevel == existingLevel && existingHammering == newHammering
                                        && newRarity == existingRarity) {
                                    alreadyExists = true;
                                    break;
                                }
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
