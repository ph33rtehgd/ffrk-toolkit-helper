package com.ffrktoolkit.ffrktoolkithelper.util;

import java.util.HashMap;
import java.util.Map;

public class DropUtils {

    private final static Map<String, String> dropIdMap;
    private final static Map<String, Integer> rarityOverrideMap;

    static {
        dropIdMap = new HashMap<>();
        rarityOverrideMap = new HashMap<>();

        // Eggs
        dropIdMap.put("70000001", "Minor Growth Egg");
        dropIdMap.put("70000002", "Lesser Growth Egg");
        dropIdMap.put("70000003", "Growth Egg");
        dropIdMap.put("70000004", "Greater Growth Egg");
        dropIdMap.put("70000005", "Major Growth Egg");

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

        // Magicite
        dropIdMap.put("161000001", "Bomb");
        dropIdMap.put("161000002", "Iguion");
        dropIdMap.put("161000003", "Dragon");
        dropIdMap.put("161000004", "Flame Dragon");
        dropIdMap.put("161000005", "Salamander");
        dropIdMap.put("161000006", "Mom Bomb");
        dropIdMap.put("161000007", "Liquid Flame");
        dropIdMap.put("161000057", "Firemane");
        dropIdMap.put("161000058", "King Bomb");
        dropIdMap.put("161000059", "Maliris");
        dropIdMap.put("161000085", "Belias");
        dropIdMap.put("161000086", "Phoenix");
        dropIdMap.put("161000008", "Gnoll");
        dropIdMap.put("161000009", "Snowman");
        dropIdMap.put("161000010", "Snow Lion");
        dropIdMap.put("161000011", "White Dragon");
        dropIdMap.put("161000012", "Taharka");
        dropIdMap.put("161000013", "Wendigo");
        dropIdMap.put("161000014", "Sealion");
        dropIdMap.put("161000060", "Krysta");
        dropIdMap.put("161000061", "Dullahan");
        dropIdMap.put("161000062", "Isgebind");
        dropIdMap.put("161000087", "Manticore");
        dropIdMap.put("161000088", "Mateus");
        dropIdMap.put("161000029", "Yellow Elemental");
        dropIdMap.put("161000030", "Kalavinka Striker");
        dropIdMap.put("161000031", "Ymir");
        dropIdMap.put("161000032", "Thunder Dragon");
        dropIdMap.put("161000033", "Humbaba");
        dropIdMap.put("161000034", "Enlil");
        dropIdMap.put("161000035", "Hydra");
        dropIdMap.put("161000069", "Mimic Queen");
        dropIdMap.put("161000070", "Garuda");
        dropIdMap.put("161000071", "Ixion");
        dropIdMap.put("161000081", "Behemoth King");
        dropIdMap.put("161000082", "Quetzalcoatl");
        dropIdMap.put("161000022", "Earth Elemental");
        dropIdMap.put("161000023", "Clay Golem");
        dropIdMap.put("161000024", "Ralvuimago");
        dropIdMap.put("161000025", "Antlion");
        dropIdMap.put("161000026", "Sand Worm");
        dropIdMap.put("161000027", "Shell Dragon");
        dropIdMap.put("161000028", "Golem");
        dropIdMap.put("161000066", "Earth Guardian");
        dropIdMap.put("161000067", "Catastrophe");
        dropIdMap.put("161000068", "Midgardsormr");
        dropIdMap.put("161000091", "Adamantoise");
        dropIdMap.put("161000092", "Hecatoncheir");
        dropIdMap.put("161000015", "Rangda");
        dropIdMap.put("161000016", "Galypdes");
        dropIdMap.put("161000017", "Zu");
        dropIdMap.put("161000018", "Rapps");
        dropIdMap.put("161000019", "Wing Raptor");
        dropIdMap.put("161000020", "Enkidu");
        dropIdMap.put("161000021", "Fenrir");
        dropIdMap.put("161000063", "Sylph");
        dropIdMap.put("161000064", "Silver Dragon");
        dropIdMap.put("161000065", "Tiamat");
        dropIdMap.put("161000089", "Typhon");
        dropIdMap.put("161000090", "Syldra");
        dropIdMap.put("161000036", "Water Flan");
        dropIdMap.put("161000037", "Sahagin");
        dropIdMap.put("161000038", "Anguiform");
        dropIdMap.put("161000039", "Sea Dragon");
        dropIdMap.put("161000040", "Bottomswell");
        dropIdMap.put("161000041", "Enki");
        dropIdMap.put("161000042", "Bismarck");
        dropIdMap.put("161000072", "Gizamaluke");
        dropIdMap.put("161000073", "Octomammoth");
        dropIdMap.put("161000074", "Kraken");
        dropIdMap.put("161000083", "Geosgaeno");
        dropIdMap.put("161000084", "Famfrit");
        dropIdMap.put("161000043", "Cure Beast");
        dropIdMap.put("161000044", "Fairy Orc");
        dropIdMap.put("161000045", "Nymph");
        dropIdMap.put("161000046", "White Flame");
        dropIdMap.put("161000047", "Kirin");
        dropIdMap.put("161000048", "Unicorn");
        dropIdMap.put("161000049", "Mist Dragon");
        dropIdMap.put("161000075", "Seraph");
        dropIdMap.put("161000076", "Evrae");
        dropIdMap.put("161000077", "Siren");
        dropIdMap.put("161000093", "Madeen");
        dropIdMap.put("161000094", "Lakshmi");
        dropIdMap.put("161000050", "Lilith");
        dropIdMap.put("161000051", "Forbidden");
        dropIdMap.put("161000052", "Etém");
        dropIdMap.put("161000053", "Ghoul");
        dropIdMap.put("161000054", "Darkmare");
        dropIdMap.put("161000055", "Phantom");
        dropIdMap.put("161000056", "Shadow Dragon");
        dropIdMap.put("161000078", "Dragon Zombie");
        dropIdMap.put("161000079", "Necrophobe");
        dropIdMap.put("161000080", "Hades");
        dropIdMap.put("161000095", "Ark");
        dropIdMap.put("161000096", "Deathgaze");

        rarityOverrideMap.put("161000057", 4);
        rarityOverrideMap.put("161000058", 4);
        rarityOverrideMap.put("161000059", 4);
        rarityOverrideMap.put("161000060", 4);
        rarityOverrideMap.put("161000061", 4);
        rarityOverrideMap.put("161000062", 4);
        rarityOverrideMap.put("161000069", 4);
        rarityOverrideMap.put("161000070", 4);
        rarityOverrideMap.put("161000071", 4);
        rarityOverrideMap.put("161000066", 4);
        rarityOverrideMap.put("161000067", 4);
        rarityOverrideMap.put("161000068", 4);
        rarityOverrideMap.put("161000063", 4);
        rarityOverrideMap.put("161000064", 4);
        rarityOverrideMap.put("161000065", 4);
        rarityOverrideMap.put("161000072", 4);
        rarityOverrideMap.put("161000073", 4);
        rarityOverrideMap.put("161000074", 4);
        rarityOverrideMap.put("161000075", 4);
        rarityOverrideMap.put("161000076", 4);
        rarityOverrideMap.put("161000077", 4);
        rarityOverrideMap.put("161000078", 4);
        rarityOverrideMap.put("161000079", 4);
        rarityOverrideMap.put("161000080", 4);

    }

    public static String getDropName(String dropId) {
        String dropName = dropIdMap.get(dropId);
        if (dropName == null) {
            dropName = dropId;
        }

        return dropName;
    }

    public static Integer overrideRarity(String dropId, Integer startingRarity) {
        Integer rarity = rarityOverrideMap.get(dropId);
        if (rarity == null) {
            rarity = startingRarity;
        }

        return rarity;
    }
}
