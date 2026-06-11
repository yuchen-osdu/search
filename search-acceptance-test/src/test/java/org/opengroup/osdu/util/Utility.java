package org.opengroup.osdu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    private static final Pattern QUOTED_LIST_ITEM = Pattern.compile("\"([^\"]*)\"");

    public static List<String> parseCommaSeparatedList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        if (value.contains("\"")) {
            List<String> quotedItems = new ArrayList<>();
            Matcher matcher = QUOTED_LIST_ITEM.matcher(value);
            while (matcher.find()) {
                quotedItems.add(matcher.group(1));
            }
            if (!quotedItems.isEmpty()) {
                return quotedItems;
            }
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }

    public static String beautifyJsonString(String payload) {
        if (payload == null) {
            return null;
        }
        Gson gson = new Gson();
        return new GsonBuilder().setPrettyPrinting().create().toJson(gson.fromJson(payload, Object.class));
    }

    public static boolean containsField(Map<String, Object> record, String fieldName) {
        if (fieldName == null || fieldName.isBlank() || record == null || record.isEmpty()) {
            return false;
        }

        if (fieldName.startsWith("data.")) {
            fieldName = fieldName.substring("data.".length());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) record.getOrDefault("data", new HashMap<>());
            if (data == null || data.isEmpty()) {
                return false;
            }
            return data.containsKey(fieldName);
        }
        return record.containsKey(fieldName);
    }
}
