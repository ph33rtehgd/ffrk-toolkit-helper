package com.ffrktoolkit.ffrktoolkithelper.util;

import java.util.HashMap;
import java.util.Map;

public class DropUtils {

    private final static Map<String, String> dropIdMap;
    static {
        dropIdMap = new HashMap<>();
        dropIdMap.put("40000001", "Minor Power Orb");
        dropIdMap.put("40000002", "Lesser Power Orb");
        dropIdMap.put("40000003", "Power Orb");
        dropIdMap.put("40000004", "Greater Power Orb");
        dropIdMap.put("40000005", "Major Power Orb");
        dropIdMap.put("40000006", "Minor White Orb");
        dropIdMap.put("40000007", "Lesser White Orb");
        dropIdMap.put("40000008", "White Orb");
        dropIdMap.put("40000009", "Greater White Orb");
        dropIdMap.put("40000010", "Major White Orb");
        dropIdMap.put("40000011", "Minor Black Orb");
        dropIdMap.put("40000012", "Lesser Black Orb");
        dropIdMap.put("40000013", "Black Orb");
        dropIdMap.put("40000014", "Greater Black Orb");
        dropIdMap.put("40000015", "Major Black Orb");
        dropIdMap.put("40000016", "Minor Blue Orb");
        dropIdMap.put("40000017", "Lesser Blue Orb");
        dropIdMap.put("40000018", "Blue Orb");
        dropIdMap.put("40000019", "Greater Blue Orb");
        dropIdMap.put("40000020", "Major Blue Orb");
        dropIdMap.put("40000021", "Minor Summon Orb");
        dropIdMap.put("40000022", "Lesser Summon Orb");
        dropIdMap.put("40000023", "Summon Orb");
        dropIdMap.put("40000024", "Greater Summon Orb");
        dropIdMap.put("40000025", "Major Summon Orb");
        dropIdMap.put("40000026", "Minor Non-Elemental Orb");
        dropIdMap.put("40000027", "Lesser Non-Elemental Orb");
        dropIdMap.put("40000028", "Non-Elemental Orb");
        dropIdMap.put("40000029", "Greater Non-Elemental Orb");
        dropIdMap.put("40000030", "Major Non-Elemental Orb");
        dropIdMap.put("40000031", "Minor Fire Orb");
        dropIdMap.put("40000032", "Lesser Fire Orb");
        dropIdMap.put("40000033", "Fire Orb");
        dropIdMap.put("40000034", "Greater Fire Orb");
        dropIdMap.put("40000035", "Major Fire Orb");
        dropIdMap.put("40000036", "Minor Ice Orb");
        dropIdMap.put("40000037", "Lesser Ice Orb");
        dropIdMap.put("40000038", "Ice Orb");
        dropIdMap.put("40000039", "Greater Ice Orb");
        dropIdMap.put("40000040", "Major Ice Orb");
        dropIdMap.put("40000041", "Minor Lightning Orb");
        dropIdMap.put("40000042", "Lesser Lightning Orb");
        dropIdMap.put("40000043", "Lightning Orb");
        dropIdMap.put("40000044", "Greater Lightning Orb");
        dropIdMap.put("40000045", "Major Lightning Orb");
        dropIdMap.put("40000046", "Minor Earth Orb");
        dropIdMap.put("40000047", "Lesser Earth Orb");
        dropIdMap.put("40000048", "Earth Orb");
        dropIdMap.put("40000049", "Greater Earth Orb");
        dropIdMap.put("40000050", "Major Earth Orb");
        dropIdMap.put("40000051", "Minor Wind Orb");
        dropIdMap.put("40000052", "Lesser Wind Orb");
        dropIdMap.put("40000053", "Wind Orb");
        dropIdMap.put("40000054", "Greater Wind Orb");
        dropIdMap.put("40000055", "Major Wind Orb");
        dropIdMap.put("40000056", "Minor Holy Orb");
        dropIdMap.put("40000057", "Lesser Holy Orb");
        dropIdMap.put("40000058", "Holy Orb");
        dropIdMap.put("40000059", "Greater Holy Orb");
        dropIdMap.put("40000060", "Major Holy Orb");
        dropIdMap.put("40000061", "Minor Dark Orb");
        dropIdMap.put("40000062", "Lesser Dark Orb");
        dropIdMap.put("40000063", "Dark Orb");
        dropIdMap.put("40000064", "Greater Dark Orb");
        dropIdMap.put("40000065", "Major Dark Orb");
        dropIdMap.put("40000066", "Power Crystal");
        dropIdMap.put("40000067", "White Crystal");
        dropIdMap.put("40000068", "Black Crystal");
        dropIdMap.put("40000069", "Blue Crystal");
        dropIdMap.put("40000070", "Summon Crystal");
        dropIdMap.put("40000071", "Non-Elemental Crystal");
        dropIdMap.put("40000072", "Fire Crystal");
        dropIdMap.put("40000073", "Ice Crystal");
        dropIdMap.put("40000074", "Lightning Crystal");
        dropIdMap.put("40000075", "Earth Crystal");
        dropIdMap.put("40000076", "Wind Crystal");
        dropIdMap.put("40000077", "Holy Crystal");
        dropIdMap.put("40000078", "Dark Crystal");
        dropIdMap.put("40000079", "Ultima Record");
    }

    public static String getDropName(String dropId) {
        String dropName = dropIdMap.get(dropId);
        if (dropName == null) {
            dropName = dropId;
        }

        return dropName;
    }

}
