package com.ffrktoolkit.ffrktoolkithelper.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.ffrktoolkit.ffrktoolkithelper.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;

import io.netty.util.CharsetUtil;

public class JsonUtils {

    public static String getStringFromFile(File file, Context context) {
        String inventoryJsonString = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            inputStream.read(data);
            inventoryJsonString = new String(data, CharsetUtil.UTF_8);
        }
        catch (Exception e) {
            Toast toast = Toast.makeText(context.getApplicationContext(), context.getString(R.string.exception_invalid_inventory_data_toast), Toast.LENGTH_SHORT);
            toast.show();
        }

        return inventoryJsonString;
    }

}
