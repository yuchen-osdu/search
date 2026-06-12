package org.opengroup.osdu.util;

import org.opengroup.osdu.core.test.config.EnvLoader;

public class Config {

    private static final int DEFAULT_SEARCH_INDEX_WAIT_MAX_ATTEMPTS = 30;
    private static final int DEFAULT_SEARCH_INDEX_WAIT_INTERVAL_SECONDS = 10;

    private Config() {
    }

    public static int getSearchIndexWaitMaxAttempts() {
        return parseIntOrDefault(
            EnvLoader.get("SEARCH_INDEX_WAIT_MAX_ATTEMPTS"),
            DEFAULT_SEARCH_INDEX_WAIT_MAX_ATTEMPTS);
    }

    public static int getSearchIndexWaitIntervalSeconds() {
        return parseIntOrDefault(
            EnvLoader.get("SEARCH_INDEX_WAIT_INTERVAL_SECONDS"),
            DEFAULT_SEARCH_INDEX_WAIT_INTERVAL_SECONDS);
    }

    public static String getDataPartitionIdTenant1() {
        return EnvLoader.getDataPartitionId();
    }

    public static String getDataPartitionIdTenant2() {
        return EnvLoader.get("DEFAULT_DATA_PARTITION_ID_TENANT2");
    }

    public static String getEntitlementsDomain() {
        return EnvLoader.get("ENTITLEMENTS_DOMAIN");
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
