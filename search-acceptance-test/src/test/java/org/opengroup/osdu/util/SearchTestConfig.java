package org.opengroup.osdu.util;

import org.opengroup.osdu.core.test.config.EnvLoader;

public final class SearchTestConfig {

    private static final String GROUP_ID_VARIABLE = "GROUP_ID";
    private static final String ENTITLEMENTS_DOMAIN_VARIABLE = "ENTITLEMENTS_DOMAIN";

    private SearchTestConfig() {
    }

    public static void updateEntitlementsDomainFromGroupId() {
        String groupId = EnvLoader.get(GROUP_ID_VARIABLE);
        if (groupId != null && !groupId.isBlank()) {
            System.setProperty(ENTITLEMENTS_DOMAIN_VARIABLE, groupId);
        }
    }
}
